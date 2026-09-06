package com.example.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.example.los.entity.LoanApplication;
import com.example.los.entity.OtpRecord;
import com.example.los.entity.User;
import com.example.los.entity.enums.OtpStatus;
import com.example.los.repository.LoanApplicationRepository;
import com.example.los.repository.OtpRepository;
import com.example.los.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Unit Tests")
class OtpServiceTest {

    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private OtpRepository otpRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    private User sampleUser;
    private LoanApplication sampleLoan;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId("user-001")
                .fullName("Nguyen Van A")
                .email("test@example.com")
                .build();

        sampleLoan = LoanApplication.builder()
                .loanApplicationId("loan-001")
                .user(sampleUser)
                .build();
    }

    // ─── sendOTP() ────────────────────────────────────────────

    @Nested
    @DisplayName("sendOTP()")
    class SendOtpTests {

        @Test
        @DisplayName("Expires previous pending OTPs, saves new OTP, and sends email")
        void sendOTP_success() {
            when(loanApplicationRepository.findByRecipientEmail("test@example.com"))
                    .thenReturn(List.of(sampleLoan));
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(sampleUser));

            otpService.sendOTP("test@example.com");

            verify(otpRepository).expireAllPendingForEmail("test@example.com");

            ArgumentCaptor<OtpRecord> recordCaptor = ArgumentCaptor.forClass(OtpRecord.class);
            verify(otpRepository).save(recordCaptor.capture());
            OtpRecord savedRecord = recordCaptor.getValue();

            assertThat(savedRecord.getRecipientEmail()).isEqualTo("test@example.com");
            assertThat(savedRecord.getOtpCode()).matches("\\d{6}");
            assertThat(savedRecord.getStatus()).isEqualTo(OtpStatus.PENDING);
            assertThat(savedRecord.getSentAt()).isNotNull();
            assertThat(savedRecord.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(savedRecord.getUser()).isEqualTo(sampleUser);

            verify(emailService).sendEmail(eq("test@example.com"), eq(savedRecord.getOtpCode()), eq("Nguyen Van A"));
        }

        @Test
        @DisplayName("Multiple loan applications for same email picks the first one without NonUniqueResultException")
        void sendOTP_multipleLoansFound_usesFirstWithoutException() {
            LoanApplication secondLoan = LoanApplication.builder()
                    .loanApplicationId("loan-002")
                    .user(sampleUser)
                    .build();

            when(loanApplicationRepository.findByRecipientEmail("test@example.com"))
                    .thenReturn(List.of(sampleLoan, secondLoan));
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(sampleUser));

            otpService.sendOTP("test@example.com");

            verify(emailService).sendEmail(eq("test@example.com"), any(), eq("Nguyen Van A"));
        }

        @Test
        @DisplayName("sendOTP works when user or loan application not found (recipient has no prior loan)")
        void sendOTP_noPriorLoanOrUser_stillGeneratesAndSends() {
            when(loanApplicationRepository.findByRecipientEmail("unknown@example.com"))
                    .thenReturn(List.of());
            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            otpService.sendOTP("unknown@example.com");

            ArgumentCaptor<OtpRecord> captor = ArgumentCaptor.forClass(OtpRecord.class);
            verify(otpRepository).save(captor.capture());
            OtpRecord saved = captor.getValue();

            assertThat(saved.getOtpCode()).hasSize(6);
            assertThat(saved.getUser()).isNull();
            verify(emailService).sendEmail(eq("unknown@example.com"), eq(saved.getOtpCode()), eq(null));
        }
    }

    // ─── verifyOTP() ──────────────────────────────────────────

    @Nested
    @DisplayName("verifyOTP()")
    class VerifyOtpTests {

        @Test
        @DisplayName("Valid and unexpired OTP verifies successfully and updates status")
        void verifyOTP_valid_success() {
            OtpRecord pending = OtpRecord.builder()
                    .otpId("otp-001")
                    .recipientEmail("test@example.com")
                    .otpCode("123456")
                    .status(OtpStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            when(otpRepository.findByRecipientEmailAndStatus("test@example.com", OtpStatus.PENDING))
                    .thenReturn(List.of(pending));

            otpService.verifyOTP("test@example.com", "123456");

            assertThat(pending.getStatus()).isEqualTo(OtpStatus.VERIFIED);
            assertThat(pending.getVerifiedAt()).isNotNull();
            verify(otpRepository).save(pending);
        }

        @Test
        @DisplayName("Throws exception when no pending OTP is found")
        void verifyOTP_noPendingOtp_throws() {
            when(otpRepository.findByRecipientEmailAndStatus("test@example.com", OtpStatus.PENDING))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> otpService.verifyOTP("test@example.com", "123456"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Không tìm thấy OTP");
        }

        @Test
        @DisplayName("Expired OTP updates status to EXPIRED and throws exception")
        void verifyOTP_expired_marksExpiredAndThrows() {
            OtpRecord expiredRecord = OtpRecord.builder()
                    .otpId("otp-002")
                    .recipientEmail("test@example.com")
                    .otpCode("123456")
                    .status(OtpStatus.PENDING)
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired 1 min ago
                    .build();

            when(otpRepository.findByRecipientEmailAndStatus("test@example.com", OtpStatus.PENDING))
                    .thenReturn(List.of(expiredRecord));

            assertThatThrownBy(() -> otpService.verifyOTP("test@example.com", "123456"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("OTP đã hết hạn");

            assertThat(expiredRecord.getStatus()).isEqualTo(OtpStatus.EXPIRED);
            verify(otpRepository).save(expiredRecord);
        }

        @Test
        @DisplayName("Incorrect OTP code throws exception without marking as verified")
        void verifyOTP_wrongCode_throws() {
            OtpRecord pending = OtpRecord.builder()
                    .otpId("otp-003")
                    .recipientEmail("test@example.com")
                    .otpCode("654321")
                    .status(OtpStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            when(otpRepository.findByRecipientEmailAndStatus("test@example.com", OtpStatus.PENDING))
                    .thenReturn(List.of(pending));

            assertThatThrownBy(() -> otpService.verifyOTP("test@example.com", "000000"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mã OTP không chính xác");

            assertThat(pending.getStatus()).isEqualTo(OtpStatus.PENDING);
        }
    }
}
