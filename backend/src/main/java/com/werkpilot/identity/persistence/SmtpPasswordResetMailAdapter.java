package com.werkpilot.identity.persistence;

import com.werkpilot.identity.application.port.PasswordResetMailPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpPasswordResetMailAdapter implements PasswordResetMailPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpPasswordResetMailAdapter(
            JavaMailSender mailSender,
            @Value("${werkpilot.identity.mail-from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetMail(String recipientEmail, String displayName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("WerkPilot Passwort zuruecksetzen");
        message.setText("""
                Hallo %s,

                zum Zuruecksetzen Ihres WerkPilot Passworts oeffnen Sie bitte diesen Link:
                %s

                Der Link ist 60 Minuten gueltig und kann nur einmal verwendet werden.
                """.formatted(displayName, resetLink));
        mailSender.send(message);
    }
}
