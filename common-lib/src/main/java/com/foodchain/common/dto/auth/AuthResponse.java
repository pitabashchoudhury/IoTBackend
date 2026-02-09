package com.foodchain.common.dto.auth;

public record AuthResponse(
        String token,
        String refreshToken,
        UserDto user
) {}
