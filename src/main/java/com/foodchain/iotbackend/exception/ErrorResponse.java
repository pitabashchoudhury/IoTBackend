package com.foodchain.iotbackend.exception;

public record ErrorResponse(
        int status,
        String error,
        String message
) {}
