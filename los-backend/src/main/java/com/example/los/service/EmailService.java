package com.example.los.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email OTP bất đồng bộ — chạy trên thread pool riêng.
     * OtpService.sendOTP() trả về ngay sau khi lưu DB, không chờ SMTP.
     *
     * Nếu SMTP lỗi → log warning, OTP record vẫn tồn tại trong DB.
     */
    @Async
    public void sendEmail(String toEmail, String otpCode, String fullName) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("OTP code không được để trống");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Mã OTP xác nhận ký Hợp đồng vay");
            helper.setText(buildEmailBody(otpCode, fullName), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("[EmailService] Không thể gửi OTP email đến {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String otpCode, String fullName) {
        String nameStr = escapeHtml((fullName != null && !fullName.isBlank()) ? fullName : "Quý khách");
        int yr = java.time.Year.now().getValue();

        StringBuilder digitBoxes = new StringBuilder();
        for (char c : otpCode.toCharArray()) {
            digitBoxes
                    .append("<span style=\"display:inline-block;width:44px;height:54px;line-height:54px;text-align:center;")
                    .append("font-size:28px;font-weight:800;color:#1e3a8a;background:#eff6ff;")
                    .append("border:2px solid #bfdbfe;border-radius:10px;margin:0 4px;")
                    .append("font-family:'Courier New',Courier,monospace;letter-spacing:0\">")
                    .append(c)
                    .append("</span>");
        }

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f1f5f9;padding:32px 16px\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)\">"

                // Header
                + "<tr><td style=\"background:linear-gradient(135deg,#1e3a8a 0%,#1d4ed8 100%);padding:36px 40px;text-align:center\">"
                + "<div style=\"font-size:40px;margin-bottom:12px\">🏦</div>"
                + "<h1 style=\"margin:0;font-size:22px;font-weight:700;color:#ffffff;letter-spacing:-0.3px\">Xác nhận ký Hợp đồng vay</h1>"
                + "<p style=\"margin:6px 0 0;font-size:13px;color:#bfdbfe\">Mã xác thực một lần (OTP)</p>"
                + "</td></tr>"

                // Body
                + "<tr><td style=\"padding:36px 40px\">"
                + "<p style=\"margin:0 0 8px;font-size:15px;color:#1e293b\">Chào Ông/Bà <strong>" + nameStr
                + "</strong>,</p>"
                + "<p style=\"margin:0 0 28px;font-size:14px;color:#475569;line-height:1.7\">"
                + "Chúng tôi nhận được yêu cầu xác nhận ký Hợp đồng vay trực tuyến của Quý khách. "
                + "Vui lòng sử dụng mã OTP bên dưới để hoàn tất thủ tục.</p>"

                // OTP digits
                + "<div style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:28px;text-align:center;margin-bottom:20px\">"
                + "<p style=\"margin:0 0 18px;font-size:11px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:1.2px\">Mã xác thực của bạn</p>"
                + digitBoxes
                + "<p style=\"margin:20px 0 0;font-size:12px;color:#94a3b8\">Hiệu lực <strong style=\"color:#d97706\">10 phút</strong> &nbsp;·&nbsp; Chỉ dùng <strong style=\"color:#d97706\">1 lần</strong></p>"
                + "</div>"

                // Expiry note
                + "<div style=\"background:#fffbeb;border:1px solid #fde68a;border-radius:10px;padding:14px 16px;margin-bottom:28px\">"
                + "<p style=\"margin:0;font-size:13px;color:#78350f;line-height:1.65\">"
                + "&#9201; Nếu Quý khách <strong>không</strong> thực hiện yêu cầu này, vui lòng bỏ qua email và kiểm tra lại tài khoản ngay.</p>"
                + "</div>"

                // Security warning
                + "<div style=\"background:#fef2f2;border:1px solid #fecaca;border-radius:10px;padding:14px 16px\">"
                + "<p style=\"margin:0;font-size:12px;color:#7f1d1d;line-height:1.65\">"
                + "&#128274; <strong>Lưu ý bảo mật:</strong> Tuyệt đối <strong>KHÔNG chia sẻ</strong> mã OTP này cho bất kỳ ai, "
                + "kể cả nhân viên ngân hàng. Chúng tôi không bao giờ hỏi mã OTP qua điện thoại hay tin nhắn.</p>"
                + "</div>"

                + "</td></tr>"

                // Footer
                + "<tr><td style=\"background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center\">"
                + "<p style=\"margin:0 0 4px;font-size:12px;color:#64748b\">Đây là email tự động, vui lòng không trả lời.</p>"
                + "<p style=\"margin:0;font-size:11px;color:#94a3b8\">© " + yr + " · Tất cả các quyền được bảo lưu</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    /** Escape HTML entities — ngăn XSS nếu input chứa <, >, & */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
