package com.gamer.fowever.tabletopserv.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendVerificationEmail(String to, String verificationUrl) {
        log.info("DEV EMAIL to [{}] — verify: {}", to, verificationUrl);
    }
}