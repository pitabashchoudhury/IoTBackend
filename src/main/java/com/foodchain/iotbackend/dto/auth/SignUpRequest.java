package com.foodchain.iotbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
) {}
