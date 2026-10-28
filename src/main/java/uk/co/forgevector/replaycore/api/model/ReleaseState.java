/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The embargo state of a replay collection or asset.
 *
 * Mirrors the cloud's {@code release_state} field. Content starts {@link #HELD}; only a released
 * item appears in the catalogue of a key without the staff bypass scope. {@link #REVOKED} is
 * terminal.
 */
public enum ReleaseState {

    /** Embargoed; not available to catalogue readers. Wire value {@code "held"}. */
    HELD("held"),

    /** Published under the collection's release policy. Wire value {@code "released"}. */
    RELEASED("released"),

    /** Withdrawn permanently. Wire value {@code "revoked"}. */
    REVOKED("revoked"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    ReleaseState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "held"})
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
    public static ReleaseState fromWire(String value) {
        if (value != null) {
            for (ReleaseState v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
