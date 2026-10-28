/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The kind of a playable replay asset.
 *
 * Mirrors the cloud's asset {@code kind} field. The {@link #FULL_MATCH} asset is produced when a
 * collection is finalised and cannot be created directly.
 */
public enum AssetKind {

    /** The whole match. Wire value {@code "FULL_MATCH"}. */
    FULL_MATCH("FULL_MATCH"),

    /** A clip cut around a recorded event. Wire value {@code "EVENT_CLIP"}. */
    EVENT_CLIP("EVENT_CLIP"),

    /** A clip with a caller-chosen tick window. Wire value {@code "CUSTOM_CLIP"}. */
    CUSTOM_CLIP("CUSTOM_CLIP"),

    /** A curated highlight. Wire value {@code "HIGHLIGHT"}. */
    HIGHLIGHT("HIGHLIGHT"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    AssetKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "FULL_MATCH"})
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
    public static AssetKind fromWire(String value) {
        if (value != null) {
            for (AssetKind v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
