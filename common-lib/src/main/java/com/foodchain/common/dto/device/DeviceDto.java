package com.foodchain.common.dto.device;

import java.util.List;

public record DeviceDto(
        String id,
        String name,
        String type,
        boolean isOnline,
        DeviceLocationDto location,
        List<DeviceControlDto> controls,
        String mqttTopicPrefix,
        Long createdAt,
        Long updatedAt
) {}
