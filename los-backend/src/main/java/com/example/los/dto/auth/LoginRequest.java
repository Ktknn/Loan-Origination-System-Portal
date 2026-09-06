package com.example.los.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO — nhận từ frontend khi đăng nhập.
 * Map với: POST /api/auth/login
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
