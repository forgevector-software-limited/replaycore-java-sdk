/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The capture quality tier a replay was recorded at.
 *
 * <p>Mirrors the cloud's {@code quality} field. {@link #UNKNOWN} is returned for
 * any value a future server version introduces that this SDK release does not yet
 * recognise, so deserialisation never fails on an unexpected enum value.
 */
public enum Quality {

    /** Standard-definition capture. Wire value {@code "standard"}. */
    STANDARD("standard"),

    /** High-definition capture. Wire value {@code "hd"}. */
    HD("hd"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    Quality(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this quality tier.
     *
     * @return the lowercase wire token (for example {@code "standard"})
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
    public static Quality fromWire(String value) {
        if (value != null) {
            for (Quality q : values()) {
                if (q.wireValue.equals(value)) {
                    return q;
                }
            }
        }
        return UNKNOWN;
    }
}
