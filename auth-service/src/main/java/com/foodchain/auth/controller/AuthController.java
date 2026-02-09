package com.foodchain.auth.controller;

import com.foodchain.auth.service.AuthService;
import com.foodchain.common.dto.auth.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(authService.refresh(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(authService.getCurrentUser(UUID.fromString(userId)));
    }
}
