package com.example.los.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import com.example.los.entity.enums.AssessmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "application_assessment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationAssessment {

    @Id
    @UuidGenerator
    @Column(name = "assessment_id", columnDefinition = "VARCHAR(36)")
    private String assessmentId;

    @Column(name = "loan_application_id", unique = true, nullable = false)
    private String loanApplicationId;

    /** income + occupation + purpose + loan_suitability */
    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "income_score")
    private Integer incomeScore;

    @Column(name = "occupation_score")
    private Integer occupationScore;

    @Column(name = "purpose_score")
    private Integer purposeScore;

    @Column(name = "loan_suitability_score")
    private Integer loanSuitabilityScore;

    /** Debt-to-Income ratio (%) */
    @Column(name = "dti", precision = 10, scale = 2)
    private BigDecimal dti;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.PENDING;

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
