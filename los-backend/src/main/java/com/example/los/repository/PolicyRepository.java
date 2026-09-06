package com.example.los.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.los.entity.Policy;

public interface PolicyRepository extends JpaRepository<Policy, String> {

    List<Policy> findByActiveOrderByMinCreditScoreDesc(String active);

    List<Policy> findByActiveOrderByMaxLoanAmountAsc(String active);

    Optional<Policy> findFirstByActiveAndMaxLoanAmountGreaterThanEqualOrderByMaxLoanAmountAsc(String active, java.math.BigDecimal maxLoanAmount);

    Optional<Policy> findByMinCreditScore(int minCreditScore);
    
    Optional<Policy> findByMaxLoanAmount(double maxLoanAmount);

}
