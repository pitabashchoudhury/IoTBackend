package com.foodchain.auth.controller;

import com.foodchain.auth.service.AuthService;
import com.foodchain.common.dto.auth.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final AuthService authService;

    public InternalUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.getCurrentUser(id));
    }
}
