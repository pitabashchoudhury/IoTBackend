package com.foodchain.iotbackend.dto.auth;

public record UserDto(
        String id,
        String email,
        String name,
        String profileImageUrl
) {}
