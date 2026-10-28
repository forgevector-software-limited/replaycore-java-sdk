/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The gameplay event an asset or catalogue filter refers to.
 *
 * Mirrors the cloud's {@code event_kind} field.
 */
public enum EventKind {

    /** A kill. Wire value {@code "KILL"}. */
    KILL("KILL"),

    /** A death. Wire value {@code "DEATH"}. */
    DEATH("DEATH"),

    /** The end of a round. Wire value {@code "ROUND_END"}. */
    ROUND_END("ROUND_END"),

    /** A win. Wire value {@code "WIN"}. */
    WIN("WIN"),

    /** An integration-defined event. Wire value {@code "CUSTOM"}. */
    CUSTOM("CUSTOM"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    EventKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "KILL"})
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a wire value to a constant, returning {@link #UNKNOWN} for any unrecognised or
     * {@code null} input rather than throwing.
     *
     * @param value the wire value from a server response, possibly {@code null}
     * @return the matching constant, or {@link #UNKNOWN}
     */
    public static EventKind fromWire(String value) {
        if (value != null) {
            for (EventKind v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
