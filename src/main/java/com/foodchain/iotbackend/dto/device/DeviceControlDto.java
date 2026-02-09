package com.foodchain.iotbackend.dto.device;

import java.util.List;

public record DeviceControlDto(
        String id,
        String name,
        String controlType,
        String currentValue,
        Float minValue,
        Float maxValue,
        Float step,
        List<String> options,
        String mqttTopic
) {}
