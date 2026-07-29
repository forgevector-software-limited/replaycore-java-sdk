/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uk.co.forgevector.replaycore.api.internal.Json;

/**
 * A successful JSON response from a generic developer or workspace-setup call.
 *
 * <p>The response tree is recursively immutable. JSON objects are represented
 * as {@code Map<String,Object>}, arrays as {@code List<Object>}, and scalar
 * values as strings, numbers, booleans or {@code null}. A successful no-content
 * response has no body.
 */
public final class ApiResponse {

    private final int statusCode;
    private final Object body;

    /**
     * Creates a response from a parsed JSON tree.
     *
     * @param statusCode the successful HTTP status
     * @param body       the parsed JSON tree, or {@code null} for no content
     */
    public ApiResponse(int statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = freeze(body);
    }

    /** @return the successful HTTP status code */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return whether the response carried a JSON body */
    public boolean hasBody() {
        return body != null;
    }

    /**
     * Returns the recursively immutable JSON tree.
     *
     * @return the parsed body, or an empty optional for a no-content response
     */
    public Optional<Object> getBody() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the body when it is a JSON object.
     *
     * @return the immutable object, or an empty optional for no body or a
     *         non-object JSON value
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getObject() {
        return body instanceof Map
                ? Optional.of((Map<String, Object>) body)
                : Optional.<Map<String, Object>>empty();
    }

    /**
     * Serialises the response body back to compact JSON.
     *
     * @return the JSON body, or an empty optional for a no-content response
     */
    public Optional<String> toJson() {
        return body == null ? Optional.<String>empty() : Optional.of(Json.write(body));
    }

    private static Object freeze(Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                copy.put(String.valueOf(entry.getKey()), freeze(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<Object>();
            for (Object element : (List<?>) value) {
                copy.add(freeze(element));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
