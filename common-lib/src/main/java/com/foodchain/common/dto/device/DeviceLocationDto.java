package com.foodchain.common.dto.device;

public record DeviceLocationDto(
        Double latitude,
        Double longitude,
        String address,
        String label
) {}
