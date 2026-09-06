package com.example.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.los.dto.loan.LoanApplicationRequest;
import com.example.los.dto.loan.LoanApplicationResponse;
import com.example.los.entity.ApplicationAssessment;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LoanProductRepository loanProductRepository;
    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private AssessmentService assessmentService;

    @InjectMocks
    private LoanService loanService;

    private LoanApplicationRequest sampleRequest;
    private LoanProduct sampleProduct;

    @BeforeEach
    void setUp() {
        sampleRequest = new LoanApplicationRequest();
        sampleRequest.setCccd("012345678901");
        sampleRequest.setFullName("Nguyen Van A");
        sampleRequest.setEmail("vana@example.com");
        sampleRequest.setPhoneNumber("0987654321");
        sampleRequest.setAddress("123 Duong ABC, Quan 1, TP.HCM");
        sampleRequest.setBirthDate("15/06/1990");
        sampleRequest.setAmount(10_000_000.0);
        sampleRequest.setTerm(6);
        sampleRequest.setPurpose("Tiêu dùng cá nhân");
        sampleRequest.setIncomeRange("10 - 20 triệu");
        sampleRequest.setOccupation("Nhân viên văn phòng");
        sampleRequest.setRef1Name("Nguyen Van B");
        sampleRequest.setRef1Phone("0912345678");
        sampleRequest.setRef2Name("Tran Thi C");
        sampleRequest.setRef2Phone("0923456789");

        sampleProduct = LoanProduct.builder()
                .loanProductId("prod-001")
                .name("Loan-10000000")
                .amount(BigDecimal.valueOf(10_000_000L))
                .term(6)
                .interestRate(BigDecimal.valueOf(2.78))
                .status("active")
                .build();
    }

    // ─── submit() ─────────────────────────────────────────────

    @Nested
    @DisplayName("submit()")
    class SubmitTests {

        @Test
        @DisplayName("Creates new user when CCCD not found and successfully submits loan")
        void submit_newUser_success() {
            when(userRepository.findByCccd("012345678901")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            when(loanProductRepository.findByAmountAndTerm(any(BigDecimal.class), eq(6)))
                    .thenReturn(List.of(sampleProduct));

            when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-001");
                return app;
            });

            when(loanApplicationRepository.findById("app-001")).thenAnswer(inv -> {
                LoanApplication app = LoanApplication.builder()
                        .loanApplicationId("app-001")
                        .amount(BigDecimal.valueOf(10_000_000L))
                        .term(6)
                        .status(LoanStatus.APPROVED)
                        .build();
                return Optional.of(app);
            });

            LoanApplicationResponse response = loanService.submit(sampleRequest);

            assertThat(response).isNotNull();
            assertThat(response.getLoanApplicationId()).isEqualTo("app-001");
            assertThat(response.getStatus()).isEqualTo(LoanStatus.APPROVED.name());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getCccd()).isEqualTo("012345678901");
            assertThat(savedUser.getFullName()).isEqualTo("Nguyen Van A");
            assertThat(savedUser.getBirthdate()).isEqualTo(LocalDate.of(1990, 6, 15));
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(assessmentService).evaluate("app-001");
        }

        @Test
        @DisplayName("Updates existing user when CCCD exists")
        void submit_existingUser_updatesProfile() {
            User existingUser = User.builder()
                    .userId("user-existing")
                    .cccd("012345678901")
                    .fullName("Old Name")
                    .status(UserStatus.INACTIVE)
                    .build();

            when(userRepository.findByCccd("012345678901")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanProductRepository.findByAmountAndTerm(any(), eq(6))).thenReturn(List.of(sampleProduct));
            when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-002");
                return app;
            });
            when(loanApplicationRepository.findById("app-002")).thenReturn(Optional.empty());

            loanService.submit(sampleRequest);

            assertThat(existingUser.getFullName()).isEqualTo("Nguyen Van A");
            assertThat(existingUser.getEmail()).isEqualTo("vana@example.com");
            assertThat(existingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("Parses birthdate with YYYY-MM-DD ISO format")
        void submit_isoBirthDateFormat_parsesCorrectly() {
            sampleRequest.setBirthDate("1995-12-25");
            when(userRepository.findByCccd(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanProductRepository.findByAmountAndTerm(any(), eq(6))).thenReturn(List.of(sampleProduct));
            when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-003");
                return app;
            });

            loanService.submit(sampleRequest);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getBirthdate()).isEqualTo(LocalDate.of(1995, 12, 25));
        }

        @Test
        @DisplayName("Invalid birthdate string does not throw exception, leaves birthdate as null")
        void submit_invalidBirthDate_doesNotThrow() {
            sampleRequest.setBirthDate("invalid-date-format");
            when(userRepository.findByCccd(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanProductRepository.findByAmountAndTerm(any(), eq(6))).thenReturn(List.of(sampleProduct));
            when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-004");
                return app;
            });

            LoanApplicationResponse response = loanService.submit(sampleRequest);

            assertThat(response).isNotNull();
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getBirthdate()).isNull();
        }

        @Test
        @DisplayName("Creates new LoanProduct when not found in database using interest rate matrix")
        void submit_productNotFound_createsNewProductFromMatrix() {
            when(userRepository.findByCccd(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Product not found in DB -> save is called with calculated rate
            when(loanProductRepository.findByAmountAndTerm(any(BigDecimal.class), eq(6)))
                    .thenReturn(List.of());
            when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> inv.getArgument(0));

            when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-005");
                return app;
            });

            loanService.submit(sampleRequest);

            ArgumentCaptor<LoanProduct> prodCaptor = ArgumentCaptor.forClass(LoanProduct.class);
            verify(loanProductRepository).save(prodCaptor.capture());
            LoanProduct createdProduct = prodCaptor.getValue();
            // Matrix: 10000000_6 -> 2.78
            assertThat(createdProduct.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(2.78));
            assertThat(createdProduct.getTerm()).isEqualTo(6);
        }

        @Test
        @DisplayName("Falls back to 5.0% interest rate when amount and term pair not in matrix")
        void submit_amountTermNotInMatrix_usesFallbackRate5Percent() {
            sampleRequest.setAmount(123_456_789.0);
            sampleRequest.setTerm(99);

            when(userRepository.findByCccd(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanProductRepository.findByAmountAndTerm(any(BigDecimal.class), eq(99)))
                    .thenReturn(List.of());
            when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-006");
                return app;
            });

            loanService.submit(sampleRequest);

            ArgumentCaptor<LoanProduct> prodCaptor = ArgumentCaptor.forClass(LoanProduct.class);
            verify(loanProductRepository).save(prodCaptor.capture());
            assertThat(prodCaptor.getValue().getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
        }

        @Test
        @DisplayName("Assessment evaluation failure does not fail loan submission")
        void submit_assessmentFailure_handledGracefully() {
            when(userRepository.findByCccd(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanProductRepository.findByAmountAndTerm(any(), eq(6))).thenReturn(List.of(sampleProduct));
            when(loanApplicationRepository.save(any())).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setLoanApplicationId("app-007");
                return app;
            });

            // Assessment throws RuntimeException
            doThrow(new RuntimeException("Assessment engine unreachable"))
                    .when(assessmentService).evaluate("app-007");

            LoanApplicationResponse response = loanService.submit(sampleRequest);

            // Loan is still submitted and returned with SUBMITTED status
            assertThat(response).isNotNull();
            assertThat(response.getLoanApplicationId()).isEqualTo("app-007");
            assertThat(response.getStatus()).isEqualTo(LoanStatus.SUBMITTED.name());
        }
    }

    // ─── getHistory() ─────────────────────────────────────────

    @Nested
    @DisplayName("getHistory()")
    class GetHistoryTests {

        @Test
        @DisplayName("Returns all history when dates are null")
        void getHistory_nullDates_returnsAll() {
            LoanApplication app = LoanApplication.builder()
                    .loanApplicationId("app-010")
                    .amount(BigDecimal.valueOf(5_000_000))
                    .status(LoanStatus.APPROVED)
                    .build();

            when(loanApplicationRepository.findByUserCccdOrderByCreatedAtDesc("012345678901"))
                    .thenReturn(List.of(app));

            List<LoanApplicationResponse> result = loanService.getHistory("012345678901", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLoanApplicationId()).isEqualTo("app-010");
        }

        @Test
        @DisplayName("Returns filtered history when valid fromDate and toDate provided")
        void getHistory_validDates_filtersByRange() {
            LoanApplication app = LoanApplication.builder()
                    .loanApplicationId("app-011")
                    .amount(BigDecimal.valueOf(8_000_000))
                    .status(LoanStatus.APPROVED)
                    .build();

            when(loanApplicationRepository.findByUserCccdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("012345678901"), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(app));

            List<LoanApplicationResponse> result = loanService.getHistory("012345678901", "2026-01-01", "2026-06-30");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLoanApplicationId()).isEqualTo("app-011");
        }

        @Test
        @DisplayName("Falls back to all history when date format is invalid")
        void getHistory_invalidDateFormat_fallsBackToAll() {
            LoanApplication app = LoanApplication.builder()
                    .loanApplicationId("app-012")
                    .amount(BigDecimal.valueOf(6_000_000))
                    .status(LoanStatus.REJECTED)
                    .build();

            when(loanApplicationRepository.findByUserCccdOrderByCreatedAtDesc("012345678901"))
                    .thenReturn(List.of(app));

            List<LoanApplicationResponse> result = loanService.getHistory("012345678901", "invalid-from", "invalid-to");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLoanApplicationId()).isEqualTo("app-012");
        }

        @Test
        @DisplayName("Returns empty list when user has no loan applications")
        void getHistory_noHistory_returnsEmptyList() {
            when(loanApplicationRepository.findByUserCccdOrderByCreatedAtDesc("empty-cccd"))
                    .thenReturn(List.of());

            List<LoanApplicationResponse> result = loanService.getHistory("empty-cccd", "", "");

            assertThat(result).isEmpty();
        }
    }
}
