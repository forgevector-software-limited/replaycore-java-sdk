/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * Who may watch a replay collection or asset once it is released.
 *
 * Mirrors the cloud's collection {@code visibility} field. It is distinct from the replay-level
 * {@link Visibility}, which has a different vocabulary.
 */
public enum CollectionVisibility {

    /** Anyone with the link, and listed publicly. Wire value {@code "public"}. */
    PUBLIC("public"),

    /** Anyone with the link, but not listed. Wire value {@code "unlisted"}. */
    UNLISTED("unlisted"),

    /** Only the players who took part. Wire value {@code "participants"}. */
    PARTICIPANTS("participants"),

    /** Only staff. Wire value {@code "staff_only"}. */
    STAFF_ONLY("staff_only"),

    /** Only the account. Wire value {@code "private"}. */
    PRIVATE("private"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    CollectionVisibility(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "public"})
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
    public static CollectionVisibility fromWire(String value) {
        if (value != null) {
            for (CollectionVisibility v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
