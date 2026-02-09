package com.foodchain.common.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateDeviceRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotNull DeviceLocationDto location,
        List<DeviceControlDto> controls,
        String mqttTopicPrefix
) {}
