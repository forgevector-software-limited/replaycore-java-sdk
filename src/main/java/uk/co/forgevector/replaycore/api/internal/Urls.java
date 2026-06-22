/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * URL helpers for building request paths and query strings.
 *
 * <p>Internal type. {@link #encode(String)} percent-encodes a value for use in a
 * query string (translating {@code +} back to {@code %20} so spaces survive),
 * which guards against a caller-supplied filter value smuggling extra query
 * parameters or path segments into the request.
 */
public final class Urls {

    private Urls() {
    }

    /**
     * Joins a base URL and a path, collapsing the boundary so exactly one slash
     * separates them.
     *
     * @param base the base URL (with or without a trailing slash)
     * @param path the path (with or without a leading slash)
     * @return the joined URL
     */
    public static String join(String base, String path) {
        StringBuilder sb = new StringBuilder(base.length() + path.length() + 1);
        sb.append(base);
        boolean baseSlash = base.endsWith("/");
        boolean pathSlash = path.startsWith("/");
        if (baseSlash && pathSlash) {
            sb.append(path.substring(1));
        } else if (!baseSlash && !pathSlash) {
            sb.append('/').append(path);
        } else {
            sb.append(path);
        }
        return sb.toString();
    }

    /**
     * Percent-encodes a single path segment so that slashes and other reserved
     * characters in caller input cannot alter the request path.
     *
     * @param segment the raw path segment
     * @return the encoded segment
     */
    public static String encodePathSegment(String segment) {
        return encode(segment);
    }

    /**
     * Appends an ordered map of parameters to a URL as a query string, encoding
     * every key and value. A {@code null} or empty map leaves the URL unchanged.
     *
     * @param url    the base URL (without a query string)
     * @param params the parameters to append
     * @return the URL with the query string appended
     */
    public static String withQuery(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append('?');
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    /**
     * Percent-encodes a value for safe inclusion in a URL.
     *
     * @param value the raw value
     * @return the encoded value
     */
    public static String encode(String value) {
        try {
            // URLEncoder targets application/x-www-form-urlencoded, which encodes
            // a space as '+'. For query-string components we want %20, so fix it
            // up here to avoid a server interpreting '+' literally.
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is guaranteed present on every JVM, so this is unreachable.
            throw new IllegalStateException("UTF-8 encoding unavailable", e);
        }
    }
}
