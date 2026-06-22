/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The provenance of a replay's stored archive.
 *
 * <p>Mirrors the cloud's {@code archive_status} field.
 * <ul>
 *   <li>{@link #ORIGINAL} is the archive as recorded.</li>
 *   <li>{@link #REDACTED} is a derivative produced after a redaction request.</li>
 *   <li>{@link #CRASH_FINALISED} was completed by write-ahead-log recovery after
 *       a recorder crash; such a replay is viewable but visibly flagged.</li>
 * </ul>
 */
public enum ArchiveStatus {

    /** The archive as originally recorded. Wire value {@code "original"}. */
    ORIGINAL("original"),

    /** A redacted derivative archive. Wire value {@code "redacted"}. */
    REDACTED("redacted"),

    /** Completed by crash recovery. Wire value {@code "crash-finalised"}. */
    CRASH_FINALISED("crash-finalised"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    ArchiveStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this archive status.
     *
     * @return the wire token (for example {@code "original"})
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a wire value to a constant, returning {@link #UNKNOWN} for any
     * unrecognised or {@code null} input rather than throwing.
     *
     * @param value the wire value from a server response, possibly {@code null}
     * @return the matching constant, or {@link #UNKNOWN}
     */
    public static ArchiveStatus fromWire(String value) {
        if (value != null) {
            for (ArchiveStatus s : values()) {
                if (s.wireValue.equals(value)) {
                    return s;
                }
            }
        }
        return UNKNOWN;
    }
}
