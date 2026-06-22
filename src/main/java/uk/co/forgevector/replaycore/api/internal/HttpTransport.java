/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.io.IOException;

/**
 * The seam between the client and the network.
 *
 * <p>The client depends only on this interface, never on a concrete HTTP
 * implementation. The production implementation is
 * {@link HttpUrlConnectionTransport}, built on the JDK's {@link java.net.HttpURLConnection}
 * so the SDK has no third-party HTTP dependency and runs on Java 8. Tests supply
 * an in-memory implementation, which is why the SDK's unit tests never touch the
 * live API.
 */
public interface HttpTransport {

    /**
     * Executes a request and returns the response.
     *
     * @param request the request to send; never {@code null}
     * @return the response; never {@code null}
     * @throws IOException if the request cannot complete (connection failure,
     *                     timeout, or an I/O error reading the response)
     */
    HttpResponse execute(HttpRequest request) throws IOException;
}
