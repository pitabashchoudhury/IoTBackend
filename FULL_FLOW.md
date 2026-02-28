# IoT Platform — Full Data Flow (Device to UI)

End-to-end documentation of how data moves from a physical ESP32 device through the backend services to the React dashboard in the browser.

---

## System Overview

```
  ┌────────────────┐        MQTT         ┌────────────────┐       WebSocket       ┌────────────────┐
  │  ESP32 Device  │ ──────────────────► │  IoT Backend   │ ────────────────────► │  React UI      │
  │  (Sensors +    │ ◄────────────────── │  (Spring Boot  │ ◄──── HTTP REST ───── │  (Browser)     │
  │   Actuators)   │    Control Cmds     │   Microservices│                        │                │
  └────────────────┘                     └────────────────┘                        └────────────────┘
```

### Components

| Component | Technology | Location |
|-----------|-----------|----------|
| Physical Device | ESP32 + PlatformIO + Arduino | `iot-arduino/` |
| MQTT Broker | HiveMQ Public (`broker.hivemq.com:1883`) | Cloud (external) |
| Discovery Server | Spring Cloud Eureka (:8761) | `iot-backend/discovery-server/` |
| API Gateway | Spring Cloud Gateway (:8080) | `iot-backend/api-gateway/` |
| Auth Service | Spring Boot (:8081) + PostgreSQL (`auth_db`) | `iot-backend/auth-service/` |
| Device Service | Spring Boot (:8082) + PostgreSQL (`device_db`) + MQTT + WebSocket | `iot-backend/device-service/` |
| Weather Service | Spring Boot (:8084) + OpenWeatherMap API | `iot-backend/weather-service/` |
| Frontend | React 19 + Vite + Tailwind (:3000) | `iot-frontend/` |

---

## 1. Physical Device (ESP32)

### Hardware

| Sensor/Actuator | GPIO | Type | Purpose |
|-----------------|------|------|---------|
| DHT22 | 4 | Digital | Temperature + Humidity |
| BMP280 | 21/22 (I2C) | I2C | Pressure + Altitude |
| Soil Moisture | 34 | Analog | Soil humidity (optional) |
| LDR (Light) | 35 | Analog | Light level (optional) |
| Built-in LED | 2 | Output | Status indicator / controllable |

### Boot Sequence

```
Power On
   │
   ▼
Serial init (115200 baud)
   │
   ▼
Configure LED pin (GPIO 2)
   │
   ▼
Connect WiFi ──► WIFI_SSID / WIFI_PASSWORD
   │               20 retries × 5s, restart on failure
   ▼
Init sensors ──► DHT22 (GPIO 4), BMP280 (I2C)
   │
   ▼
Connect MQTT ──► broker.hivemq.com:1883
   │               Client ID: iot_arduino_{timestamp}
   │               Last Will: devices/{id}/status → {"is_online":false} (retained)
   ▼
Publish ──► devices/{id}/status → {"is_online":true} (retained)
   │
   ▼
Subscribe ──► devices/{id}/control (QoS 1)
   │
   ▼
Enter main loop
```

**Source**: `iot-arduino/src/main.cpp`

### Main Loop Timers

```
┌─────────────────────────────────────────────────────────┐
│                    ESP32 Main Loop                       │
│                                                          │
│  Every 5 seconds:                                        │
│    Read sensors → cache in latestData                    │
│    (DHT22: temp + humidity, BMP280: pressure + altitude) │
│                                                          │
│  Every 10 seconds:                                       │
│    Publish telemetry → devices/{id}/telemetry            │
│    Payload: { temperature, humidity, pressure, ... }     │
│                                                          │
│  Every 30 seconds:                                       │
│    Publish heartbeat → devices/{id}/status               │
│    Payload: { "is_online": true }                        │
│                                                          │
│  Continuously:                                           │
│    Check WiFi → reconnect if dropped                     │
│    Check MQTT → reconnect if dropped (5s retry)          │
│    Process incoming control commands                     │
└─────────────────────────────────────────────────────────┘
```

**Source**: `iot-arduino/src/main.cpp` (lines 105-149)

---

## 2. MQTT Broker

The MQTT broker is the message bus between physical devices and the backend.

