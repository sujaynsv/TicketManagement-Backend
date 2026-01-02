package com.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${notification.email.from}")
    private String fromEmail;
    
    @Value("${notification.email.enabled:true}")
    private Boolean emailEnabled;
    
    /**
     * Send simple text email
     */
    @Async
    public void sendEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Skipping email to {}", to);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send email with retry logic
     */
    public boolean sendEmailWithRetry(String to, String subject, String text, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                sendEmail(to, subject, text);
                return true;
            } catch (Exception e) {
                attempt++;
                log.warn("Email send attempt {} failed for {}: {}", attempt, to, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return false;
    }
}
