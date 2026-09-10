package com.gamer.fowever.tabletopservice.support;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Field-level AES-256-GCM converter for string PII (email, display name, real name).
 * Delegates to {@link PiiCrypto} so it works both when Spring registers it as a JPA
 * converter bean and when Hibernate instantiates it via a no-arg constructor.
 * Lookup-sensitive fields (email) must keep a separate deterministic blind index
 * ({@link PiiCrypto#emailKey(String)}) because the ciphertext is non-deterministic.
 */
@Converter
public class EncryptingStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String plain) {
        return PiiCrypto.encrypt(plain);
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        return PiiCrypto.decrypt(stored);
    }
}