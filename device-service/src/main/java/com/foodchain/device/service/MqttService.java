package com.foodchain.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodchain.device.repository.DeviceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MqttService implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final MqttClient mqttClient;
    private final DeviceRepository deviceRepository;
    private final WebSocketNotificationService webSocketService;
    private final ObjectMapper objectMapper;

    @Value("${app.mqtt.qos}")
    private int qos;

    public MqttService(MqttClient mqttClient,
                       DeviceRepository deviceRepository,
                       WebSocketNotificationService webSocketService,
                       ObjectMapper objectMapper) {
        this.mqttClient = mqttClient;
        this.deviceRepository = deviceRepository;
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        mqttClient.setCallback(this);
        subscribeToTopics();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            log.error("Error disconnecting from MQTT broker", e);
        }
    }

    private void subscribeToTopics() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.subscribe("devices/#", qos);
                log.info("Subscribed to devices/#");
            }
        } catch (MqttException e) {
            log.error("Error subscribing to MQTT topics", e);
        }
    }

    public void publish(String topic, String payload) {
        try {
            if (mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(qos);
                mqttClient.publish(topic, message);
                log.debug("Published to {}: {}", topic, payload);
            }
        } catch (MqttException e) {
            log.error("Error publishing MQTT message to {}", topic, e);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT {} to {}", reconnect ? "reconnected" : "connected", serverURI);
        subscribeToTopics();
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        log.debug("MQTT message received on {}: {}", topic, payload);

        try {
            String[] parts = topic.split("/");
            if (parts.length >= 3 && "devices".equals(parts[0])) {
                String deviceId = parts[1];
                String messageType = parts[2];

                switch (messageType) {
                    case "status" -> handleStatusMessage(deviceId, payload);
                    case "control" -> webSocketService.sendDeviceControl(deviceId, payload);
                    case "telemetry" -> webSocketService.sendDeviceTelemetry(deviceId, payload);
                }

                if ("status".equals(messageType)) {
                    webSocketService.sendDeviceStatus(deviceId, payload);
                }
            }
        } catch (Exception e) {
            log.error("Error processing MQTT message from {}", topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    private void handleStatusMessage(String deviceId, String payload) {
        try {
            UUID id = UUID.fromString(deviceId);
            deviceRepository.findById(id).ifPresent(device -> {
                try {
                    JsonNode node = objectMapper.readTree(payload);
                    if (node.has("is_online")) {
                        device.setOnline(node.get("is_online").asBoolean());
                        deviceRepository.save(device);
                        log.debug("Updated device {} online status to {}", deviceId, device.isOnline());
                    }
                } catch (Exception e) {
                    log.error("Error parsing status payload for device {}", deviceId, e);
                }
            });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid device ID in MQTT topic: {}", deviceId);
        }
    }
}
