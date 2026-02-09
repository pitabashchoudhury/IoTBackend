package com.foodchain.iotbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendDeviceStatus(String deviceId, String payload) {
        String destination = "/topic/devices/" + deviceId + "/status";
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Sent WebSocket message to {}", destination);
    }

    public void sendDeviceControl(String deviceId, String payload) {
        String destination = "/topic/devices/" + deviceId + "/control";
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Sent WebSocket message to {}", destination);
    }

    public void sendDeviceTelemetry(String deviceId, String payload) {
        String destination = "/topic/devices/" + deviceId + "/telemetry";
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Sent WebSocket message to {}", destination);
    }
}
