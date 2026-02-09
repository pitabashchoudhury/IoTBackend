package com.foodchain.common.dto.mqtt;

public record ControlCommandDto(
        String deviceId,
        String controlId,
        String value
) {}
