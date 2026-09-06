package com.example.los.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.los.dto.auth.AuthResponse;
import com.example.los.dto.auth.LoginRequest;
import com.example.los.dto.auth.RefreshResponse;
import com.example.los.dto.auth.RegisterRequest;
import com.example.los.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AuthController - dang ky / dang nhap / refresh / logout.
 *
 * POST /api/v1/auth/register  - dang ky, tra accessToken (body) + refreshToken (HttpOnly cookie)
 * POST /api/v1/auth/login     - dang nhap, tra accessToken (body) + refreshToken (HttpOnly cookie)
 * POST /api/v1/auth/refresh   - silent refresh; doc cookie, tra accessToken moi (body) + cookie moi
 * POST /api/v1/auth/logout    - xoa refresh token DB + clear cookie
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final long COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60L;

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse response = authService.register(req);
        ResponseCookie cookie = buildRefreshCookie(response.getRefreshToken(), COOKIE_MAX_AGE_SECONDS);
        response.setRefreshToken(null);
        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.login(req);
        ResponseCookie cookie = buildRefreshCookie(response.getRefreshToken(), COOKIE_MAX_AGE_SECONDS);
        response.setRefreshToken(null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshTokenValue) {

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.<RefreshResponse>error("Khong co refresh token.", null));
        }

        RefreshResponse refreshResponse = authService.refresh(refreshTokenValue);
        ResponseCookie cookie = buildRefreshCookie(refreshResponse.getNewRefreshToken(), COOKIE_MAX_AGE_SECONDS);
        refreshResponse.setNewRefreshToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(refreshResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String userId = auth.getPrincipal().toString();
            authService.logout(userId);
        }
        ResponseCookie cleared = buildRefreshCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(ApiResponse.success(null));
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value != null ? value : "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }
}