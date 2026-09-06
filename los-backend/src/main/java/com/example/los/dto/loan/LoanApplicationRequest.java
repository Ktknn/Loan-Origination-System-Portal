package com.example.los.dto.loan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LoanApplicationRequest {
    private String loanProductId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Term is required")
    @Min(value = 1, message = "Term must be at least 1")
    @Max(value = 48, message = "Term must not exceed 48")
    private Integer term;

    // Personal & Contact Information
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "CCCD is required")
    private String cccd;

    @NotBlank(message = "Email is required")
    private String email;

    private String phoneNumber;

    private String gender;

    private String birthDate;

    private String address;

    // Employment Information
    private String occupation;

    private String incomeRange;

    // Loan Details
    private String purpose;

    // Reference Contacts
    private String ref1Name;

    private String ref1Phone;

    private String ref2Name;

    private String ref2Phone;
}
