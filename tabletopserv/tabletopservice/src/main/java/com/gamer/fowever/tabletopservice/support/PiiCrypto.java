package com.gamer.fowever.tabletopservice.support;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Static AES-256-GCM cipher for field-level PII encryption plus a deterministic blind index
 * for lookup-sensitive values (email). The key is configured at startup from
 * {@code tabletopserv.pii.secret}; see {@link com.gamer.fowever.tabletopservice.config.PiiConfig}.
 * When no key has been configured (e.g. JPA test slices) a fixed dev key is used so round-trips
 * still work inside the same JVM/context.
 */
public final class PiiCrypto {

    private static final byte[] DEV_FALLBACK_KEY = HexFormat.of().parseHex(
            "6b3e5f7a9c1d4e8b2f6a0c3d5e7f9a1b0c2d4e6f8a0b1c3d5e7f9a0b1c3d5e7f");
    private static final String PREFIX = "v1:";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static volatile SecretKeySpec key = new SecretKeySpec(DEV_FALLBACK_KEY, "AES");
    private static final SecureRandom RANDOM = new SecureRandom();

    private PiiCrypto() {
    }

    public static synchronized void configure(byte[] keyBytes) {
        key = new SecretKeySpec(keyBytes.clone(), "AES");
    }

    public static String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_BYTES];
            RANDOM.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt PII", ex);
        }
    }

    public static String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(stored.startsWith(PREFIX) ? stored.substring(PREFIX.length()) : stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_BYTES);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(payload, GCM_IV_BYTES, payload.length - GCM_IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt PII", ex);
        }
    }

    /**
     * Deterministic blind index for email-style lookup values: HMAC-SHA256 of the trimmed,
     * lower-cased value. Stable across saves so equality queries and unique constraints work
     * even though the stored email itself is non-deterministically encrypted.
     */
    public static String emailKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute blind index", ex);
        }
    }
}