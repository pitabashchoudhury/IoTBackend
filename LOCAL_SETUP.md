# Local Setup Guide - IoT Backend

Step-by-step guide to run the entire backend on a local machine. Covers both **Docker Compose (fully containerized)** and **IntelliJ IDEA (hybrid)** approaches.

---

## Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Java JDK | 17 | `java -version` |
| Maven | 3.8+ | `mvn --version` |
| Docker | 20+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |
| IntelliJ IDEA | Any (Community/Ultimate) | Optional, for hybrid setup |

### Install Docker (Ubuntu 22.04)

If Docker is not installed:

```bash
# Install prerequisites
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# Add Docker GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine + Compose
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow running docker without sudo
sudo usermod -aG docker $USER
newgrp docker    # or log out and log back in
```

---

## Option A: Full Docker Compose (Recommended for quick start)

Everything runs inside containers. No local Java/Maven needed after build.

### Step 1: Build the project

```bash
cd iot-backend
mvn clean install -DskipTests
```

This creates JAR files in each service's `target/` directory. The Dockerfiles copy these JARs into the container images.

> **If this fails**, ensure Java 17 and Maven 3.8+ are installed. Run `java -version` and `mvn --version` to verify.

### Step 2: Start all services

```bash
docker compose up --build -d
```

This starts (in dependency order):
1. `auth-db` (PostgreSQL on port 5432)
2. `device-db` (PostgreSQL on port 5433)
3. `discovery-server` (Eureka on port 8761)
4. `auth-service` (port 8081)
5. `device-service` (port 8082)
6. `weather-service` (port 8084)
7. `api-gateway` (port 8080)

### Step 3: Seed the databases (optional, for sample data)

Wait about 15 seconds for databases to initialize, then run:

```bash
docker exec -i auth-db psql -U iot_user -d auth_db < init-auth-db.sql
docker exec -i device-db psql -U iot_user -d device_db < init-device-db.sql
```

### Step 4: Verify

```bash
# Check all containers are running
docker compose ps

# Check Eureka dashboard - all 4 services should appear
# Open: http://localhost:8761

# Test API
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "pitabash@example.com", "password": "password123"}'
```

### Useful Docker commands

```bash
docker compose logs -f                    # Follow all logs
docker compose logs -f auth-service       # Follow specific service
docker compose ps                         # Check container status
docker compose restart auth-service       # Restart one service
docker compose down                       # Stop everything
docker compose down -v                    # Stop + delete database volumes (clean slate)
```

---

## Option B: Hybrid (Docker DBs + IntelliJ services)

Use Docker for databases only. Run microservices from IntelliJ IDEA for debugging, hot-reload, and breakpoints.

### Step 1: Start databases only

```bash
cd iot-backend
docker compose up auth-db device-db -d
```

### Step 2: Seed the databases (optional)

```bash
docker exec -i auth-db psql -U iot_user -d auth_db < init-auth-db.sql
docker exec -i device-db psql -U iot_user -d device_db < init-device-db.sql
```

### Step 3: Open project in IntelliJ IDEA

1. **File > Open** > select the `iot-backend` folder
2. Wait for Maven import to complete (watch the progress bar)
3. **Build > Build Project** (or `mvn clean install -DskipTests` from terminal)

### Step 4: Create Run Configurations

Go to **Run > Edit Configurations > + > Spring Boot** for each service.

Start them **in this order**:

| # | Service | Main Class | Module | Env Vars |
|---|---------|------------|--------|----------|
| 1 | Discovery Server | `com.foodchain.discovery.DiscoveryServerApplication` | discovery-server | *(none needed)* |
| 2 | Auth Service | `com.foodchain.auth.AuthServiceApplication` | auth-service | *(none needed)* |
| 3 | Device Service | `com.foodchain.device.DeviceServiceApplication` | device-service | `DB_PORT=5433` |
| 4 | Weather Service | `com.foodchain.weather.WeatherServiceApplication` | weather-service | `WEATHER_API_KEY=your_key` (optional) |
| 5 | API Gateway | `com.foodchain.gateway.ApiGatewayApplication` | api-gateway | *(none needed)* |

> **Important:** Device Service needs `DB_PORT=5433` because docker-compose maps device-db to host port 5433 (to avoid conflict with auth-db on 5432). When running inside Docker, both DBs use port 5432 internally.

### Step 5: Verify

Open http://localhost:8761 - all 4 services (AUTH-SERVICE, DEVICE-SERVICE, WEATHER-SERVICE, API-GATEWAY) should be registered.

### IntelliJ Tip

Use **View > Tool Windows > Services** to see all Spring Boot run configurations in a single panel. Start/stop/restart all services from one place.

---

## Option C: Mixed (some in Docker, some in IntelliJ)

Useful when you're actively developing one service but need the rest running.

```bash
# Start everything in Docker
docker compose up --build -d

# Stop the service you want to debug
docker compose stop auth-service

# Run it from IntelliJ with these env vars:
#   DB_HOST=localhost
#   EUREKA_URI=http://localhost:8761/eureka/
```

The IntelliJ-run service will register with the Dockerized Eureka and connect to the Dockerized database.

---

## Service URLs

| Service | Port | URL |
|---------|------|-----|
| API Gateway | 8080 | http://localhost:8080 |
| Eureka Dashboard | 8761 | http://localhost:8761 |
| Auth Service (direct) | 8081 | http://localhost:8081 |
| Device Service (direct) | 8082 | http://localhost:8082 |
| Weather Service (direct) | 8084 | http://localhost:8084 |
| auth-db (PostgreSQL) | 5432 | `jdbc:postgresql://localhost:5432/auth_db` |
| device-db (PostgreSQL) | 5433 | `jdbc:postgresql://localhost:5433/device_db` |

