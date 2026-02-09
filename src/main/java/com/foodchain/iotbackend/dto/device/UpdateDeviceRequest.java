package com.foodchain.iotbackend.dto.device;

import java.util.List;

public record UpdateDeviceRequest(
        String name,
        String type,
        DeviceLocationDto location,
        List<DeviceControlDto> controls,
        String mqttTopicPrefix
) {}
