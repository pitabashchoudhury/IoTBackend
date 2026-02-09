package com.foodchain.iotbackend.controller;

import com.foodchain.iotbackend.dto.auth.*;
import com.foodchain.iotbackend.security.UserPrincipal;
import com.foodchain.iotbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> refresh(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.refresh(principal.getId().toString()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }
}
