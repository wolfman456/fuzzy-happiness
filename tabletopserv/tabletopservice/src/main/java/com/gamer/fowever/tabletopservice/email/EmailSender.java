package com.gamer.fowever.tabletopservice.email;

public interface EmailSender {

    void sendVerificationEmail(String to, String verificationUrl);
}