```
                    ┌──────────────────────────────────┐
                    │   MQTT Broker (HiveMQ Public)    │
                    │   tcp://broker.hivemq.com:1883   │
                    └──────────────────────────────────┘
                         │                      ▲
          Subscribe:     │                      │  Publish:
          devices/#      │                      │  devices/{id}/status
                         │                      │  devices/{id}/telemetry
                         ▼                      │
                  ┌──────────────┐       ┌──────────────┐
                  │ Device Svc   │       │    ESP32     │
                  │ (Backend)    │       │   Device     │
                  └──────────────┘       └──────────────┘
```

### Topic Structure

| Topic | Direction | Publisher | Subscriber | QoS | Retained | Payload |
|-------|-----------|-----------|------------|-----|----------|---------|
| `devices/{id}/status` | Device → Backend | ESP32 | device-service | 1 | Yes | `{"is_online": true/false}` |
| `devices/{id}/telemetry` | Device → Backend | ESP32 | device-service | 1 | No | Sensor JSON (see below) |
| `devices/{id}/control` | Backend → Device | External/App | ESP32 | 1 | No | `controlId:value` |

### Payload Formats

**Status** (retained — broker stores last message):
```json
{"is_online": true}
```

**Telemetry**:
```json
{
  "temperature": 25.30,
  "humidity": 62.10,
  "pressure": 1013.25,
  "altitude": 45.20,
  "soilMoisture": 72,
  "lightLevel": 3200,
  "uptimeMs": 120000,
  "timestamp": 120,
  "freeHeap": 245000
}
```

**Control command** (plain text, not JSON):
```
led-control-uuid:true
fan-speed-uuid:75
mode-uuid:auto
```

### Last Will Testament (LWT)

When the ESP32 disconnects unexpectedly (power loss, WiFi drop), the broker automatically publishes:
```
Topic:    devices/{id}/status
Payload:  {"is_online": false}
Retained: Yes
```

This ensures the backend is notified even without an explicit disconnect message.

---

## 3. Device Service — MQTT Listener

The device-service subscribes to `devices/#` at startup and routes incoming messages.

```
MQTT: devices/{deviceId}/{messageType}
              │              │
              │              ├── "status"    → handleStatusMessage()
              │              ├── "control"   → sendDeviceControl()
              │              └── "telemetry" → sendDeviceTelemetry()
              │
              └── Parsed from topic string parts[1]
```

### MqttService (Message Arrival)

**File**: `iot-backend/device-service/src/main/java/com/foodchain/device/service/MqttService.java`

```
@PostConstruct init()
   │
   ▼
mqttClient.setCallback(this)
   │
   ▼
mqttClient.subscribe("devices/#", qos=1)
   │
   ▼
messageArrived(topic, message)               ◄── Called by MQTT client
   │
   ├── Parse topic: "devices/{deviceId}/{type}"
   │
   ├── type == "status"
   │     ├── handleStatusMessage(deviceId, payload)
   │     │     ├── Parse JSON: {"is_online": true/false}
   │     │     ├── deviceRepository.findById(deviceId)
   │     │     ├── device.setOnline(is_online)
   │     │     └── deviceRepository.save(device)          ◄── Updates PostgreSQL
   │     │
   │     └── webSocketService.sendDeviceStatus(deviceId, payload)
   │           └── messagingTemplate.convertAndSend(
   │                 "/topic/devices/{deviceId}/status", payload)
   │
   ├── type == "control"
   │     └── webSocketService.sendDeviceControl(deviceId, payload)
   │           └── messagingTemplate.convertAndSend(
   │                 "/topic/devices/{deviceId}/control", payload)
   │
   └── type == "telemetry"
         └── webSocketService.sendDeviceTelemetry(deviceId, payload)
               └── messagingTemplate.convertAndSend(
                     "/topic/devices/{deviceId}/telemetry", payload)
```

### What Gets Stored vs What Gets Forwarded

| Message Type | Stored in DB? | Forwarded to WebSocket? |
|-------------|--------------|------------------------|
| Status | Yes — `is_online` column in `devices` table | Yes |
| Control | No | Yes |
| Telemetry | No — pass-through only | Yes |

---

## 4. Device Service — WebSocket Broadcast

**File**: `iot-backend/device-service/src/main/java/com/foodchain/device/service/WebSocketNotificationService.java`

