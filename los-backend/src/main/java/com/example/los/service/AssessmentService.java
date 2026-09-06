package com.example.los.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.los.entity.ApplicationAssessment;
import com.example.los.entity.LoanApplication;
import com.example.los.entity.Policy;
import com.example.los.entity.enums.AssessmentStatus;
import com.example.los.entity.enums.LoanStatus;
import com.example.los.repository.ApplicationAssessmentRepository;
import com.example.los.repository.LoanApplicationRepository;
import com.example.los.repository.PolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final ApplicationAssessmentRepository applicationAssessmentRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final PolicyRepository policyRepository;

    @Transactional
    public ApplicationAssessment evaluate(String loanApplicationId) {
        LoanApplication loanApplication = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanApplicationId));

        double dti = calculateDTI(loanApplication);
        LocalDate dob = loanApplication.getUser() != null ? loanApplication.getUser().getBirthdate() : null;
        int age = calculateAge(dob);

        Policy policy = findApplicablePolicy(loanApplication.getAmount() != null ? loanApplication.getAmount().doubleValue() : 0.0);
        double dtiThreshold = (policy != null && policy.getDtiThreshold() != null) ? policy.getDtiThreshold().doubleValue() : 60.0;
        int minCreditScore = (policy != null && policy.getMinCreditScore() != null) ? policy.getMinCreditScore() : 50;

        int incomeScore = loanApplication.getIncomeRange() != null ? loanApplication.getIncomeRange().getScore() : 20;
        int purposeScore = loanApplication.getPurpose() != null ? loanApplication.getPurpose().getScore() : 15;
        int occupationScore = loanApplication.getOccupation() != null ? loanApplication.getOccupation().getScore() : 15;
        int loanSuitabilityScore = purposeScore + occupationScore;
        int totalScore = (int) Math.round(((incomeScore * 0.4) + (loanSuitabilityScore * 0.4)) + (Math.max(0, 100 - (dti * 2)) * 0.2));

        AssessmentStatus assessmentStatus;
        LoanStatus loanStatus;

        // Knock-out rule 1: Age
        if (dob != null && (age < 18 || age > 60)) {
            assessmentStatus = AssessmentStatus.REJECTED;
            loanStatus = LoanStatus.REJECTED;
            log.info("[AssessmentService] Loan {} rejected by age knock-out rule (age={}, required 18-60)", loanApplicationId, age);
        }
        // Knock-out rule 2: DTI threshold
        else if (dti > dtiThreshold) {
            assessmentStatus = AssessmentStatus.REJECTED;
            loanStatus = LoanStatus.REJECTED;
            log.info("[AssessmentService] Loan {} rejected by DTI rule (dti={}% > threshold={}%)", loanApplicationId, dti, dtiThreshold);
        }
        // Score evaluation
        else if (totalScore >= minCreditScore) {
            assessmentStatus = AssessmentStatus.APPROVED;
            loanStatus = LoanStatus.APPROVED;
            log.info("[AssessmentService] Loan {} APPROVED (score={} >= minScore={})", loanApplicationId, totalScore, minCreditScore);
        } else {
            assessmentStatus = AssessmentStatus.REJECTED;
            loanStatus = LoanStatus.REJECTED;
            log.info("[AssessmentService] Loan {} rejected by credit score (score={} < minScore={})", loanApplicationId, totalScore, minCreditScore);
        }

        // 1. Save or Update ApplicationAssessment
        ApplicationAssessment aa = applicationAssessmentRepository
                .findByLoanApplicationId(loanApplication.getLoanApplicationId())
                .orElse(ApplicationAssessment.builder()
                        .loanApplicationId(loanApplication.getLoanApplicationId())
                        .build());

        aa.setDti(BigDecimal.valueOf(dti));
        aa.setIncomeScore(incomeScore);
        aa.setOccupationScore(occupationScore);
        aa.setPurposeScore(purposeScore);
        aa.setLoanSuitabilityScore(loanSuitabilityScore);
        aa.setTotalScore(totalScore);
        aa.setStatus(assessmentStatus);
        aa = applicationAssessmentRepository.save(aa);

        // 2. Update LoanApplication Status & Decision Date
        loanApplication.setStatus(loanStatus);
        loanApplication.setDecisionDate(LocalDateTime.now());
        loanApplicationRepository.save(loanApplication);

        return aa;
    }

    private Policy findApplicablePolicy(double amount) {
        BigDecimal loanAmount = BigDecimal.valueOf(amount);
        return policyRepository
                .findFirstByActiveAndMaxLoanAmountGreaterThanEqualOrderByMaxLoanAmountAsc("active", loanAmount)
                .orElseGet(() -> {
                    List<Policy> activePolicies = policyRepository.findByActiveOrderByMaxLoanAmountAsc("active");
                    if (!activePolicies.isEmpty()) {
                        return activePolicies.get(activePolicies.size() - 1);
                    }
                    Policy fallback = new Policy();
                    fallback.setPolicyName("Chính sách mặc định");
                    fallback.setMinCreditScore(50);
                    fallback.setDtiThreshold(BigDecimal.valueOf(60.0));
                    return fallback;
                });
    }

    private int calculateAge(LocalDate dob) {
        if (dob == null) {
            return 28;
        }
        return java.time.Period.between(dob, LocalDate.now()).getYears();
    }

    private double calculateDTI(LoanApplication loanApplication) {
        long totalDebt = loanApplication.getAmount() != null ? loanApplication.getAmount().longValue() : 0L;
        Integer termObj = loanApplication.getTerm();
        int term = (termObj != null && termObj > 0) ? termObj : 12;

        com.example.los.entity.enums.IncomeRange incomeRange = loanApplication.getIncomeRange();
        long monthlyIncome = (incomeRange != null && incomeRange.getEstimatedMonthlyIncome() != null)
                ? incomeRange.getEstimatedMonthlyIncome()
                : 15_000_000L;

        if (monthlyIncome <= 0) {
            monthlyIncome = 15_000_000L;
        }

        long monthlyPayment = totalDebt / term;
        double dti = ((double) monthlyPayment / monthlyIncome) * 100.0;
        return Math.round(dti * 100.0) / 100.0;
    }
}


