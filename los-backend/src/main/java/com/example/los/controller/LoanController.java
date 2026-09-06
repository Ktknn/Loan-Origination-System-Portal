package com.example.los.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.los.dto.loan.LoanApplicationRequest;
import com.example.los.dto.loan.LoanApplicationResponse;
import com.example.los.service.LoanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LoanController — xử lý nộp hồ sơ vay và xem lịch sử.
 *
 * Endpoints:
 *  POST /api/v1/portal/submit        → nộp hồ sơ vay (cần JWT)
 *  GET  /api/v1/portal/history/{cccd}→ lịch sử hồ sơ của user (cần JWT)
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService; // I: chỉ inject LoanService, không inject gì thừa

    /**
     * POST /api/v1/portal/submit
     * Header: Authorization: Bearer {token}
     * Body: { fullName, cccd, email, phoneNumber, address, birthDate,
     *         amount, term, purpose, incomeRange, occupation,
     *         ref1Name, ref1Phone, ref2Name, ref2Phone }
     * Response 201: LoanApplicationResponse
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> submit(
            @Valid @RequestBody LoanApplicationRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        LoanApplicationResponse response = loanService.submit(req);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/portal/history/{cccd}?fromDate=yyyy-MM-dd&toDate=yyyy-MM-dd
     * Header: Authorization: Bearer {token}
     * Response 200: List<LoanApplicationResponse>
     */
    @GetMapping("/history/{cccd}")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponse>>> getHistory(
            @PathVariable String cccd,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<LoanApplicationResponse> history = loanService.getHistory(cccd, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
