package com.uniticket.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Supports both epoch millis and ISO-8601 strings for LocalDateTime.
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            long epochMillis = p.getLongValue();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            if (text.chars().allMatch(Character::isDigit)) {
                long epochMillis = Long.parseLong(text);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            }
            try {
                return LocalDateTime.parse(text, ISO_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw InvalidFormatException.from(p, "Invalid LocalDateTime format", text, LocalDateTime.class);
            }
        }
        return (LocalDateTime) ctxt.handleUnexpectedToken(LocalDateTime.class, p);
    }
}
