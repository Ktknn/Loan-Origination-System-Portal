package com.example.los.dto.auth;

import com.example.los.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO — trả về cho frontend sau khi login/register thành công.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;   // short-lived JWT (15 min)
    private String refreshToken;  // long-lived (7 days) — used server-side to set cookie
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String cccd;
    private String dob;           // yyyy-MM-dd

    public static AuthResponse from(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .cccd(user.getCccd())
                .dob(user.getBirthdate() != null ? user.getBirthdate().toString() : "")
                .build();
    }
}
