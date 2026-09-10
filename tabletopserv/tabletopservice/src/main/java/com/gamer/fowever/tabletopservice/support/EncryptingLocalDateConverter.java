package com.gamer.fowever.tabletopservice.support;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/**
 * Encrypts a {@link LocalDate} as an ISO-8601 string (AES-256-GCM) so dates of birth are
 * protected at rest. The backing column becomes a varchar of the ciphertext.
 */
@Converter
public class EncryptingLocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : PiiCrypto.encrypt(date.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String stored) {
        return stored == null ? null : LocalDate.parse(PiiCrypto.decrypt(stored));
    }
}