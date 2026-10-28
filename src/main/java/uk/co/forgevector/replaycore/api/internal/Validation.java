/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Argument checks shared by the request builders.
 *
 * <p>Internal type. Each check mirrors the cloud's own validation of the same
 * field, so a bad value fails in the caller's JVM with an
 * {@link IllegalArgumentException} rather than after a network round trip.
 */
public final class Validation {

    /** The longest idempotency key the cloud accepts. */
    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 200;

    /** The most metadata entries a collection may carry. */
    public static final int MAX_METADATA_KEYS = 32;

    private static final int MAX_METADATA_KEY_LENGTH = 64;
    private static final int MAX_METADATA_VALUE_LENGTH = 256;

    private static final Pattern UUID = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern METADATA_KEY = Pattern.compile(
            "^[a-z0-9][a-z0-9_.-]*(?::[a-z0-9][a-z0-9_.-]*)*$");

    private Validation() {
    }

    /**
     * Requires a value that is neither {@code null} nor blank.
     *
     * @param value the value to check
     * @param field the field name used in the failure message
     * @return the value, unchanged
     * @throws IllegalArgumentException if {@code null} or blank
     */
    public static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * Requires bounded free text with no carriage return, line feed or NUL.
     *
     * @param value the value to check
     * @param field the field name used in the failure message
     * @param max   the longest accepted length
     * @return the trimmed value
     * @throws IllegalArgumentException if blank, too long or containing a control character
     */
    public static String requireText(String value, String field, int max) {
        requireNonBlank(value, field);
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        if (hasControlCharacter(trimmed)) {
            throw new IllegalArgumentException(field + " must not contain CR, LF or NUL");
        }
        return trimmed;
    }

    /**
     * Requires a canonical UUID and normalises it to lower case, as the cloud does.
     *
     * @param value the value to check
     * @param field the field name used in the failure message
     * @return the trimmed, lower-case UUID
     * @throws IllegalArgumentException if blank or not a UUID
     */
    public static String requireUuid(String value, String field) {
        requireNonBlank(value, field);
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        if (!UUID.matcher(normalised).matches()) {
            throw new IllegalArgumentException(field + " must be a valid UUID");
        }
        return normalised;
    }

    /**
     * Requires a well-formed idempotency key: 1 to 200 characters with no
     * carriage return, line feed or NUL.
     *
     * @param value the caller-supplied key
     * @return the trimmed key
     * @throws IllegalArgumentException if the key is not acceptable
     */
    public static String requireIdempotencyKey(String value) {
        return requireText(value, "idempotencyKey", MAX_IDEMPOTENCY_KEY_LENGTH);
    }

    /**
     * Requires a namespaced metadata key such as {@code acme:region}.
     *
     * @param value the key to check
     * @param field the field name used in the failure message
     * @return the key, unchanged
     * @throws IllegalArgumentException if the key is not well formed
     */
    public static String requireMetadataKey(String value, String field) {
        requireNonBlank(value, field);
        if (value.length() > MAX_METADATA_KEY_LENGTH || !METADATA_KEY.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a namespaced key of lower-case"
                    + " letters, digits, '.', '_' and '-', separated by ':', at most "
                    + MAX_METADATA_KEY_LENGTH + " characters");
        }
        return value;
    }

    /**
     * Checks and copies a collection's metadata map.
     *
     * @param metadata the entries to check; may be {@code null}
     * @return an unmodifiable ordered copy
     * @throws IllegalArgumentException if there are too many entries, a key is
     *                                  malformed, or a value is too long or contains a control character
     */
    public static Map<String, String> requireMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return Collections.<String, String>emptyMap();
        }
        if (metadata.size() > MAX_METADATA_KEYS) {
            throw new IllegalArgumentException("metadata may contain at most " + MAX_METADATA_KEYS + " keys");
        }
        Map<String, String> copy = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = requireMetadataKey(entry.getKey(), "metadata key");
            String value = entry.getValue();
            if (value == null || value.length() > MAX_METADATA_VALUE_LENGTH || hasControlCharacter(value)) {
                throw new IllegalArgumentException("metadata value for " + key + " must be at most "
                        + MAX_METADATA_VALUE_LENGTH + " characters with no CR, LF or NUL");
            }
            copy.put(key, value);
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Requires a tick position of zero or more.
     *
     * @param value the tick
     * @param field the field name used in the failure message
     * @return the tick
     * @throws IllegalArgumentException if negative
     */
    public static long requireTick(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    /**
     * Reports whether a string contains a carriage return, line feed or NUL.
     *
     * @param value the string to inspect
     * @return {@code true} if one of those characters is present
     */
    public static boolean hasControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\0') {
                return true;
            }
        }
        return false;
    }
}
