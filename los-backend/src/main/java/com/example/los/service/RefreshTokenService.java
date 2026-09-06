package com.example.los.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.los.config.JwtProperties;
import com.example.los.entity.RefreshToken;
import com.example.los.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000))
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validateAndRotate(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token khong hop le."));

        if (existing.isExpired()) {
            refreshTokenRepository.delete(existing);
            throw new RuntimeException("Refresh token da het han. Vui long dang nhap lai.");
        }

        String userId = existing.getUserId();
        refreshTokenRepository.delete(existing);

        RefreshToken newToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000))
                .createdAt(LocalDateTime.now())
                .build();

        log.info("[RefreshTokenService] Rotated refresh token for userId={}", userId);
        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public void deleteByUserId(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("[RefreshTokenService] Deleted all refresh tokens for userId={}", userId);
    }
}