The WebSocket bridge uses Spring's `SimpMessagingTemplate` to forward MQTT payloads to connected browser clients:

```
MQTT payload (raw JSON string)
         │
         ▼
SimpMessagingTemplate.convertAndSend(destination, payload)
         │
         ▼
Spring SimpleBroker at /topic/**
         │
         ▼
All subscribed STOMP clients receive the message
```

**STOMP Topics Broadcast**:

| STOMP Destination | Source | Payload |
|------------------|--------|---------|
| `/topic/devices/{id}/status` | MQTT `devices/{id}/status` | `{"is_online": true/false}` |
| `/topic/devices/{id}/control` | MQTT `devices/{id}/control` | `controlId:value` |
| `/topic/devices/{id}/telemetry` | MQTT `devices/{id}/telemetry` | Sensor JSON |

**WebSocket Config** (`WebSocketConfig.java`):
- STOMP endpoint: `/ws` (with SockJS fallback)
- Simple broker prefix: `/topic`
- Application prefix: `/app`
- CORS: all origins allowed

---

## 5. API Gateway — Request Routing

**File**: `iot-backend/api-gateway/src/main/resources/application.yml`

```
Browser Request
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (:8080)                        │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Route Matching                                         │  │
│  │                                                        │  │
│  │  /api/auth/login        ──► auth-service   (no JWT)   │  │
│  │  /api/auth/register     ──► auth-service   (no JWT)   │  │
│  │  /api/auth/**           ──► auth-service   (JWT)      │  │
│  │  /api/devices/**        ──► device-service (JWT)      │  │
│  │  /api/weather/**        ──► weather-service(JWT)      │  │
│  │  /api/ws/**             ──► device-service (no JWT)   │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ JWT Filter (for protected routes)                      │  │
│  │                                                        │  │
│  │  1. Extract token from: Authorization: Bearer {token}  │  │
│  │  2. Validate signature with JWT_SECRET                 │  │
│  │  3. Extract userId from JWT subject claim              │  │
│  │  4. Add header: X-User-Id: {userId}                   │  │
│  │  5. StripPrefix=1: /api/devices → /devices            │  │
│  │  6. Forward to downstream service                      │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Frontend — Receiving Real-Time Data

### WebSocket Connection

**File**: `iot-frontend/src/services/websocket.js`

```
Browser
   │
   ├── new SockJS('/api/ws')                    ◄── HTTP upgrade via nginx/Vite proxy
   │     │
   │     ▼ (gateway strips /api → /ws → device-service)
   │
   ├── STOMP CONNECT
   │
   ├── SUBSCRIBE /topic/devices/{id}/status
   ├── SUBSCRIBE /topic/devices/{id}/control
   └── SUBSCRIBE /topic/devices/{id}/telemetry
         │
         ▼ (on message arrival)
   Parse JSON → toCamelCase() → update React state
```

### Dashboard — Live Status Indicators

**File**: `iot-frontend/src/pages/DashboardPage.jsx`

```
DashboardPage mounts
   │
   ├── useDevices() → GET /api/devices → device list
   │
   ├── useWebSocket(devices.map(d => `/topic/devices/${d.id}/status`))
   │     │
   │     └── Subscribes to ALL device status topics
   │
   └── Merge live status into device list:
         devices.map(d => {
           statusUpdate = messages[`/topic/devices/${d.id}/status`]
           if (statusUpdate?.isOnline !== undefined)
             return { ...d, isOnline: statusUpdate.isOnline }
           return d
         })
         │
         ▼
   DeviceGrid renders DeviceCard with live online/offline badge
```

### Device Detail — Live Controls + Telemetry

**File**: `iot-frontend/src/pages/DeviceDetailPage.jsx`

```
DeviceDetailPage mounts (for device {id})
   │
   ├── useDevice(id) → GET /api/devices/{id} → device details
   │
   ├── useWebSocket([
   │     `/topic/devices/${id}/status`,
   │     `/topic/devices/${id}/control`,
   │     `/topic/devices/${id}/telemetry`
   │   ])
   │
   ├── Live status:
   │     statusMsg?.isOnline → update DeviceStatusBadge (green/gray dot)
   │
   ├── Live control values:
   │     controlMsg?.controlId → update matching control's currentValue
   │
   ├── Live telemetry:
   │     telemetryMsg → render raw JSON in telemetry panel
   │
   └── ControlRenderer renders each control:
         ToggleControl / SliderControl / ButtonControl / DropdownControl / ColorPickerControl
