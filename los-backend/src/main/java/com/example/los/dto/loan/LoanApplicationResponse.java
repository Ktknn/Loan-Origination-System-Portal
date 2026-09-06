package com.example.los.dto.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LoanApplicationResponse {
    // Loan Application Info
    private String loanApplicationId;
    private String userId;
    private String loanProductId;
    private String status;
    private BigDecimal amount;
    private Integer term;
    private Double interestRate; // Approved interest rate
    private String purpose;
    private String incomeRange;
    private String occupation;
    private LocalDateTime submittedAt;
    private LocalDateTime decisionDate;

    // Personal Info
    private String fullName;
    private String cccd;
    private String email;
    private String phoneNumber;
    private String gender;
    private String birthDate;
    private String address;

    // Reference Info
    private String ref1Name;
    private String ref1Phone;
    private String ref2Name;
    private String ref2Phone;

    // Assessment Info (optional, join if needed)
    private Integer totalScore;
    private Double dti;
    
    // Additional useful fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Convert từ LoanApplication entity → Response DTO */
    public static LoanApplicationResponse from(com.example.los.entity.LoanApplication app) {
        LoanApplicationResponse res = new LoanApplicationResponse();
        res.setLoanApplicationId(app.getLoanApplicationId());
        res.setUserId(app.getUser() != null ? app.getUser().getUserId() : null);
        res.setLoanProductId(app.getLoanProduct() != null ? app.getLoanProduct().getLoanProductId() : null);
        res.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
        res.setAmount(app.getAmount());
        res.setTerm(app.getTerm());
        res.setInterestRate(app.getApproveInterestRate() != null ? app.getApproveInterestRate().doubleValue() : null);
        res.setPurpose(app.getPurpose() != null ? app.getPurpose().getLabel() : null);
        res.setIncomeRange(app.getIncomeRange() != null ? app.getIncomeRange().getLabel() : null);
        res.setOccupation(app.getOccupation() != null ? app.getOccupation().getLabel() : null);
        res.setSubmittedAt(app.getSubmittedAt());
        res.setDecisionDate(app.getDecisionDate());
        res.setRef1Name(app.getReferenceContactName1());
        res.setRef1Phone(app.getReferenceContactPhone1());
        res.setRef2Name(app.getReferenceContactName2());
        res.setRef2Phone(app.getReferenceContactPhone2());
        res.setCreatedAt(app.getCreatedAt());
        res.setUpdatedAt(app.getUpdatedAt());
        // User info
        if (app.getUser() != null) {
            res.setFullName(app.getUser().getFullName());
            res.setCccd(app.getUser().getCccd());
            res.setEmail(app.getUser().getEmail());
            res.setPhoneNumber(app.getUser().getPhoneNumber());
            res.setAddress(app.getUser().getAddress());
        }
        return res;
    }
}
