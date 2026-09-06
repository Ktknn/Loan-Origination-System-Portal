package com.example.los.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO — nhận từ frontend khi đăng ký.
 * Map với: POST /api/auth/register
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "^([0-9]{9}|[0-9]{12})$", message = "CCCD phải có 9 hoặc 12 chữ số")
    private String cccd;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String phone;

    /** Định dạng: yyyy-MM-dd */
    private String birthDate;
}
