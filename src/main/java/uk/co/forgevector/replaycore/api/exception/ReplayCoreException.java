/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

/**
 * Base type for every checked failure raised by the ReplayCore SDK.
 *
 * <p>Two broad families extend this class:
 * <ul>
 *   <li>{@link ReplayCoreApiException} &mdash; the server answered, but with a
 *       non-success status (4xx/5xx). It carries the parsed problem detail so a
 *       caller can branch on {@link ReplayCoreApiException#getCode()} or the HTTP
 *       status.</li>
 *   <li>{@link ReplayCoreTransportException} &mdash; the request never produced a
 *       usable response (DNS failure, connection refused, socket timeout,
 *       malformed body). It wraps the underlying I/O cause.</li>
 * </ul>
 *
 * <p>The SDK never throws unchecked exceptions for ordinary remote failures, so a
 * single {@code catch (ReplayCoreException e)} is enough to handle anything the
 * network can throw at the caller.
 */
public class ReplayCoreException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a human-readable message.
     *
     * @param message a description of what failed; never {@code null}
     */
    public ReplayCoreException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and an underlying cause.
     *
     * @param message a description of what failed; never {@code null}
     * @param cause   the underlying failure, or {@code null} if none
     */
    public ReplayCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