```

---

## 7. Full Data Flows

### Flow A: Sensor Telemetry (Device → UI)

```
┌─────────┐     ┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌─────────┐
│  DHT22  │     │  ESP32   │     │ MQTT Broker  │     │ Device Svc   │     │ Browser │
│ Sensor  │     │          │     │ (HiveMQ)     │     │ (:8082)      │     │         │
└────┬────┘     └────┬─────┘     └──────┬───────┘     └──────┬───────┘     └────┬────┘
     │               │                   │                     │                  │
     │ read()        │                   │                     │                  │
     ├──────────────►│                   │                     │                  │
     │  temp=25.3    │                   │                     │                  │
     │  humidity=62  │                   │                     │                  │
     │               │                   │                     │                  │
     │               │ PUBLISH           │                     │                  │
     │               │ devices/{id}/     │                     │                  │
     │               │ telemetry         │                     │                  │
     │               │ {"temperature":   │                     │                  │
     │               │  25.3, ...}       │                     │                  │
     │               ├──────────────────►│                     │                  │
     │               │                   │                     │                  │
     │               │                   │ DELIVER (QoS 1)     │                  │
     │               │                   ├────────────────────►│                  │
     │               │                   │                     │                  │
     │               │                   │                     │ messageArrived() │
     │               │                   │                     │ type=telemetry   │
     │               │                   │                     │                  │
     │               │                   │                     │ (NOT stored      │
     │               │                   │                     │  in database)    │
     │               │                   │                     │                  │
     │               │                   │                     │ STOMP SEND       │
     │               │                   │                     │ /topic/devices/  │
     │               │                   │                     │ {id}/telemetry   │
     │               │                   │                     ├─────────────────►│
     │               │                   │                     │                  │
     │               │                   │                     │                  │ useWebSocket()
     │               │                   │                     │                  │ parse JSON
     │               │                   │                     │                  │ toCamelCase()
     │               │                   │                     │                  │ update state
     │               │                   │                     │                  │ render telemetry
     │               │                   │                     │                  │ panel
```

### Flow B: Device Status / Online-Offline

```
┌──────────┐     ┌──────────┐     ┌────────────┐     ┌───────────┐     ┌─────────┐
│  ESP32   │     │  MQTT    │     │ Device Svc │     │ PostgreSQL│     │ Browser │
│          │     │  Broker  │     │            │     │ device_db │     │         │
└────┬─────┘     └────┬─────┘     └─────┬──────┘     └─────┬─────┘     └────┬────┘
     │                │                  │                   │                │
     │ ── CASE 1: Normal heartbeat (every 30s) ────────────────────────────  │
     │                │                  │                   │                │
     │ PUBLISH        │                  │                   │                │
     │ devices/{id}/  │                  │                   │                │
     │ status         │                  │                   │                │
     │ {"is_online":  │                  │                   │                │
     │  true}         │                  │                   │                │
     │ (retained)     │                  │                   │                │
     ├───────────────►│                  │                   │                │
     │                │ DELIVER          │                   │                │
     │                ├─────────────────►│                   │                │
     │                │                  │                   │                │
     │                │                  │ UPDATE            │                │
     │                │                  │ SET is_online=    │                │
     │                │                  │ true              │                │
     │                │                  ├──────────────────►│                │
     │                │                  │                   │                │
     │                │                  │ STOMP SEND        │                │
     │                │                  │ /topic/.../status │                │
     │                │                  ├──────────────────────────────────►│
     │                │                  │                   │                │
     │                │                  │                   │          DeviceCard
     │                │                  │                   │          updates
     │                │                  │                   │          green dot
     │                │                  │                   │                │
     │ ── CASE 2: Power loss / WiFi drop ──────────────────────────────────  │
     │                │                  │                   │                │
     ╳ (disconnects)  │                  │                   │                │
                      │                  │                   │                │
                      │ LWT triggers:    │                   │                │
                      │ devices/{id}/    │                   │                │
                      │ status           │                   │                │
                      │ {"is_online":    │                   │                │
                      │  false}          │                   │                │
                      │ (retained)       │                   │                │
                      ├─────────────────►│                   │                │
                      │                  │                   │                │
                      │                  │ UPDATE            │                │
                      │                  │ SET is_online=    │                │
                      │                  │ false             │                │
                      │                  ├──────────────────►│                │
                      │                  │                   │                │
                      │                  │ STOMP SEND        │                │
                      │                  ├──────────────────────────────────►│
                      │                  │                   │                │
                      │                  │                   │          DeviceCard
                      │                  │                   │          updates
                      │                  │                   │          gray dot
