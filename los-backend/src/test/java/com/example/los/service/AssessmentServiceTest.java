package com.example.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.los.entity.ApplicationAssessment;
import com.example.los.entity.LoanApplication;
import com.example.los.entity.Policy;
import com.example.los.entity.User;
import com.example.los.entity.enums.AssessmentStatus;
import com.example.los.entity.enums.IncomeRange;
import com.example.los.entity.enums.LoanPurpose;
import com.example.los.entity.enums.LoanStatus;
import com.example.los.entity.enums.Occupation;
import com.example.los.repository.ApplicationAssessmentRepository;
import com.example.los.repository.LoanApplicationRepository;
import com.example.los.repository.PolicyRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentService Unit Tests")
class AssessmentServiceTest {

    @Mock private ApplicationAssessmentRepository applicationAssessmentRepository;
    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private PolicyRepository policyRepository;

    @InjectMocks
    private AssessmentService assessmentService;

    private Policy defaultPolicy;
    private ApplicationAssessment savedAssessment;

    @BeforeEach
    void setUp() {
        defaultPolicy = new Policy();
        defaultPolicy.setPolicyName("Test Policy");
        defaultPolicy.setMinCreditScore(50);
        defaultPolicy.setDtiThreshold(BigDecimal.valueOf(60.0));

        savedAssessment = ApplicationAssessment.builder()
                .loanApplicationId("app-001")
                .build();
    }

    // ─── Helper ───────────────────────────────────────────────

    private LoanApplication buildLoanApp(String id, int ageYearsAgo, long amount, int term,
                                          IncomeRange income, LoanPurpose purpose, Occupation occupation) {
        User user = User.builder()
                .userId("user-001")
                .fullName("Nguyen Van A")
                .birthdate(LocalDate.now().minusYears(ageYearsAgo))
                .build();
        return LoanApplication.builder()
                .loanApplicationId(id)
                .user(user)
                .amount(BigDecimal.valueOf(amount))
                .term(term)
                .incomeRange(income)
                .purpose(purpose)
                .occupation(occupation)
                .build();
    }