---

## Seed Data Reference

### Users (auth_db)

| Name | Email | Password |
|------|-------|----------|
| Pitabash Admin | pitabash@example.com | password123 |
| John Doe | john@example.com | password123 |
| Jane Smith | jane@example.com | password123 |

### Devices (device_db)

| Device | Type | Owner | Location | Controls |
|--------|------|-------|----------|----------|
| Living Room Light | LIGHT | Pitabash | Bhubaneswar | Power, Brightness, Color |
| Bedroom AC | THERMOSTAT | Pitabash | Bhubaneswar | Power, Temperature, Mode |
| Front Door Camera | CAMERA | Pitabash | Bhubaneswar | Recording, Night Vision |
| Kitchen Fan | FAN | Pitabash | Bhubaneswar | Power, Speed |
| Front Door Lock | LOCK | Pitabash | Bhubaneswar | Lock, Auto Lock |
| Office Light | LIGHT | John | New Delhi | Power, Brightness |
| Temperature Sensor | SENSOR | John | New Delhi | Reading |
| Garden Sprinkler | SWITCH | Jane | Mumbai | Power, Duration |

### Re-seed (reset data)

```bash
# Drop and recreate (if tables already exist with conflicting data)
docker exec auth-db psql -U iot_user -d auth_db -c "DROP TABLE IF EXISTS refresh_tokens, users CASCADE;"
docker exec device-db psql -U iot_user -d device_db -c "DROP TABLE IF EXISTS device_controls, devices CASCADE;"

# Re-seed
docker exec -i auth-db psql -U iot_user -d auth_db < init-auth-db.sql
docker exec -i device-db psql -U iot_user -d device_db < init-device-db.sql
```

---

## Database Connection (for DB clients)

Connect using IntelliJ Database tool, DBeaver, pgAdmin, or any PostgreSQL client:

**auth_db:**
```
Host: localhost
Port: 5432
Database: auth_db
Username: iot_user
Password: iot_pass
```

**device_db:**
```
Host: localhost
Port: 5433
Database: device_db
Username: iot_user
Password: iot_pass
```

---

## Environment Variables

| Variable | Default | Used By | Description |
|----------|---------|---------|-------------|
| `JWT_SECRET` | `default-secret-key...` | Gateway, Auth, Device, Weather | JWT signing key (min 256 bits) |
| `DB_HOST` | `localhost` | Auth, Device | PostgreSQL host |
| `DB_PORT` | `5432` | Auth, Device | PostgreSQL port |
| `DB_NAME` | `auth_db` / `device_db` | Auth, Device | Database name |
| `DB_USERNAME` | `iot_user` | Auth, Device | Database username |
| `DB_PASSWORD` | `iot_pass` | Auth, Device | Database password |
| `EUREKA_URI` | `http://localhost:8761/eureka/` | All services | Eureka server URL |
| `MQTT_BROKER_URL` | `tcp://broker.hivemq.com:1883` | Device | MQTT broker URL |
| `WEATHER_API_KEY` | *(empty)* | Weather | OpenWeatherMap API key |

---

## Troubleshooting

### `COPY target/*.jar app.jar` fails during docker build
**Cause:** JARs not built yet.
**Fix:** Run `mvn clean install -DskipTests` before `docker compose up --build`.

### `Could not resolve placeholder 'app.jwt.secret'`
**Cause:** `device-service` or `weather-service` missing JWT config. The `JwtTokenProvider` bean from `common-lib` is component-scanned by all services and requires `app.jwt.*` properties.
**Fix:** Ensure `app.jwt.secret`, `app.jwt.access-token-expiration-ms`, and `app.jwt.refresh-token-expiration-ms` are defined in each service's `application.yml`. This is already fixed in the current codebase.

### `Connection refused` to database
**Cause:** PostgreSQL container not ready yet.
**Fix:** Wait 10-15 seconds after `docker compose up` for healthchecks to pass. Check with `docker compose ps` - status should show `healthy`.

### Device Service can't connect to DB in hybrid mode
**Cause:** device-db runs on host port **5433**, but the app defaults to 5432.
**Fix:** Set `DB_PORT=5433` in IntelliJ run configuration environment variables.

### `permission denied` when running docker
**Cause:** User not in the docker group.
**Fix:**
```bash
sudo usermod -aG docker $USER
newgrp docker    # or log out and log back in
```

### Port already in use
**Cause:** Another process or a previous container is using the port.
**Fix:**
```bash
# Find what's using the port
sudo lsof -i :8080

# Or stop all containers first
docker compose down
```

### Services not appearing in Eureka
**Cause:** Discovery server wasn't ready when services started.
**Fix:** In Docker Compose, `depends_on` with healthchecks handles this. If running from IntelliJ, always start Discovery Server first and wait for it to be up at http://localhost:8761 before starting other services.

### `version` warning in docker-compose
**Warning:** `the attribute 'version' is obsolete`
**Fix:** Already removed. The `version` key is no longer needed in modern Docker Compose.

---

## Quick Start (TL;DR)

```bash
git clone <repo-url>
cd iot-backend
mvn clean install -DskipTests
docker compose up --build -d

# Optional: seed sample data
sleep 15
docker exec -i auth-db psql -U iot_user -d auth_db < init-auth-db.sql
docker exec -i device-db psql -U iot_user -d device_db < init-device-db.sql

# Verify
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "pitabash@example.com", "password": "password123"}'
```
