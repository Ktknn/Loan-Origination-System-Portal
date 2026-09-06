package com.example.los.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.los.entity.LoanProduct;

public interface LoanProductRepository extends JpaRepository<LoanProduct, String> {

    List<LoanProduct> findByAmountAndTerm(BigDecimal amount, Integer term);
}
