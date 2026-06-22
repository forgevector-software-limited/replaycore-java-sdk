/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The production {@link HttpTransport}, built on {@link HttpURLConnection}.
 *
 * <p>It is deliberately written against the Java 8 networking API rather than the
 * Java 11 {@code java.net.http.HttpClient}, so the SDK loads cleanly inside a
 * Minecraft plugin running on a Java 8 JVM. Connect and read timeouts are applied
 * to every request; a body, when present, is written as UTF-8.
 *
 * <p>Both success (2xx) and error (4xx/5xx) responses are returned to the caller;
 * mapping an error status to an exception is the client's responsibility, not the
 * transport's. Only a genuine I/O failure throws.
 */
public final class HttpUrlConnectionTransport implements HttpTransport {

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    /**
     * Creates a transport with the given timeouts.
     *
     * @param connectTimeoutMillis the connect timeout in milliseconds (must be &ge; 0)
     * @param readTimeoutMillis    the read timeout in milliseconds (must be &ge; 0)
     */
    public HttpUrlConnectionTransport(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(request.getUrl()).openConnection();
        try {
            conn.setConnectTimeout(connectTimeoutMillis);
            conn.setReadTimeout(readTimeoutMillis);
            conn.setRequestMethod(request.getMethod());
            conn.setInstanceFollowRedirects(false);
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }

            String body = request.getBody();
            if (body != null) {
                conn.setDoOutput(true);
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                OutputStream out = conn.getOutputStream();
                try {
                    out.write(payload);
                } finally {
                    out.close();
                }
            }

            int status = conn.getResponseCode();
            Map<String, String> headers = collectHeaders(conn);
            String responseBody = readBody(conn, status);
            return new HttpResponse(status, headers, responseBody);
        } finally {
            conn.disconnect();
        }
    }

    private static Map<String, String> collectHeaders(HttpURLConnection conn) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key != null && values != null && !values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        }
        return headers;
    }

    private static String readBody(HttpURLConnection conn, int status) throws IOException {
        // For error statuses the body arrives on the error stream, not the input
        // stream; read whichever is present so problem+json bodies are captured.
        InputStream stream = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return null;
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            byte[] bytes = buffer.toByteArray();
            return bytes.length == 0 ? null : new String(bytes, StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }
}
