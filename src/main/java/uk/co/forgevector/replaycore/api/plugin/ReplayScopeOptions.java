/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded catalogue and capture options for {@link ReplayCoreExternalSubjectApi#openScope}. */
public final class ReplayScopeOptions {
    public static final int MAX_FIELD_LENGTH = 128;
    public static final int MAX_WORLDS = 32;
    public static final int MAX_METADATA_ENTRIES = 16;

    private final String policyId;
    private final String category;
    private final String mode;
    private final List<String> worlds;
    private final Map<String, String> metadata;

    private ReplayScopeOptions(Builder builder) {
        this.policyId = identifier(builder.policyId, "policyId");
        this.category = field(builder.category, "category");
        this.mode = field(builder.mode, "mode");
        this.worlds = worlds(builder.worlds);
        this.metadata = metadata(builder.metadata);
    }

    public static Builder builder(String policyId) { return new Builder(policyId); }

    public String policyId() { return policyId; }
    public String category() { return category; }
    public String mode() { return mode; }
    public List<String> worlds() { return worlds; }
    public Map<String, String> metadata() { return metadata; }

    private static String identifier(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        String trimmed = value.trim();
        if (trimmed.length() > MAX_FIELD_LENGTH) throw new IllegalArgumentException(name + " is too long");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) throw new IllegalArgumentException(name + " contains a control character");
        }
        return trimmed;
    }

    private static String field(String value, String name) {
        String source = identifier(value, name);
        StringBuilder result = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            boolean allowed = Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':' || c == '-';
            result.append(allowed ? c : '_');
        }
        return result.toString();
    }

    private static List<String> worlds(Collection<String> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        ArrayList<String> result = new ArrayList<String>();
        for (String value : source) {
            if (result.size() >= MAX_WORLDS) break;
            if (value == null || value.trim().isEmpty()) continue;
            result.add(identifier(value, "world"));
        }
        return Collections.unmodifiableList(result);
    }

    private static Map<String, String> metadata(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Collections.emptyMap();
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (result.size() >= MAX_METADATA_ENTRIES) break;
            if (entry.getKey() == null || entry.getValue() == null) continue;
            result.put(field(entry.getKey(), "metadata key"), identifier(entry.getValue(), "metadata value"));
        }
        return Collections.unmodifiableMap(result);
    }

    public static final class Builder {
        private final String policyId;
        private String category = "external";
        private String mode = "player-shaped";
        private Collection<String> worlds;
        private Map<String, String> metadata;

        private Builder(String policyId) { this.policyId = policyId; }
        public Builder category(String value) { this.category = value; return this; }
        public Builder mode(String value) { this.mode = value; return this; }
        public Builder worlds(Collection<String> value) { this.worlds = value; return this; }
        public Builder metadata(Map<String, String> value) { this.metadata = value; return this; }
        public ReplayScopeOptions build() { return new ReplayScopeOptions(this); }
    }
}
