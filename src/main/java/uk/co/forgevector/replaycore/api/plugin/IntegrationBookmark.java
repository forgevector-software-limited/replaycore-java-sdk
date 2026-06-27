/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * An immutable timeline bookmark you build and hand to {@link ReplayCoreTimelineApi#tagTimelineEvent} to
 * mark a moment on the live recording (a kill, a round start, an objective taken). It is the canonical
 * custom-event type the recorder writes to the replay timeline, so the marker a plugin raises renders on
 * the in-browser viewer's scrubber exactly like a built-in event.
 *
 * <p>Build one with the fluent {@link #builder(String, String)} (a {@code source} and a {@code type} are
 * required), or with the all-arguments constructor. Every field is bounded and sanitised on construction:
 * {@code source} and {@code type} must be non-blank and are capped at {@link #MAX_FIELD_LENGTH} characters,
 * the optional fields are trimmed and capped likewise, and at most {@link #MAX_METADATA_ENTRIES} metadata
 * entries are retained (keys are lower-cased and restricted to {@code [a-z0-9_.-]}). Control characters are
 * stripped. This means a bookmark is always safe to record, whatever a caller passes.
 *
 * <p>Immutable and thread-safe once built. The same type backs both recorder lanes (modern 1.21 and above,
 * and legacy 1.8.8) and is written byte-for-byte identically on the wire, so a single viewer decoder reads
 * a bookmark whichever lane recorded it.
 */
public final class IntegrationBookmark {

    /** Bookmark severity, mapped to the lower-case wire labels {@code info}, {@code warning}, {@code red}. */
    public enum Severity { INFO, WARNING, RED }

    /** Maximum length (characters) of any single bounded field; over-length values are truncated. */
    public static final int MAX_FIELD_LENGTH = 128;
    /** Maximum number of metadata entries retained; extra entries are dropped. */
    public static final int MAX_METADATA_ENTRIES = 16;

    private final String source;
    private final String type;
    private final Severity severity;
    private final UUID playerUuid;
    private final String playerName;
    private final String arenaId;
    private final String message;
    private final Map<String, String> metadata;

    /**
     * Creates a bookmark directly. Prefer {@link #builder(String, String)} for readability; this
     * constructor exists for callers that already hold every field.
     *
     * @param source     the originating plugin or system name (required, non-blank)
     * @param type       the event type, for example {@code objective} or {@code arena_start} (required)
     * @param severity   the severity, or {@code null} for {@link Severity#INFO}
     * @param playerUuid the subject player's id, or {@code null}
     * @param playerName the subject player's name, or {@code null}
     * @param arenaId    the arena or match id this event belongs to, or {@code null}
     * @param message    a human-readable description, or {@code null}
     * @param metadata   extra key/value detail, or {@code null}; bounded to {@link #MAX_METADATA_ENTRIES}
     */
    public IntegrationBookmark(String source, String type, Severity severity, UUID playerUuid,
                               String playerName, String arenaId, String message,
                               Map<String, String> metadata) {
        this.source = bounded(required(source, "source"));
        this.type = bounded(required(type, "type"));
        this.severity = severity == null ? Severity.INFO : severity;
        this.playerUuid = playerUuid;
        this.playerName = nullableBounded(playerName);
        this.arenaId = nullableBounded(arenaId);
        this.message = nullableBounded(message);
        this.metadata = sanitiseMetadata(metadata);
    }

    /**
     * Starts building a bookmark with the two required fields.
     *
     * @param source the originating plugin or system name (required, non-blank)
     * @param type   the event type (required, non-blank)
     * @return a new builder
     */
    public static Builder builder(String source, String type) {
        return new Builder(source, type);
    }

    /** @return the originating plugin or system name; never {@code null} */
    public String source() { return source; }
    /** @return the event type; never {@code null} */
    public String type() { return type; }
    /** @return the severity; never {@code null} */
    public Severity severity() { return severity; }
    /** @return the subject player's id, or {@code null} */
    public UUID playerUuid() { return playerUuid; }
    /** @return the subject player's name, or {@code null} */
    public String playerName() { return playerName; }
    /** @return the arena or match id, or {@code null} */
    public String arenaId() { return arenaId; }
    /** @return the human-readable description, or {@code null} */
    public String message() { return message; }
    /** @return the sanitised, unmodifiable metadata map; never {@code null} */
    public Map<String, String> metadata() { return metadata; }

    /**
     * Renders the full {@code key=value;} wire payload as a standalone string. This is the form the
     * recorder records; an addon rarely needs it directly.
     *
     * @return the encoded payload
     */
    public String toPayload() {
        StringBuilder out = new StringBuilder(192);
        appendPayloadTo(out);
        return out.toString();
    }

    /**
     * Appends the {@code key=value;} wire payload to a caller-owned builder, so a hot path can encode
     * without allocating a fresh builder per bookmark. The output is identical to {@link #toPayload()}.
     *
     * @param out the builder to append to; must not be {@code null}
     */
    public void appendPayloadTo(StringBuilder out) {
        if (out == null) throw new IllegalArgumentException("out must not be null");
        append(out, "source", source);
        append(out, "type", type);
        append(out, "severity", severityLabel(severity));
        if (playerUuid != null) appendUuid(out, "player_uuid", playerUuid);
        if (playerName != null) append(out, "player_name", playerName);
        if (arenaId != null) append(out, "arena_id", arenaId);
        if (message != null) append(out, "message", message);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            appendMeta(out, entry.getKey(), entry.getValue());
        }
    }

    private static void append(StringBuilder out, String key, String value) {
        if (out.length() > 0) out.append(';');
        appendEscaped(out, key);
        out.append('=');
        appendEscaped(out, value);
    }

    private static void appendMeta(StringBuilder out, String key, String value) {
        if (out.length() > 0) out.append(';');
        out.append("meta_");
        appendEscaped(out, key);
        out.append('=');
        appendEscaped(out, value);
    }

    private static void appendUuid(StringBuilder out, String key, UUID uuid) {
        if (out.length() > 0) out.append(';');
        appendEscaped(out, key);
        out.append('=');
        appendUuidValue(out, uuid);
    }

    private static void appendEscaped(StringBuilder out, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == ';' || c == '=') out.append('\\');
            out.append(c);
        }
    }

    private static String severityLabel(Severity severity) {
        switch (severity) {
            case RED: return "red";
            case WARNING: return "warning";
            case INFO:
            default: return "info";
        }
    }

    private static void appendUuidValue(StringBuilder out, UUID uuid) {
        appendHex(out, uuid.getMostSignificantBits() >>> 32, 8);
        out.append('-');
        appendHex(out, uuid.getMostSignificantBits() >>> 16, 4);
        out.append('-');
        appendHex(out, uuid.getMostSignificantBits(), 4);
        out.append('-');
        appendHex(out, uuid.getLeastSignificantBits() >>> 48, 4);
        out.append('-');
        appendHex(out, uuid.getLeastSignificantBits(), 12);
    }

    private static void appendHex(StringBuilder out, long value, int digits) {
        for (int i = (digits - 1) * 4; i >= 0; i -= 4) {
            int nibble = (int) ((value >>> i) & 0xF);
            out.append((char) (nibble < 10 ? '0' + nibble : 'a' + (nibble - 10)));
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value.trim();
    }

    private static String nullableBounded(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return bounded(value.trim());
    }

    private static String bounded(String value) {
        String stripped = stripControl(value);
        if (stripped.length() <= MAX_FIELD_LENGTH) return stripped;
        return stripped.substring(0, MAX_FIELD_LENGTH);
    }

    private static Map<String, String> sanitiseMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) return Collections.emptyMap();
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (out.size() >= MAX_METADATA_ENTRIES) break;
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_.-]", "_");
            if (key.isEmpty()) continue;
            out.put(bounded(key), bounded(entry.getValue().trim()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static String stripControl(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 0x20 && c != 0x7f) || c == '\t') out.append(c);
        }
        return out.toString();
    }

    /**
     * A fluent builder for {@link IntegrationBookmark}. Not thread-safe; build one bookmark per builder.
     * The {@code source} and {@code type} are supplied to {@link IntegrationBookmark#builder(String, String)};
     * every other field is optional.
     */
    public static final class Builder {
        private final String source;
        private final String type;
        private Severity severity = Severity.INFO;
        private UUID playerUuid;
        private String playerName;
        private String arena;
        private String message;
        private Map<String, String> metadata;

        private Builder(String source, String type) {
            this.source = source;
            this.type = type;
        }

        /**
         * Sets the severity (defaults to {@link Severity#INFO}).
         *
         * @param severity the severity, or {@code null} for {@link Severity#INFO}
         * @return this builder
         */
        public Builder severity(Severity severity) {
            this.severity = severity == null ? Severity.INFO : severity;
            return this;
        }

        /**
         * Sets the subject player by id and name (either may be {@code null}).
         *
         * @param playerUuid the player's id, or {@code null}
         * @param playerName the player's name, or {@code null}
         * @return this builder
         */
        public Builder player(UUID playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            return this;
        }

        /**
         * Sets the arena or match id this event belongs to.
         *
         * @param arena the arena id, or {@code null}
         * @return this builder
         */
        public Builder arena(String arena) {
            this.arena = arena;
            return this;
        }

        /**
         * Sets a human-readable description of the event.
         *
         * @param message the message, or {@code null}
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Adds a single metadata entry. May be called repeatedly; a later call with the same key overwrites
         * the earlier value. The full set is bounded to {@link IntegrationBookmark#MAX_METADATA_ENTRIES}
         * sanitised entries on {@link #build()}.
         *
         * @param key   the metadata key (lower-cased and restricted to {@code [a-z0-9_.-]} on build)
         * @param value the metadata value
         * @return this builder
         */
        public Builder metadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new LinkedHashMap<String, String>();
            }
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Adds every entry from {@code metadata} (a convenience for a pre-built map). The full set is
         * bounded to {@link IntegrationBookmark#MAX_METADATA_ENTRIES} sanitised entries on {@link #build()}.
         *
         * @param metadata the metadata to add, or {@code null} for none
         * @return this builder
         */
        public Builder metadata(Map<String, String> metadata) {
            if (metadata != null && !metadata.isEmpty()) {
                if (this.metadata == null) {
                    this.metadata = new LinkedHashMap<String, String>();
                }
                this.metadata.putAll(metadata);
            }
            return this;
        }

        /**
         * Builds the immutable bookmark, applying all field bounds and sanitisation.
         *
         * @return a new {@link IntegrationBookmark}
         * @throws IllegalArgumentException if {@code source} or {@code type} is blank
         */
        public IntegrationBookmark build() {
            return new IntegrationBookmark(source, type, severity, playerUuid, playerName, arena,
                    message, metadata);
        }
    }
}
