package com.foodchain.iotbackend.dto.mqtt;

public record ControlCommandDto(
        String deviceId,
        String controlId,
        String value
) {}