```

### Flow C: User Changes Control (UI → Device)

```
┌─────────┐     ┌──────────┐     ┌──────────┐     ┌────────────┐     ┌───────────┐
│ Browser │     │ Gateway  │     │Device Svc│     │ PostgreSQL │     │  ESP32    │
│         │     │ (:8080)  │     │ (:8082)  │     │ device_db  │     │           │
└────┬────┘     └────┬─────┘     └────┬─────┘     └─────┬──────┘     └─────┬─────┘
     │               │                │                   │                  │
     │ User toggles  │                │                   │                  │
     │ a switch      │                │                   │                  │
     │               │                │                   │                  │
     │ PUT /api/     │                │                   │                  │
     │ devices/{id}  │                │                   │                  │
     │ { controls:   │                │                   │                  │
     │   [{ id,      │                │                   │                  │
     │   currentValue│                │                   │                  │
     │   :"true"}] } │                │                   │                  │
     ├──────────────►│                │                   │                  │
     │               │                │                   │                  │
     │               │ Validate JWT   │                   │                  │
     │               │ Extract userId │                   │                  │
     │               │ Add X-User-Id  │                   │                  │
     │               │ Strip /api     │                   │                  │
     │               │                │                   │                  │
     │               │ PUT /devices/  │                   │                  │
     │               │ {id}           │                   │                  │
     │               ├───────────────►│                   │                  │
     │               │                │                   │                  │
     │               │                │ findByIdAndUserId │                  │
     │               │                ├──────────────────►│                  │
     │               │                │◄─────────────────┤│                  │
     │               │                │                   │                  │
     │               │                │ applyUpdate()     │                  │
     │               │                │ update controls   │                  │
     │               │                │ currentValue      │                  │
     │               │                │                   │                  │
     │               │                │ save()            │                  │
     │               │                ├──────────────────►│                  │
     │               │                │                   │                  │
     │               │                │ Return DeviceDto  │                  │
     │               │◄──────────────┤│                   │                  │
     │◄──────────────┤│               │                   │                  │
     │               │                │                   │                  │
     │ Update UI     │                │                   │                  │
     │ with response │                │                   │                  │
     │               │                │                   │                  │
     │  NOTE: The backend does NOT publish to MQTT here.  │                  │
     │  The physical device does NOT receive this command  │                  │
     │  through this flow. See "Architecture Note" below.  │                  │
