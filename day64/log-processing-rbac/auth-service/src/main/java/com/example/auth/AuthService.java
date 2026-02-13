package com.example.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      JwtService jwtService,
                      RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account disabled");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken();

        // Store refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUserId(user.getId());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiry()));
        refreshTokenRepository.save(refreshToken);

        log.info("User {} logged in successfully", user.getUsername());

        return new AuthResponse(accessToken, refreshTokenValue, user);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        // Check Redis blacklist
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:refresh:" + refreshTokenValue))) {
            throw new RuntimeException("Refresh token blacklisted");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Rotate refresh token (revoke old, issue new)
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Blacklist old token in Redis
        redisTemplate.opsForValue().set(
                "blacklist:refresh:" + refreshTokenValue,
                "true",
                jwtService.getRefreshTokenExpiry(),
                TimeUnit.SECONDS
        );

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenValue = jwtService.generateRefreshToken();

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(newRefreshTokenValue);
        newRefreshToken.setUserId(user.getId());
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiry()));
        refreshTokenRepository.save(newRefreshToken);

        log.info("Tokens refreshed for user {}", user.getUsername());

        return new AuthResponse(newAccessToken, newRefreshTokenValue, user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);

            // Add to Redis blacklist
            redisTemplate.opsForValue().set(
                    "blacklist:refresh:" + refreshTokenValue,
                    "true",
                    jwtService.getRefreshTokenExpiry(),
                    TimeUnit.SECONDS
            );
        });

        log.info("User logged out, refresh token revoked");
    }

    public String getPublicKey() {
        // In production, use RSA key pair and return public key
        // For this demo, we're using HMAC so return placeholder
        return "HMAC-based-validation-use-shared-secret";
    }
}
