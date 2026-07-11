/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * Whether a connected ReplayCore server instance is currently reporting in.
 */
public enum ServerStatus {

    /** The server has reported in recently. */
    ONLINE("online"),

    /** The server has not reported in recently. */
    OFFLINE("offline"),

    /** A status introduced after this SDK version. */
    UNKNOWN("unknown");

    private final String wireValue;

    ServerStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the lowercase value used by the public API.
     *
     * @return the wire value
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a public API value without failing on a newer status.
     *
     * @param value the value from the API, possibly {@code null}
     * @return the matching status, or {@link #UNKNOWN}
     */
    public static ServerStatus fromWire(String value) {
        if (value != null) {
            for (ServerStatus status : values()) {
                if (status.wireValue.equals(value)) {
                    return status;
                }
            }
        }
        return UNKNOWN;
    }
}
