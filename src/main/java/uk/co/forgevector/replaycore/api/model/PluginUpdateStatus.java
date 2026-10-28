/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * Whether a connected server's ReplayCore plugin needs an update.
 *
 * Mirrors the {@code status} field of a server's {@code update} block, which the cloud computes
 * from one configurable policy.
 */
public enum PluginUpdateStatus {

    /** The server runs a supported, current plugin. Wire value {@code "current"}. */
    CURRENT("current"),

    /** The server runs a supported plugin that is behind the latest. Wire value {@code "outdated"}. */
    OUTDATED("outdated"),

    /** The server runs a plugin older than the minimum supported version. Wire value {@code "unsupported"}. */
    UNSUPPORTED("unsupported"),

    /** The server runs the classic plugin and should move to the Loader. Wire value {@code "migrate_to_loader"}. */
    MIGRATE_TO_LOADER("migrate_to_loader"),

    /** The server runs the Loader, which updates itself. Wire value {@code "loader"}. */
    LOADER("loader"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    PluginUpdateStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "current"})
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
    public static PluginUpdateStatus fromWire(String value) {
        if (value != null) {
            for (PluginUpdateStatus v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
