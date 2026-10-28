/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validation of caller-supplied request headers.
 *
 * <p>Internal type. The client owns the headers that identify the caller and
 * describe the entity it sends, so a caller-supplied header may add to a request
 * but never replace one of those. Names and values are checked so that a header
 * cannot carry a second header or a second request.
 */
public final class Headers {

    /** The header carrying a caller-supplied idempotency key. */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final List<String> RESERVED = Collections.unmodifiableList(Arrays.asList(
            "authorization", "host", "user-agent", "accept", "content-type"));

    private static final String TOKEN_PUNCTUATION = "!#$%&'*+-.^_`|~";

    private Headers() {
    }

    /**
     * Checks a caller-supplied header map and returns an ordered copy.
     *
     * @param headers the headers to send in addition to the client's own; may be {@code null}
     * @return an unmodifiable ordered copy, empty when {@code headers} is {@code null}
     * @throws IllegalArgumentException if a name is not an HTTP token, a name or
     *         value is blank or contains CR, LF or NUL, a name repeats
     *         (ignoring case), or a name is one the client controls
     *         ({@code Authorization}, {@code Host}, {@code User-Agent},
     *         {@code Accept} or {@code Content-Type})
     */
    public static Map<String, String> validate(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.<String, String>emptyMap();
        }
        Map<String, String> copy = new LinkedHashMap<String, String>();
        Set<String> seen = new HashSet<String>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("header name must not be blank");
            }
            if (!isToken(name)) {
                throw new IllegalArgumentException("header name is not a valid HTTP token: " + safe(name));
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (RESERVED.contains(lower)) {
                throw new IllegalArgumentException("header " + name + " is set by the client and cannot be overridden");
            }
            if (!seen.add(lower)) {
                throw new IllegalArgumentException("header " + name + " is supplied more than once");
            }
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("header " + name + " must have a value");
            }
            if (Validation.hasControlCharacter(value)) {
                throw new IllegalArgumentException("header " + name + " value must not contain CR, LF or NUL");
            }
            copy.put(name, value);
        }
        return Collections.unmodifiableMap(copy);
    }

    private static boolean isToken(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || TOKEN_PUNCTUATION.indexOf(c) >= 0;
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static String safe(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length() && i < 40; i++) {
            char c = name.charAt(i);
            sb.append(c < 0x20 || c == 0x7f ? '?' : c);
        }
        return sb.toString();
    }
}
