package com.foodchain.iotbackend.service;

import com.foodchain.iotbackend.dto.auth.*;
import com.foodchain.iotbackend.entity.RefreshTokenEntity;
import com.foodchain.iotbackend.entity.UserEntity;
import com.foodchain.iotbackend.exception.BadRequestException;
import com.foodchain.iotbackend.exception.ResourceNotFoundException;
import com.foodchain.iotbackend.mapper.UserMapper;
import com.foodchain.iotbackend.repository.RefreshTokenRepository;
import com.foodchain.iotbackend.repository.UserRepository;
import com.foodchain.iotbackend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String bearerToken) {
        UUID userId = tokenProvider.getUserIdFromToken(bearerToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUserId(userId);

        return createAuthResponse(user);
    }

    public UserDto getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    private AuthResponse createAuthResponse(UserEntity user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId());
        String refreshToken = tokenProvider.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs()));
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }
}