```

### Flow D: User Registration + First Device

```
┌─────────┐     ┌──────────┐     ┌──────────┐     ┌────────────┐     ┌────────────┐
│ Browser │     │ Gateway  │     │ Auth Svc │     │ Device Svc │     │ PostgreSQL │
└────┬────┘     └────┬─────┘     └────┬─────┘     └─────┬──────┘     └─────┬──────┘
     │               │                │                   │                  │
     │ POST /api/auth/register        │                   │                  │
     │ {name, email, password}        │                   │                  │
     ├──────────────►│                │                   │                  │
     │               │ (no JWT filter)│                   │                  │
     │               ├───────────────►│                   │                  │
     │               │                │ hash password     │                  │
     │               │                │ save to auth_db   │                  │
     │               │                │ generate JWT      │                  │
     │               │                │ (exp: 1 hour)     │                  │
     │               │                │ generate refresh  │                  │
     │               │                │ (exp: 7 days)     │                  │
     │               │◄──────────────┤│                   │                  │
     │◄──────────────┤│               │                   │                  │
     │               │                │                   │                  │
     │ Store in localStorage:         │                   │                  │
     │ token, refreshToken, user      │                   │                  │
     │ Schedule refresh (55 min)      │                   │                  │
     │ Navigate to /                  │                   │                  │
     │               │                │                   │                  │
     │ ── Dashboard loads ───────────────────────────────────────────────── │
     │               │                │                   │                  │
     │ GET /api/devices               │                   │                  │
     │ Auth: Bearer {token}           │                   │                  │
     ├──────────────►│                │                   │                  │
     │               │ JWT valid      │                   │                  │
     │               │ X-User-Id added│                   │                  │
     │               ├───────────────────────────────────►│                  │
     │               │                │                   │ findByUserId     │
     │               │                │                   ├─────────────────►│
     │               │                │                   │◄────────────────┤│
     │               │◄──────────────────────────────────┤│  [] (empty)     │
     │◄──────────────┤│               │                   │                  │
     │               │                │                   │                  │
     │ Show empty grid + "Add Device" │                   │                  │
     │               │                │                   │                  │
     │ ── User clicks Add Device ────────────────────────────────────────── │
     │               │                │                   │                  │
     │ POST /api/devices              │                   │                  │
     │ {name:"Temp Sensor",           │                   │                  │
     │  type:"SENSOR",                │                   │                  │
     │  location:{label:"Garden"},    │                   │                  │
     │  mqttTopicPrefix:"garden",     │                   │                  │
     │  controls:[{                   │                   │                  │
     │    name:"LED",                 │                   │                  │
     │    controlType:"TOGGLE",       │                   │                  │
     │    mqttTopic:"garden/led"      │                   │                  │
     │  }]}                           │                   │                  │
     ├──────────────►│                │                   │                  │
     │               ├───────────────────────────────────►│                  │
     │               │                │                   │ save device      │
     │               │                │                   │ + controls       │
     │               │                │                   │ isOnline=false   │
     │               │                │                   ├─────────────────►│
     │               │                │                   │                  │
     │               │◄──────────────────────────────────┤│ DeviceDto       │
     │◄──────────────┤│               │                   │ (201 Created)   │
     │               │                │                   │                  │
     │ Add to grid,  │                │                   │                  │
     │ show as Offline│               │                   │                  │
     │ (gray dot)    │                │                   │                  │
     │               │                │                   │                  │
     │ Subscribe to WebSocket:        │                   │                  │
     │ /topic/devices/{newId}/status  │                   │                  │
```

### Flow E: Token Refresh

```
┌─────────┐              ┌──────────┐     ┌──────────┐
│ Browser │              │ Gateway  │     │ Auth Svc │
└────┬────┘              └────┬─────┘     └────┬─────┘
     │                        │                │
     │ (55 min after login)   │                │
     │ Timer fires in         │                │
     │ AuthContext             │                │
     │                        │                │
     │ POST /api/auth/refresh │                │
     │ Auth: Bearer {token}   │                │
     ├───────────────────────►│                │
     │                        │ JWT valid      │
     │                        │ X-User-Id      │
     │                        ├───────────────►│
     │                        │                │ validate refresh token
     │                        │                │ rotate: new token +
     │                        │                │ new refresh token
     │                        │◄──────────────┤│
     │◄───────────────────────┤│               │
     │                        │                │
     │ Update localStorage    │                │
     │ Schedule next refresh  │                │
     │ (55 min from now)      │                │
     │                        │                │
     │ ── If refresh fails ─────────────────── │
     │                        │                │
     │ Clear localStorage     │                │
     │ Navigate to /login     │                │
```

---

## 8. Database Schema

### auth_db (Auth Service)

```
┌─────────────────────────────────────┐
│ users                                │
├─────────────────────────────────────┤
│ id            UUID    PK             │
│ name          VARCHAR NOT NULL       │
│ email         VARCHAR NOT NULL UNIQUE│
│ password      VARCHAR NOT NULL       │ ◄── BCrypt hashed
│ profile_image_url VARCHAR            │
│ created_at    TIMESTAMP              │
│ updated_at    TIMESTAMP              │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ refresh_tokens                       │
├─────────────────────────────────────┤
│ id            UUID    PK             │
│ user_id       UUID    FK → users     │
│ token         VARCHAR NOT NULL UNIQUE│
│ expires_at    TIMESTAMP NOT NULL     │
│ created_at    TIMESTAMP              │
└─────────────────────────────────────┘
```

### device_db (Device Service)

```
┌───────────────────────────────────────┐
│ devices                                │
├───────────────────────────────────────┤
│ id                UUID    PK           │
│ user_id           UUID    NOT NULL     │ ◄── No FK (separate DB), matches auth user id
│ name              VARCHAR NOT NULL     │
│ type              VARCHAR NOT NULL     │ ◄── LIGHT/THERMOSTAT/SWITCH/SENSOR/CAMERA/LOCK/FAN/CUSTOM
│ is_online         BOOLEAN DEFAULT false│
│ latitude          DOUBLE               │
│ longitude         DOUBLE               │
│ address           VARCHAR              │
│ location_label    VARCHAR              │
│ mqtt_topic_prefix VARCHAR              │
│ created_at        TIMESTAMP            │
│ updated_at        TIMESTAMP            │
└───────────────┬───────────────────────┘
                │ 1:N
                ▼
