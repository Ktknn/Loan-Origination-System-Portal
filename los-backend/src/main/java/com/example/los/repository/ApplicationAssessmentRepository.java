package com.example.los.repository;

import com.example.los.entity.ApplicationAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplicationAssessmentRepository extends JpaRepository<ApplicationAssessment, String> {

    Optional<ApplicationAssessment> findByLoanApplicationId(String loanApplicationId);
}
