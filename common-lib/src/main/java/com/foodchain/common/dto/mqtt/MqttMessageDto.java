package com.foodchain.common.dto.mqtt;

public record MqttMessageDto(
        String topic,
        String payload,
        int qos,
        boolean retained,
        long timestamp
) {}
