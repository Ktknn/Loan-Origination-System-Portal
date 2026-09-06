package com.example.los.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.los.entity.LoanApplication;
import com.example.los.entity.OtpRecord;
import com.example.los.entity.enums.OtpStatus;
import com.example.los.repository.LoanApplicationRepository;
import com.example.los.repository.OtpRepository;
import com.example.los.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final int OTP_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;


   

    @Transactional
    public void sendOTP(String email) {

        otpRepository.expireAllPendingForEmail(email);

        List<LoanApplication> loans = loanApplicationRepository.findByRecipientEmail(email);
        LoanApplication loan = (loans != null && !loans.isEmpty()) ? loans.get(0) : null;
        String fullName = (loan != null && loan.getUser() != null)
                ? loan.getUser().getFullName() : null;

        String otpCode = generateOTP();

        OtpRecord otpRecord = OtpRecord.builder()
                .recipientEmail(email)
                .otpCode(otpCode)
                .status(OtpStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                .user(userRepository.findByEmail(email).orElse(null))
                .build();

        otpRepository.save(otpRecord);
        emailService.sendEmail(email, otpCode, fullName);
    }

    @Transactional
    public void verifyOTP(String email, String otpCode) {
        List<OtpRecord> otpRecords = otpRepository.findByRecipientEmailAndStatus(email, OtpStatus.PENDING);

        if (otpRecords.isEmpty()) {
            throw new RuntimeException("Không tìm thấy OTP. Vui lòng yêu cầu gửi lại.");
        }

        OtpRecord otpRecord = otpRecords.get(0);

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRecord.setStatus(OtpStatus.EXPIRED);
            otpRepository.save(otpRecord);
            throw new RuntimeException("OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        if (!otpRecord.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("Mã OTP không chính xác.");
        }

        otpRecord.setStatus(OtpStatus.VERIFIED);
        otpRecord.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otpRecord);
    }

    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

   
}