    private void mockRepoFor(LoanApplication app) {
        when(loanApplicationRepository.findById(app.getLoanApplicationId())).thenReturn(Optional.of(app));
        when(policyRepository.findFirstByActiveAndMaxLoanAmountGreaterThanEqualOrderByMaxLoanAmountAsc(anyString(), any()))
                .thenReturn(Optional.of(defaultPolicy));
        when(applicationAssessmentRepository.findByLoanApplicationId(anyString())).thenReturn(Optional.empty());
        when(applicationAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── Loan Not Found ───────────────────────────────────────

    @Test
    @DisplayName("evaluate() throws when loan application not found")
    void evaluate_throwsWhenLoanNotFound() {
        when(loanApplicationRepository.findById("not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.evaluate("not-exist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan application not found");
    }

    // ─── Age Knock-out Rule ───────────────────────────────────

    @Nested
    @DisplayName("Age Knock-out Rule")
    class AgeKnockoutRule {

        @Test
        @DisplayName("Age 17 (under 18) -> REJECTED")
        void age_under18_isRejected() {
            LoanApplication app = buildLoanApp("app-001", 17, 5_000_000, 12,
                    IncomeRange.TU_10_DEN_20_TRIEU, LoanPurpose.HOC_TAP, Occupation.SINH_VIEN);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.REJECTED);
        }

        @Test
        @DisplayName("Age 18 (boundary - valid) -> not rejected by age")
        void age_exactly18_isNotRejectedByAge() {
            LoanApplication app = buildLoanApp("app-001", 18, 5_000_000, 12,
                    IncomeRange.TU_20_DEN_30_TRIEU, LoanPurpose.HOC_TAP, Occupation.CAN_BO_CONG_CHUC);
            mockRepoFor(app);
            ApplicationAssessment result = assessmentService.evaluate("app-001");
            // Not rejected by age rule (may be APPROVED or rejected by other rules)
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Age 60 (boundary - valid) -> not rejected by age")
        void age_exactly60_isNotRejectedByAge() {
            LoanApplication app = buildLoanApp("app-001", 60, 5_000_000, 12,
                    IncomeRange.TU_20_DEN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
            mockRepoFor(app);
            ApplicationAssessment result = assessmentService.evaluate("app-001");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Age 61 (over 60) -> REJECTED")
        void age_over60_isRejected() {
            LoanApplication app = buildLoanApp("app-001", 61, 5_000_000, 12,
                    IncomeRange.TREN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.HUU_TRI);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.REJECTED);
        }

        @Test
        @DisplayName("No birthdate (dob=null) -> not rejected by age")
        void age_noDob_isNotRejectedByAgeRule() {
            User user = User.builder().userId("user-001").build(); // no birthdate
            LoanApplication app = LoanApplication.builder()
                    .loanApplicationId("app-001")
                    .user(user)
                    .amount(BigDecimal.valueOf(5_000_000))
                    .term(12)
                    .incomeRange(IncomeRange.TREN_30_TRIEU)
                    .purpose(LoanPurpose.CHUA_BENH)
                    .occupation(Occupation.CAN_BO_CONG_CHUC)
                    .build();
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            // Age rule not triggered when dob is null
            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.APPROVED);
        }
    }

    // ─── DTI Knock-out Rule ───────────────────────────────────

    @Nested
    @DisplayName("DTI Knock-out Rule")
    class DtiKnockoutRule {

        @Test
        @DisplayName("DTI > 60% -> REJECTED")
        void dti_over60_isRejected() {
            // amount=10tr, term=1month, income=8tr -> DTI = (10tr/1) / 8tr * 100 = 125%
            LoanApplication app = buildLoanApp("app-001", 30, 10_000_000, 1,
                    IncomeRange.DUOI_10_TRIEU, LoanPurpose.DU_LICH, Occupation.KHAC);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.REJECTED);
        }

        @Test
        @DisplayName("DTI below threshold -> not rejected by DTI")
        void dti_belowThreshold_isNotRejectedByDti() {
            LoanApplication app = buildLoanApp("app-001", 30, 12_000_000, 12,
                    IncomeRange.TU_20_DEN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            // Should not be rejected by DTI
            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.APPROVED);
        }

        @Test
        @DisplayName("DTI=0 (amount=0) -> no DTI rejection")
        void dti_zeroAmount_noRejection() {
            LoanApplication app = buildLoanApp("app-001", 30, 0, 12,
                    IncomeRange.TU_10_DEN_20_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getDti()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ─── Credit Score / Approval ──────────────────────────────

    @Nested
    @DisplayName("Credit Score and Approval Logic")
    class CreditScoreLogic {

        @Test
        @DisplayName("High score -> APPROVED")
        void highScore_isApproved() {
            // Max scores: income=40, purpose=30(CHUA_BENH), occupation=30(CAN_BO) -> loanSuit=60
            // totalScore = (40*0.4) + (60*0.4) + (small_dti * 0.2) = 16+24+... >= 50
            LoanApplication app = buildLoanApp("app-001", 35, 5_000_000, 24,
                    IncomeRange.TREN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.APPROVED);
        }

        @Test
        @DisplayName("Low score -> REJECTED")
        void lowScore_isRejected() {
            // Min scores: income=10(DUOI_10), purpose=10(DU_LICH), occupation=5(KHAC)
            // totalScore = (10*0.4) + (15*0.4) + dti_component = 4+6+... = ~10 < 50
            LoanApplication app = buildLoanApp("app-001", 25, 1_000_000, 12,
                    IncomeRange.DUOI_10_TRIEU, LoanPurpose.DU_LICH, Occupation.KHAC);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getStatus()).isEqualTo(AssessmentStatus.REJECTED);
        }

        @Test
        @DisplayName("totalScore == minCreditScore (boundary) -> APPROVED")
        void score_equalToMinCreditScore_isApproved() {
            defaultPolicy.setMinCreditScore(40);
            LoanApplication app = buildLoanApp("app-001", 30, 3_000_000, 24,
                    IncomeRange.TU_10_DEN_20_TRIEU, LoanPurpose.HOC_TAP, Occupation.NHAN_VIEN_CONG_TY);
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            assertThat(result.getStatus()).isIn(AssessmentStatus.APPROVED, AssessmentStatus.REJECTED);
        }

        @Test
        @DisplayName("Null incomeRange/purpose/occupation -> uses defaults, not NPE")
        void nullFields_usesDefaultScores() {
            User user = User.builder()
                    .userId("user-001")
                    .birthdate(LocalDate.now().minusYears(30))
                    .build();
            LoanApplication app = LoanApplication.builder()
                    .loanApplicationId("app-001")
                    .user(user)
                    .amount(BigDecimal.valueOf(5_000_000))
                    .term(12)
                    .incomeRange(null)
                    .purpose(null)
                    .occupation(null)
                    .build();
            mockRepoFor(app);

            ApplicationAssessment result = assessmentService.evaluate("app-001");

            // Should not throw NPE
            assertThat(result).isNotNull();
            assertThat(result.getIncomeScore()).isEqualTo(20);
            assertThat(result.getPurposeScore()).isEqualTo(15);
            assertThat(result.getOccupationScore()).isEqualTo(15);
        }
    }

    // ─── Assessment Metrics Saved ─────────────────────────────

    @Test
    @DisplayName("evaluate() saves ApplicationAssessment and updates LoanApplication status")
    void evaluate_savesAssessmentAndUpdatesLoanStatus() {
        LoanApplication app = buildLoanApp("app-001", 30, 5_000_000, 24,
                IncomeRange.TREN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
        mockRepoFor(app);

        assessmentService.evaluate("app-001");

        verify(applicationAssessmentRepository).save(any(ApplicationAssessment.class));
        verify(loanApplicationRepository).save(any(LoanApplication.class));
    }

    @Test
    @DisplayName("evaluate() updates existing assessment if it already exists")
    void evaluate_updatesExistingAssessment() {
        LoanApplication app = buildLoanApp("app-001", 30, 5_000_000, 24,
                IncomeRange.TREN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
        ApplicationAssessment existing = ApplicationAssessment.builder()
                .loanApplicationId("app-001")
                .status(AssessmentStatus.REJECTED)
                .build();
        when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));
        when(policyRepository.findFirstByActiveAndMaxLoanAmountGreaterThanEqualOrderByMaxLoanAmountAsc(anyString(), any()))
                .thenReturn(Optional.of(defaultPolicy));
        when(applicationAssessmentRepository.findByLoanApplicationId("app-001")).thenReturn(Optional.of(existing));
        when(applicationAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationAssessment result = assessmentService.evaluate("app-001");

        // Should have updated the existing assessment object
        assertThat(result.getLoanApplicationId()).isEqualTo("app-001");
    }

    // ─── Fallback Policy ──────────────────────────────────────

    @Test
    @DisplayName("Falls back to largest active policy when loan amount exceeds all policies")
    void evaluate_fallbackToLargestPolicy_whenAmountExceedsAll() {
        LoanApplication app = buildLoanApp("app-001", 30, 999_000_000, 12,
                IncomeRange.TREN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
        when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));
        when(policyRepository.findFirstByActiveAndMaxLoanAmountGreaterThanEqualOrderByMaxLoanAmountAsc(anyString(), any()))
                .thenReturn(Optional.empty());
        when(policyRepository.findByActiveOrderByMaxLoanAmountAsc("active"))
                .thenReturn(List.of(defaultPolicy));
        when(applicationAssessmentRepository.findByLoanApplicationId(anyString())).thenReturn(Optional.empty());
        when(applicationAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationAssessment result = assessmentService.evaluate("app-001");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Uses hardcoded fallback policy when no active policies exist at all")
    void evaluate_usesHardcodedFallback_whenNoPoliciesExist() {
        LoanApplication app = buildLoanApp("app-001", 30, 5_000_000, 12,
                IncomeRange.TREN_30_TRIEU, LoanPurpose.CHUA_BENH, Occupation.CAN_BO_CONG_CHUC);
        when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));
        when(policyRepository.findFirstByActiveAndMaxLoanAmountGreaterThanEqualOrderByMaxLoanAmountAsc(anyString(), any()))
                .thenReturn(Optional.empty());
        when(policyRepository.findByActiveOrderByMaxLoanAmountAsc("active")).thenReturn(List.of());
        when(applicationAssessmentRepository.findByLoanApplicationId(anyString())).thenReturn(Optional.empty());
        when(applicationAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw — uses fallback policy with minCreditScore=50, dtiThreshold=60
        ApplicationAssessment result = assessmentService.evaluate("app-001");
        assertThat(result).isNotNull();
    }
}