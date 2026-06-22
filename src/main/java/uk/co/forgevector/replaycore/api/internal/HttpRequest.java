/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable HTTP request the client hands to an {@link HttpTransport}.
 *
 * <p>Internal type. The body, when present, is already-serialised JSON; a
 * {@code null} body means no entity is sent (used for GET requests).
 */
public final class HttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;

    /**
     * Creates a request.
     *
     * @param method  the HTTP method (for example {@code "GET"})
     * @param url     the absolute request URL
     * @param headers the request headers (copied defensively)
     * @param body    the JSON request body, or {@code null} for none
     */
    public HttpRequest(String method, String url, Map<String, String> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
        this.body = body;
    }

    /** @return the HTTP method */
    public String getMethod() {
        return method;
    }

    /** @return the absolute request URL */
    public String getUrl() {
        return url;
    }

    /** @return an unmodifiable view of the request headers */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /** @return the JSON request body, or {@code null} if none */
    public String getBody() {
        return body;
    }
}
