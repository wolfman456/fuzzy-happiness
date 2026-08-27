package com.gamer.fowever.tabletopserv.email;

public interface EmailSender {

    void sendVerificationEmail(String to, String verificationUrl);
}