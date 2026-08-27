package com.gamer.fowever.tabletopserv.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void consoleSenderLogsVerificationLink(CapturedOutput output) {
        new ConsoleEmailSender().sendVerificationEmail("aria@example.com", "http://localhost:8080/api/auth/verify?token=abc123");

        assertThat(output.getOut()).contains("aria@example.com").contains("/api/auth/verify?token=abc123");
    }

    @Test
    void smtpSenderSendsSimpleMailMessage() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender);

        sender.sendVerificationEmail("aria@example.com", "http://example.com/api/auth/verify?token=xyz");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("aria@example.com");
        assertThat(message.getSubject()).isEqualTo("Verify your tabletop account");
        assertThat(message.getText()).contains("http://example.com/api/auth/verify?token=xyz");
    }
}