package com.example.los.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — xử lý lỗi tập trung theo SOLID:
 *
 *  S - Tách riêng exception handling ra khỏi business logic của Controller.
 *  O - Thêm loại lỗi mới → chỉ thêm @ExceptionHandler method, không sửa code cũ.
 *
 * Tất cả lỗi đều trả về cùng cấu trúc ApiResponse để frontend xử lý nhất quán.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 — Validation lỗi (@Valid thất bại).
     * Trả về map field → message để frontend highlight từng field.
     * Ví dụ: { "email": "Email không hợp lệ", "cccd": "CCCD phải có 9 hoặc 12 chữ số" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Giá trị không hợp lệ",
                        (first, second) -> first  // giữ lỗi đầu tiên nếu trùng field
                ));

        String firstError = errors.isEmpty() ? "Dữ liệu không hợp lệ" : errors.values().iterator().next();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dữ liệu không hợp lệ: " + firstError, errors));
    }

    /**
     * 400 — IllegalArgumentException (ví dụ: OTP hết hạn, sai mã OTP).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 400 — RuntimeException từ service layer (ví dụ: email đã tồn tại, sai mật khẩu).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 500 — Mọi lỗi khác không được xử lý cụ thể.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."));
    }
}
