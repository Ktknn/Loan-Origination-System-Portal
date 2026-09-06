package com.example.los.service;

import com.example.los.dto.auth.AuthResponse;
import com.example.los.dto.auth.LoginRequest;
import com.example.los.dto.auth.RefreshResponse;
import com.example.los.dto.auth.RegisterRequest;
import com.example.los.entity.RefreshToken;
import com.example.los.entity.User;
import com.example.los.entity.enums.UserStatus;
import com.example.los.repository.UserRepository;
import com.example.los.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // =========================================================
    // REGISTER
    // =========================================================
    @Transactional
    public AuthResponse register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Tài khoản với Email này đã tồn tại.");
        }
        if (userRepository.existsByCccd(req.getCccd())) {
            throw new RuntimeException("Tài khoản với CCCD này đã tồn tại.");
        }

        // Parse ngày sinh
        LocalDate birthdate = null;
        if (req.getBirthDate() != null && !req.getBirthDate().isBlank()) {
            birthdate = LocalDate.parse(req.getBirthDate()); // yyyy-MM-dd
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .cccd(req.getCccd())
                .phoneNumber(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .birthdate(birthdate)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());
        return AuthResponse.from(user, accessToken, refreshToken.getToken());
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public AuthResponse login(LoginRequest req) {

        User user = userRepository
                .findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu không chính xác.");
        }

        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());
        return AuthResponse.from(user, accessToken, refreshToken.getToken());
    }

    // =========================================================
    // REFRESH — validate refresh token, rotate, issue new access token
    // =========================================================
    @Transactional
    public RefreshResponse refresh(String refreshTokenValue) {
        RefreshToken rotated = refreshTokenService.validateAndRotate(refreshTokenValue);

        User user = userRepository.findById(rotated.getUserId())
                .orElseThrow(() -> new RuntimeException("User không tồn tại."));

        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getEmail());
        return RefreshResponse.builder()
                .accessToken(accessToken)
                .newRefreshToken(rotated.getToken())
                .build();
    }

    // =========================================================
    // LOGOUT — xóa refresh token của user khỏi DB
    // =========================================================
    @Transactional
    public void logout(String userId) {
        refreshTokenService.deleteByUserId(userId);
    }
}


