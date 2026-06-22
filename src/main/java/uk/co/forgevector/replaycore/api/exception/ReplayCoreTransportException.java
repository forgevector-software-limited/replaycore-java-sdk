/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

/**
 * Raised when a request cannot complete the round trip to ReplayCore: the host is
 * unreachable, the connection is refused or reset, a read or connect timeout
 * elapses, or the response body cannot be read or parsed.
 *
 * <p>Unlike {@link ReplayCoreApiException}, no HTTP status is available here
 * because the server never returned a meaningful one. The original I/O failure is
 * always attached as the {@linkplain #getCause() cause}.
 */
public class ReplayCoreTransportException extends ReplayCoreException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a transport exception with a message and the originating cause.
     *
     * @param message a description of the transport failure; never {@code null}
     * @param cause   the underlying I/O failure; never {@code null}
     */
    public ReplayCoreTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
