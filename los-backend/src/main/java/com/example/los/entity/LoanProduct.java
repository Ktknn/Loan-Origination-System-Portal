package com.example.los.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Loan_Products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProduct {

    @Id
    @UuidGenerator
    @Column(name = "loan_product_id", columnDefinition = "VARCHAR(36)")
    private String loanProductId;

    @Column(name = "name")
    private String name;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "term")
    private Integer term;

    /** Monthly interest rate (%) */
    @Column(name = "interest_rate", precision = 10, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "status")
    @Builder.Default
    private String status = "active";

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
