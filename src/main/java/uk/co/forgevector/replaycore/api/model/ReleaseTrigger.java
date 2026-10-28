/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * What releases a child of a replay collection from its embargo.
 *
 * Mirrors the {@code release-trigger} value of a {@link ReleasePolicy} rule.
 */
public enum ReleaseTrigger {

    /** Released when the collection's scope ends. Wire value {@code "collection_end"}. */
    COLLECTION_END("collection_end"),

    /** Released as soon as the asset's own media is ready. Wire value {@code "asset_ready"}. */
    ASSET_READY("asset_ready"),

    /** Released only by an explicit release call. Wire value {@code "explicit"}. */
    EXPLICIT("explicit"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    ReleaseTrigger(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "collection_end"})
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
    public static ReleaseTrigger fromWire(String value) {
        if (value != null) {
            for (ReleaseTrigger v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
