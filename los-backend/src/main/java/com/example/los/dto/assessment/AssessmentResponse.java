package com.example.los.dto.assessment;

import com.example.los.entity.ApplicationAssessment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho kết quả thẩm định tín dụng.
 * Convert từ ApplicationAssessment entity.
 */
@Data
public class AssessmentResponse {

    private String assessmentId;
    private String loanApplicationId;
    private String status;          // PENDING | APPROVED | REJECTED

    // Score breakdown
    private Integer totalScore;
    private Integer incomeScore;
    private Integer occupationScore;
    private Integer purposeScore;
    private Integer loanSuitabilityScore;

    private BigDecimal dti;         // Debt-to-Income ratio (%)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AssessmentResponse from(ApplicationAssessment aa) {
        AssessmentResponse res = new AssessmentResponse();
        res.setAssessmentId(aa.getAssessmentId());
        res.setLoanApplicationId(aa.getLoanApplicationId());
        res.setStatus(aa.getStatus() != null ? aa.getStatus().name() : null);
        res.setTotalScore(aa.getTotalScore());
        res.setIncomeScore(aa.getIncomeScore());
        res.setOccupationScore(aa.getOccupationScore());
        res.setPurposeScore(aa.getPurposeScore());
        res.setLoanSuitabilityScore(aa.getLoanSuitabilityScore());
        res.setDti(aa.getDti());
        res.setCreatedAt(aa.getCreatedAt());
        res.setUpdatedAt(aa.getUpdatedAt());
        return res;
    }
}
