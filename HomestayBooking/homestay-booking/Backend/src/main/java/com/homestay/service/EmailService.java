package com.homestay.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@homestay.com}")
    private String fromEmail;

    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachment, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            if (attachment != null && attachment.length > 0) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage(), e);
        }
    }

    /**
     * Best-effort plain text email (FR-20 invite). Returns false when SMTP is
     * missing or send fails — callers must not fail the business transaction.
     */
    public boolean trySendPlainText(String to, String subject, String text) {
        if (!StringUtils.hasText(to)) {
            return false;
        }
        String from = resolveFrom();
        if (!StringUtils.hasText(from) || "no-reply@dev.local".equalsIgnoreCase(from.trim())) {
            log.warn("Invite/plain email skipped for {} — SMTP not configured", to);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from.trim());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send plain email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private String resolveFrom() {
        if (StringUtils.hasText(fromEmail) && !"no-reply@dev.local".equalsIgnoreCase(fromEmail.trim())) {
            return fromEmail.trim();
        }
        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl
                && StringUtils.hasText(impl.getUsername())) {
            return impl.getUsername().trim();
        }
        return fromEmail;
    }
}
