package com.example.los.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.los.dto.loan.LoanApplicationRequest;
import com.example.los.dto.loan.LoanApplicationResponse;
import com.example.los.entity.LoanApplication;
import com.example.los.entity.LoanProduct;
import com.example.los.entity.User;
import com.example.los.entity.enums.IncomeRange;
import com.example.los.entity.enums.LoanPurpose;
import com.example.los.entity.enums.LoanStatus;
import com.example.los.entity.enums.Occupation;
import com.example.los.entity.enums.UserStatus;
import com.example.los.repository.LoanApplicationRepository;
import com.example.los.repository.LoanProductRepository;
import com.example.los.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final UserRepository userRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final AssessmentService assessmentService;

    @Transactional
    public LoanApplicationResponse submit(LoanApplicationRequest req) {

        User applicant = userRepository.findByCccd(req.getCccd())
                .orElse(User.builder().build());

        applicant.setFullName(req.getFullName());
        applicant.setCccd(req.getCccd());
        applicant.setEmail(req.getEmail());
        applicant.setPhoneNumber(req.getPhoneNumber());
        applicant.setAddress(req.getAddress());
        applicant.setStatus(UserStatus.ACTIVE);
        if (req.getBirthDate() != null && !req.getBirthDate().isBlank()) {
            try {
                String bd = req.getBirthDate().trim();
                if (bd.contains("/")) {
                    String[] parts = bd.split("/");
                    if (parts.length == 3) {
                        applicant.setBirthdate(LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0])));
                    }
                } else {
                    applicant.setBirthdate(LocalDate.parse(bd));
                }
            } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                log.warn("[LoanService] Could not parse birthdate: {}", req.getBirthDate());
            }
        }
        userRepository.save(applicant);

        BigDecimal amount = BigDecimal.valueOf(req.getAmount());
        final BigDecimal initialRate = BigDecimal.valueOf(getInterestRate(amount, req.getTerm()));

        LoanProduct product = loanProductRepository
                .findByAmountAndTerm(amount, req.getTerm())
                .stream()
                .findFirst()
                .orElseGet(() -> loanProductRepository.save(
                        LoanProduct.builder()
                                .name("Loan-" + amount.longValue())
                                .amount(amount)
                                .term(req.getTerm())
                                .interestRate(initialRate)
                                .status("active")
                                .build()));

        BigDecimal rate = (product.getInterestRate() != null) ? product.getInterestRate() : initialRate;

        LoanApplication loanApp = LoanApplication.builder()
                .user(applicant)
                .loanProduct(product)
                .amount(amount)
                .term(req.getTerm())
                .approveInterestRate(rate)
                .purpose(LoanPurpose.fromLabel(req.getPurpose()))
                .incomeRange(IncomeRange.fromLabel(req.getIncomeRange()))
                .occupation(Occupation.fromLabel(req.getOccupation()))
                .referenceContactName1(req.getRef1Name())
                .referenceContactPhone1(req.getRef1Phone())
                .referenceContactName2(req.getRef2Name())
                .referenceContactPhone2(req.getRef2Phone())
                .status(LoanStatus.SUBMITTED)
                .build();

        loanApplicationRepository.save(loanApp);
        try {
            assessmentService.evaluate(loanApp.getLoanApplicationId());
            loanApp = loanApplicationRepository.findById(loanApp.getLoanApplicationId()).orElse(loanApp);
        } catch (Exception e) {
            log.warn("[LoanService] Assessment failed for {}: {}", loanApp.getLoanApplicationId(), e.getMessage());
        }

        return LoanApplicationResponse.from(loanApp);
    }

    public List<LoanApplicationResponse> getHistory(String cccd, String fromDate, String toDate) {
        if (fromDate != null && !fromDate.isBlank() && toDate != null && !toDate.isBlank()) {
            try {
                LocalDateTime start = LocalDate.parse(fromDate.trim()).atStartOfDay();
                LocalDateTime end = LocalDate.parse(toDate.trim()).atTime(LocalTime.MAX);
                return loanApplicationRepository
                        .findByUserCccdAndCreatedAtBetweenOrderByCreatedAtDesc(cccd, start, end)
                        .stream()
                        .map(LoanApplicationResponse::from)
                        .toList();
            } catch (Exception e) {
                log.warn("[LoanService] Invalid date format for history filter: fromDate={}, toDate={}", fromDate, toDate);
            }
        }
        return loanApplicationRepository
                .findByUserCccdOrderByCreatedAtDesc(cccd)
                .stream()
                .map(LoanApplicationResponse::from)
                .toList();
    }

    private double getInterestRate(BigDecimal amount, Integer term) {
        Map<String, Double> rates = new HashMap<>();
        rates.put("6000000_3", 4.08);
        rates.put("6000000_4", 3.87);
        rates.put("6000000_5", 3.73);
        rates.put("6000000_6", 3.67);
        rates.put("7000000_3", 4.08);
        rates.put("7000000_4", 3.87);
        rates.put("7000000_5", 3.73);
        rates.put("7000000_6", 3.68);
        rates.put("8000000_3", 4.08);
        rates.put("8000000_4", 3.86);
        rates.put("8000000_5", 3.74);
        rates.put("8000000_6", 3.67);
        rates.put("8000000_9", 3.6);
        rates.put("9000000_3", 4.08);
        rates.put("9000000_4", 3.86);
        rates.put("9000000_5", 3.74);
        rates.put("9000000_6", 3.68);
        rates.put("9000000_9", 3.6);
        rates.put("10000000_6", 2.78);
        rates.put("10000000_9", 2.7);
        rates.put("12000000_3", 4.08);
        rates.put("12000000_4", 3.86);
        rates.put("12000000_5", 3.74);
        rates.put("12000000_6", 3.67);
        rates.put("12000000_9", 3.6);
        rates.put("12000000_12", 3.6);
        rates.put("12000000_15", 3.63);
        rates.put("15000000_9", 2.7);
        rates.put("15000000_12", 2.69);
        rates.put("15000000_15", 2.7);
        rates.put("20000000_12", 2.69);
        rates.put("20000000_15", 2.7);
        rates.put("20000000_18", 2.72);
        rates.put("50000000_18", 2.72);
        rates.put("50000000_21", 2.76);
        rates.put("50000000_24", 2.79);
        rates.put("50000000_30", 2.87);
        rates.put("50000000_36", 2.95);
        rates.put("50000000_42", 3.03);
        rates.put("50000000_48", 3.1);
        rates.put("60000000_15", 2.7);
        rates.put("60000000_18", 2.72);
        rates.put("60000000_21", 2.75);
        rates.put("60000000_24", 2.79);
        rates.put("60000000_27", 2.83);
        rates.put("60000000_30", 2.87);
        rates.put("60000000_36", 2.95);
        rates.put("70000000_18", 2.72);
        rates.put("70000000_21", 2.75);
        rates.put("70000000_24", 2.79);
        rates.put("70000000_27", 2.83);
        rates.put("70000000_30", 2.87);
        rates.put("70000000_36", 2.95);
        rates.put("80000000_21", 2.2);
        rates.put("80000000_24", 2.23);
        rates.put("80000000_27", 2.25);
        rates.put("80000000_30", 2.28);
        rates.put("80000000_36", 2.33);
        rates.put("90000000_24", 2.23);
        rates.put("90000000_27", 2.25);
        rates.put("90000000_30", 2.28);
        rates.put("90000000_36", 2.23);
        rates.put("100000000_30", 2.28);
        rates.put("100000000_36", 2.33);
        rates.put("100000000_42", 2.39);
        rates.put("100000000_48", 2.44);

        String key = amount.longValue() + "_" + term;
        return rates.getOrDefault(key, 5.0); // fallback 5% nếu không có trong matrix
    }
}
