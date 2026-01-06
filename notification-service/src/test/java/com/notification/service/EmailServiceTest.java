package com.notification.service;

import com.notification.exception.EmailSendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private void enableEmail(boolean enabled) {
        ReflectionTestUtils.setField(emailService, "emailEnabled", enabled); // set @Value field [web:575]
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com"); // set @Value field [web:575]
    }

    @Test
    void sendEmail_whenDisabled_skipsSending() {
        enableEmail(false);

        assertDoesNotThrow(() -> emailService.sendEmail("to@test.com", "sub", "text"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void sendEmail_whenEnabled_sendsMessage() {
        enableEmail(true);

        emailService.sendEmail("to@test.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class); // [web:607]
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("noreply@test.com", msg.getFrom());
        assertArrayEquals(new String[]{"to@test.com"}, msg.getTo());
        assertEquals("Subject", msg.getSubject());
        assertEquals("Body", msg.getText());
    }

    @Test
    void sendEmail_whenMailSenderThrows_throwsEmailSendException() {
        enableEmail(true);

        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailSendException ex = assertThrows(
                EmailSendException.class,
                () -> emailService.sendEmail("to@test.com", "sub", "text")
        );
        assertTrue(ex.getMessage().contains("Failed to send email"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailWithRetry_successFirstTry_returnsTrue() {
        enableEmail(true);

        // mailSender.send does nothing by default
        boolean ok = emailService.sendEmailWithRetry("to@test.com", "sub", "text", 3);

        assertTrue(ok);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailWithRetry_failThenSuccess_returnsTrue() {
        enableEmail(true);

        // First attempt throws, second succeeds (consecutive stubbing)
        doThrow(new RuntimeException("smtp down"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        boolean ok = emailService.sendEmailWithRetry("to@test.com", "sub", "text", 2);

        assertTrue(ok);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailWithRetry_allFail_returnsFalse() {
        enableEmail(true);

        doThrow(new RuntimeException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        boolean ok = emailService.sendEmailWithRetry("to@test.com", "sub", "text", 2);

        assertFalse(ok);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
