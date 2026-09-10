package com.gamer.fowever.tabletopservice.config;

import com.gamer.fowever.tabletopservice.support.PiiCrypto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Reads {@code tabletopserv.pii.secret} and configures {@link PiiCrypto} with a 256-bit key
 * derived (SHA-256) from the secret. Fails fast on startup when the secret is missing so PII
 * is never encrypted with the dev fallback key in a configured runtime.
 */
@Configuration
public class PiiConfig {

    private final String secret;

    public PiiConfig(@Value("${tabletopserv.pii.secret:}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void configure() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("tabletopserv.pii.secret must be configured");
        }
        PiiCrypto.configure(SHA256(secret));
    }

    private byte[] SHA256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}