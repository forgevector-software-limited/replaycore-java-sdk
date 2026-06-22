/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An immutable HTTP response returned by an {@link HttpTransport}.
 *
 * <p>Internal type. Header lookups are case-insensitive, matching HTTP semantics.
 */
public final class HttpResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    /**
     * Creates a response.
     *
     * @param statusCode the HTTP status code
     * @param headers    the response headers (copied defensively, keyed lower-case)
     * @param body       the response body text, or {@code null} if empty
     */
    public HttpResponse(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        Map<String, String> copy = new LinkedHashMap<String, String>();
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null) {
                    copy.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
                }
            }
        }
        this.headers = Collections.unmodifiableMap(copy);
        this.body = body;
    }

    /** @return the HTTP status code */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns a response header by name, case-insensitively.
     *
     * @param name the header name
     * @return the header value, or {@code null} if absent
     */
    public String getHeader(String name) {
        return name == null ? null : headers.get(name.toLowerCase(Locale.ROOT));
    }

    /** @return the response body text, or {@code null} if empty */
    public String getBody() {
        return body;
    }
}
