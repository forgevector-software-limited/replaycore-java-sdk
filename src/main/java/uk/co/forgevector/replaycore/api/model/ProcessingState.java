/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The processing state of a replay collection or asset.
 *
 * Mirrors the cloud's {@code processing_state} field. A collection starts in {@link #RECORDING},
 * moves to {@link #FINALIZING} when its scope ends, and settles at {@link #READY} once its footage
 * is playable. {@link #FAILED}, {@link #EXPIRED} and {@link #DELETED} are terminal.
 */
public enum ProcessingState {

    /** The match is still being recorded. Wire value {@code "recording"}. */
    RECORDING("recording"),

    /** The scope has ended and the archive is still uploading. Wire value {@code "finalizing"}. */
    FINALIZING("finalizing"),

    /** The asset is waiting to be produced. Wire value {@code "queued"}. */
    QUEUED("queued"),

    /** The footage is being uploaded. Wire value {@code "uploading"}. */
    UPLOADING("uploading"),

    /** The footage is being processed. Wire value {@code "processing"}. */
    PROCESSING("processing"),

    /** The footage is processed and playable once released. Wire value {@code "ready"}. */
    READY("ready"),

    /** Processing failed. Wire value {@code "failed"}. */
    FAILED("failed"),

    /** The footage passed its retention period. Wire value {@code "expired"}. */
    EXPIRED("expired"),

    /** The footage was deleted. Wire value {@code "deleted"}. */
    DELETED("deleted"),

    /** A value this SDK release does not recognise. */
    UNKNOWN("unknown");

    private final String wireValue;

    ProcessingState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this constant.
     *
     * @return the wire token (for example {@code "recording"})
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
    public static ProcessingState fromWire(String value) {
        if (value != null) {
            for (ProcessingState v : values()) {
                if (v.wireValue.equals(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
