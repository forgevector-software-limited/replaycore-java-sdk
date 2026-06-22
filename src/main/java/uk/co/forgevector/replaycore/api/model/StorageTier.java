/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * Where a replay's archive currently lives in the storage hierarchy.
 *
 * <p>Mirrors the cloud's {@code storage_tier} field. A {@link #HOT} replay is
 * served quickly; an {@link #R2_INFREQUENT_ACCESS} replay has aged into
 * infrequent-access storage and may take marginally longer to retrieve.
 */
public enum StorageTier {

    /** Fast, frequently-accessed storage. Wire value {@code "hot"}. */
    HOT("hot"),

    /** Infrequent-access storage for aged replays. Wire value {@code "r2_ia"}. */
    R2_INFREQUENT_ACCESS("r2_ia"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    StorageTier(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this storage tier.
     *
     * @return the wire token (for example {@code "hot"})
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
    public static StorageTier fromWire(String value) {
        if (value != null) {
            for (StorageTier t : values()) {
                if (t.wireValue.equals(value)) {
                    return t;
                }
            }
        }
        return UNKNOWN;
    }
}
