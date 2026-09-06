package com.example.los.entity;

import com.example.los.entity.enums.IncomeRange;
import com.example.los.entity.enums.LoanPurpose;
import com.example.los.entity.enums.LoanStatus;
import com.example.los.entity.enums.Occupation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Loan_Applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplication {

    @Id
    @UuidGenerator
    @Column(name = "loan_application_id", columnDefinition = "VARCHAR(36)")
    private String loanApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id")
    private LoanProduct loanProduct;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "term")
    private Integer term;

    @Column(name = "approve_interest_rate", precision = 10, scale = 2)
    private BigDecimal approveInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    @Builder.Default
    private LoanPurpose purpose = LoanPurpose.VAY_TIEU_DUNG_KHAC;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_range")
    private IncomeRange incomeRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupation")
    @Builder.Default
    private Occupation occupation = Occupation.KHAC;

    @Column(name = "reference_contact_name_1")
    private String referenceContactName1;

    @Column(name = "reference_contact_phone_1")
    private String referenceContactPhone1;

    @Column(name = "reference_contact_name_2")
    private String referenceContactName2;

    @Column(name = "reference_contact_phone_2")
    private String referenceContactPhone2;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "decision_date")
    private LocalDateTime decisionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private LoanStatus status = LoanStatus.DRAFT;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
