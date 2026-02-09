package com.foodchain.common.exception;

public record ErrorResponse(
        int status,
        String error,
        String message
) {}
