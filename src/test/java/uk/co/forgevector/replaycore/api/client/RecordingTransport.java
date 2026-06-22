/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.HttpRequest;
import uk.co.forgevector.replaycore.api.internal.HttpResponse;
import uk.co.forgevector.replaycore.api.internal.HttpTransport;

/**
 * An in-memory {@link HttpTransport} for unit tests. It records every request the
 * client makes and replays a pre-seeded queue of responses, so the SDK's
 * behaviour is exercised without any network access.
 */
final class RecordingTransport implements HttpTransport {

    private final List<HttpRequest> requests = new ArrayList<HttpRequest>();
    private final List<Object> responses = new ArrayList<Object>();
    private int index;

    /**
     * Seeds a successful response with the given status and body.
     *
     * @param status the HTTP status
     * @param body   the response body
     * @return this transport
     */
    RecordingTransport enqueue(int status, String body) {
        return enqueue(status, body, new LinkedHashMap<String, String>());
    }

    /**
     * Seeds a response with status, body and headers.
     *
     * @param status  the HTTP status
     * @param body    the response body
     * @param headers the response headers
     * @return this transport
     */
    RecordingTransport enqueue(int status, String body, Map<String, String> headers) {
        responses.add(new HttpResponse(status, headers, body));
        return this;
    }

    /**
     * Seeds an I/O failure for the next request (a transport-level error).
     *
     * @param failure the exception to throw
     * @return this transport
     */
    RecordingTransport enqueueFailure(IOException failure) {
        responses.add(failure);
        return this;
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws IOException {
        requests.add(request);
        if (index >= responses.size()) {
            throw new IllegalStateException("no response seeded for request " + index);
        }
        Object next = responses.get(index++);
        if (next instanceof IOException) {
            throw (IOException) next;
        }
        return (HttpResponse) next;
    }

    /** @return the most recent request the client made */
    HttpRequest lastRequest() {
        return requests.get(requests.size() - 1);
    }

    /** @return every request the client made, in order */
    List<HttpRequest> allRequests() {
        return requests;
    }
}
