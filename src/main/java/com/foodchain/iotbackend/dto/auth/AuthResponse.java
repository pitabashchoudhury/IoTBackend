package com.foodchain.iotbackend.dto.auth;

public record AuthResponse(
        String token,
        String refreshToken,
        UserDto user
) {}
