/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded immutable player metadata snapshot supplied by the integration. */
public final class SubjectMetadata {
    public static final int MAX_ENTRIES = 32;
    private static final SubjectMetadata EMPTY = new SubjectMetadata(Collections.<String, String>emptyMap());

    private final Map<String, String> values;

    public SubjectMetadata(Map<String, String> source) {
        if (source != null && source.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("metadata must contain at most " + MAX_ENTRIES + " entries");
        }
        LinkedHashMap<String, String> copy = new LinkedHashMap<String, String>();
        if (source != null) {
            for (Map.Entry<String, String> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("metadata keys and values must not be null");
                }
                copy.put(bounded(entry.getKey(), "metadata key", 128),
                        bounded(entry.getValue(), "metadata value", 512));
            }
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    public static SubjectMetadata empty() { return EMPTY; }
    public Map<String, String> values() { return values; }

    private static String bounded(String value, String name, int maximum) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        if (trimmed.length() > maximum) throw new IllegalArgumentException(name + " is too long");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) throw new IllegalArgumentException(name + " contains a control character");
        }
        return trimmed;
    }
}
