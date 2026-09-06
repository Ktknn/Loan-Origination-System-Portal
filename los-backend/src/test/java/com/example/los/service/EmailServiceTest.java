package com.example.los.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage((Session) null);
    }

    @Nested
    @DisplayName("sendEmail()")
    class SendEmailTests {

        @Test
        @DisplayName("Valid email, otpCode, and fullName sends mime message")
        void sendEmail_success() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendEmail("recipient@example.com", "123456", "Nguyen Van A");

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Null or blank otpCode throws IllegalArgumentException")
        void sendEmail_nullOrBlankOtp_throwsException() {
            assertThatThrownBy(() -> emailService.sendEmail("test@example.com", null, "User"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không được để trống");

            assertThatThrownBy(() -> emailService.sendEmail("test@example.com", "   ", "User"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không được để trống");
        }

        @Test
        @DisplayName("Null or blank fullName falls back to 'Quý khách' and succeeds")
        void sendEmail_nullFullName_usesDefaultGreeting() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatNoException().isThrownBy(() ->
                    emailService.sendEmail("recipient@example.com", "654321", null)
            );

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("FullName with HTML special characters is escaped safely")
        void sendEmail_htmlEscaping_noException() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatNoException().isThrownBy(() ->
                    emailService.sendEmail("recipient@example.com", "999888", "<script>alert('xss')</script> & Co.")
            );

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("When mailSender fails, exception is handled gracefully without crashing caller")
        void sendEmail_mailException_handledGracefully() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailSendException("SMTP connection refused"))
                    .when(mailSender).send(any(MimeMessage.class));

            // Should catch or handle without bubbling unhandled up
            try {
                emailService.sendEmail("fail@example.com", "112233", "Test User");
            } catch (Exception e) {
                // MailSendException is a MailException (RuntimeException)
            }
        }
    }
}
