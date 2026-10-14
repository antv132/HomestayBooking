package com.homestay.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@homestay.test");
    }

    @Test
    void sendEmailWithAttachment_ok() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendEmailWithAttachment(
                "a@b.c", "Subject", "Body", new byte[]{1, 2, 3}, "file.pdf");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void trySendPlainText_skipsBlankTo() {
        assertFalse(service.trySendPlainText("  ", "S", "T"));
    }

    @Test
    void trySendPlainText_skipsDevFrom() {
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@dev.local");
        assertFalse(service.trySendPlainText("a@b.c", "S", "T"));
    }

    @Test
    void trySendPlainText_ok() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        assertTrue(service.trySendPlainText("a@b.c", "S", "T"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void trySendPlainText_returnsFalseOnFailure() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));
        assertFalse(service.trySendPlainText("a@b.c", "S", "T"));
    }

    @Test
    void trySendPlainText_usesImplUsernameFallback() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setUsername("smtp-user@homestay.test");
        EmailService svc = new EmailService(impl);
        ReflectionTestUtils.setField(svc, "fromEmail", "no-reply@dev.local");
        // No SMTP host configured — should fail soft and return false
        assertFalse(svc.trySendPlainText("a@b.c", "S", "T"));
    }
}
