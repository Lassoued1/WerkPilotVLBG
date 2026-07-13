package com.werkpilot.identity.application.port;

public interface PasswordResetMailPort {

    void sendPasswordResetMail(String recipientEmail, String displayName, String resetLink);
}
