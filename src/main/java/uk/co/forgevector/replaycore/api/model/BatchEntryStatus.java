/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The outcome of one entry in a batch write.
 *
 * Mirrors the {@code status} field of each batch result. Results are positional: result {@code n}
 * describes entry {@code n} of the request.
 */
public enum BatchEntryStatus {

    /** The entry was written, or had already been written. Wire value {@code "accepted"}. */
    ACCEPTED("accepted"),

    /** The entry is invalid and will never be accepted as sent. Wire value {@code "rejected"}. */
    REJECTED("rejected"),

    /** The entry could not be written; resend the same batch. Wire value {@code "retryable"}. */
    RETRYABLE("retryable"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    BatchEntryStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "accepted"})
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
    public static BatchEntryStatus fromWire(String value) {
        if (value != null) {
            for (BatchEntryStatus v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
