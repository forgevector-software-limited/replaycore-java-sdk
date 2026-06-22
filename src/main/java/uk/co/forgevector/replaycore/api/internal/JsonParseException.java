/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

/**
 * Thrown by {@link Json} when a document is not well-formed JSON. This is an
 * internal, unchecked failure; the client layer catches it and re-wraps it as a
 * {@link uk.co.forgevector.replaycore.api.exception.ReplayCoreTransportException}
 * so callers never see this type directly.
 */
public final class JsonParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a parse exception with a description of the malformed input.
     *
     * @param message a description of the parse failure
     */
    public JsonParseException(String message) {
        super(message);
    }
}
