package com.example.los.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.los.dto.assessment.AssessmentResponse;
import com.example.los.entity.ApplicationAssessment;
import com.example.los.service.AssessmentService;

import lombok.RequiredArgsConstructor;

/**
 * AssessmentController — trigger thẩm định tín dụng cho một hồ sơ vay.
 *
 * Endpoints (cần JWT — chỉ admin/staff gọi):
 *  POST /api/v1/assessment/{loanApplicationId}  → chạy evaluate(), trả kết quả
 */
@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    /**
     * POST /api/v1/assessment/{loanApplicationId}
     * Header: Authorization: Bearer {token}
     *
     * Response 200: AssessmentResponse { status, totalScore, incomeScore, ... }
     */
    @PostMapping("/{loanApplicationId}")
    public ResponseEntity<ApiResponse<AssessmentResponse>> evaluate(
            @PathVariable String loanApplicationId) {

        ApplicationAssessment result = assessmentService.evaluate(loanApplicationId);
        return ResponseEntity.ok(ApiResponse.success(AssessmentResponse.from(result)));
    }
}
