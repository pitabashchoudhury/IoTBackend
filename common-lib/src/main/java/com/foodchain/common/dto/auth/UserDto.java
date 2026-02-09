package com.foodchain.common.dto.auth;

public record UserDto(
        String id,
        String email,
        String name,
        String profileImageUrl
) {}
