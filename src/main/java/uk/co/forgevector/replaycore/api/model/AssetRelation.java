/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * How a player relates to a replay asset.
 *
 * Mirrors the cloud's {@code relation} and {@code viewer_relation} fields.
 */
public enum AssetRelation {

    /** The player made the kill. Wire value {@code "KILL"}. */
    KILL("KILL"),

    /** The player died. Wire value {@code "DEATH"}. */
    DEATH("DEATH"),

    /** The player took part in the asset's moment. Wire value {@code "PARTICIPANT"}. */
    PARTICIPANT("PARTICIPANT"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    AssetRelation(String wireValue) {
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
    public static AssetRelation fromWire(String value) {
        if (value != null) {
            for (AssetRelation v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
