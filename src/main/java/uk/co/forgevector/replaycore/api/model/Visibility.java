/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * Who is permitted to watch a replay.
 *
 * <p>Mirrors the cloud's {@code visibility} field.
 * <ul>
 *   <li>{@link #STAFF} is a legacy default that behaves exactly as
 *       {@link #PRIVATE}; it is retained for older rows and never written by
 *       newer governance-aware paths.</li>
 *   <li>{@link #PRIVATE} is watchable only by tenant members.</li>
 *   <li>{@link #UNLISTED} is never listed publicly but is watchable by tenant
 *       members and by anyone holding an active share link.</li>
 *   <li>{@link #PUBLIC} is openly watchable.</li>
 * </ul>
 */
public enum Visibility {

    /** Legacy default; behaves as {@link #PRIVATE}. Wire value {@code "staff"}. */
    STAFF("staff"),

    /** Watchable only by tenant members. Wire value {@code "private"}. */
    PRIVATE("private"),

    /** Hidden from public listings but watchable via share link. Wire value {@code "unlisted"}. */
    UNLISTED("unlisted"),

    /** Openly watchable. Wire value {@code "public"}. */
    PUBLIC("public"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    Visibility(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this visibility.
     *
     * @return the wire token (for example {@code "private"})
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
    public static Visibility fromWire(String value) {
        if (value != null) {
            for (Visibility v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
