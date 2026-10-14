package com.homestay.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * Binds SMTP from MAIL_* env/.env explicitly so JavaMailSender does not keep
 * silent application.yml fallbacks when placeholder order hides real credentials.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);
    private static final String DEV_USERNAME_FALLBACK = "no-reply@dev.local";

    @Value("${MAIL_USERNAME:${spring.mail.username:}}")
    private String username;

    @Value("${MAIL_PASSWORD:${spring.mail.password:}}")
    private String password;

    @Value("${MAIL_HOST:${spring.mail.host:smtp.gmail.com}}")
    private String host;

    @Value("${MAIL_PORT:${spring.mail.port:587}}")
    private int port;

    @PostConstruct
    void logMailBinding() {
        if (!StringUtils.hasText(username) || DEV_USERNAME_FALLBACK.equalsIgnoreCase(username.trim())) {
            log.warn("SMTP username is missing or still the dev fallback. "
                    + "Set MAIL_USERNAME and MAIL_PASSWORD in .env (repo root). OTP emails will fail.");
            return;
        }
        int at = username.indexOf('@');
        String maskedLocal = username.substring(0, Math.min(3, at > 0 ? at : username.length())) + "***";
        String domain = at > 0 ? username.substring(at + 1) : "?";
        log.info("SMTP mail sender bound for {}@{} (host={}, port={})", maskedLocal, domain, host, port);
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
