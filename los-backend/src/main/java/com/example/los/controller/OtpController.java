package com.example.los.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.los.dto.otp.OtpSendRequest;
import com.example.los.dto.otp.OtpVerifyRequest;
import com.example.los.service.OtpService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * OtpController — xử lý gửi và xác thực OTP.
 *
 * Endpoints (public — không cần JWT, khai báo trong SecurityConfig):
 *  POST /api/v1/otp/send   → gửi OTP qua email
 *  POST /api/v1/otp/verify → xác thực OTP
 */
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    /**
     * POST /api/v1/otp/send
     * Body: { email }
     * Response 200: { message: "OTP đã được gửi đến email của bạn" }
     *
     * Overload không có loan info — dùng cho trường hợp gửi OTP đơn giản.
     * Nếu muốn kèm thông tin vay, thêm fullName/amount/term vào OtpSendRequest.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody OtpSendRequest req) {
        otpService.sendOTP(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success("OTP đã được gửi đến email của bạn"));
    }

    /**
     * POST /api/v1/otp/verify
     * Body: { email, otp }
     * Response 200: { message: "Xác thực thành công" }
     * Response 400: { message: "OTP không hợp lệ hoặc đã hết hạn" }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody OtpVerifyRequest req) {
        otpService.verifyOTP(req.getEmail(), req.getOtp());
        return ResponseEntity.ok(ApiResponse.success("Xác thực OTP thành công"));
    }
}
