package com.example.los.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Policy {

    @Id
    @UuidGenerator
    @Column(name = "policy_id", columnDefinition = "VARCHAR(36)")
    private String policyId;

    @Column(name = "policy_name")
    private String policyName;

    @Column(name = "max_loan_amount", precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    @Column(name = "min_credit_score")
    private Integer minCreditScore;

    @Column(name = "required_income_annual", precision = 15, scale = 2)
    private BigDecimal requiredIncomeAnnual;

    @Column(name = "required_employment_length_months")
    private Integer requiredEmploymentLengthMonths;

    @Column(name = "dti_threshold", precision = 10, scale = 2)
    private BigDecimal dtiThreshold;


    @Column(name = "active")
    @Builder.Default
    private String active = "active";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
