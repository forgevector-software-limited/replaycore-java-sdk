/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The state of an idempotent collection or asset operation.
 *
 * Mirrors the {@code status} field of the update, finalise and create-asset responses.
 */
public enum OperationStatus {

    /** The operation is still being applied. Wire value {@code "pending"}. */
    PENDING("pending"),

    /** The operation finished. Wire value {@code "completed"}. */
    COMPLETED("completed"),

    /** The operation failed; see {@link ScopeOperation#getFailureCode()}. Wire value {@code "failed"}. */
    FAILED("failed"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    OperationStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "pending"})
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
    public static OperationStatus fromWire(String value) {
        if (value != null) {
            for (OperationStatus v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