┌───────────────────────────────────────┐
│ device_controls                        │
├───────────────────────────────────────┤
│ id              UUID    PK             │
│ device_id       UUID    FK → devices   │
│ name            VARCHAR NOT NULL       │
│ control_type    VARCHAR NOT NULL       │ ◄── TOGGLE/SLIDER/BUTTON/DROPDOWN/COLOR_PICKER
│ current_value   VARCHAR               │
│ min_value       FLOAT                 │ ◄── For SLIDER
│ max_value       FLOAT                 │ ◄── For SLIDER
│ step            FLOAT                 │ ◄── For SLIDER
│ options         TEXT                   │ ◄── For DROPDOWN (comma-separated)
│ mqtt_topic      VARCHAR               │
└───────────────────────────────────────┘
```

---

## 9. Network / Port Map

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Docker Network                                │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ auth-db  │  │device-db │  │discovery │  │ gateway  │            │
│  │ PG :5432 │  │ PG :5432 │  │  :8761   │  │  :8080   │            │
│  │ ►host    │  │ ►host    │  │ ►host    │  │ ►host    │            │
│  │  :5432   │  │  :5433   │  │  :8761   │  │  :8080   │            │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘            │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐        │
│  │ auth-svc │  │device-svc│  │weather   │  │ iot-frontend │        │
│  │  :8081   │  │  :8082   │  │  :8084   │  │ nginx :80    │        │
│  │ ►host    │  │ ►host    │  │ ►host    │  │ ►host :3000  │        │
│  │  :8081   │  │  :8082   │  │  :8084   │  │              │        │
│  └──────────┘  └──────────┘  └──────────┘  │ /api/* ──────┼─► :8080│
│                                             │ /api/ws ─────┼─► :8080│
│                                             │ /* ──► SPA   │        │
│                                             └──────────────┘        │
└──────────────────────────────────────────────────────────────────────┘

External:
  MQTT Broker ── tcp://broker.hivemq.com:1883
  OpenWeatherMap ── https://api.openweathermap.org
  ESP32 Device ── WiFi → MQTT Broker
```

---

## 10. JSON Serialization Note

All backend JSON uses **snake_case** (configured via `JacksonConfig` in `common-lib`).

The frontend Axios interceptors automatically convert:
- **Outgoing requests**: camelCase → snake_case
- **Incoming responses**: snake_case → camelCase

| Java Field | JSON (wire) | JavaScript |
|-----------|-------------|------------|
| `isOnline` | `is_online` | `isOnline` |
| `currentValue` | `current_value` | `currentValue` |
| `mqttTopicPrefix` | `mqtt_topic_prefix` | `mqttTopicPrefix` |
| `controlType` | `control_type` | `controlType` |
| `createdAt` | `created_at` | `createdAt` |

---

## 11. Architecture Note — Control Command Gap

The current architecture stores control value changes in the database but **does not publish them back to the physical device via MQTT**. When a user toggles a switch in the UI:

1. `PUT /api/devices/{id}` updates `current_value` in PostgreSQL
2. The device-service returns the updated DeviceDto
3. The UI reflects the new value

However, the ESP32 device **does not receive this command**. To close this gap, either:

- **Option A**: Add MQTT publishing in `DeviceService.updateDevice()` — when a control value changes, publish to the control's `mqttTopic` (already stored in DB)
- **Option B**: The ESP32 subscribes to its `devices/{id}/control` topic and an external system (or the backend) publishes control commands there
- **Option C**: The device polls a REST endpoint for its current control state

This is a known architectural boundary — the backend acts as the system of record, while MQTT-to-device command delivery is a separate concern.
