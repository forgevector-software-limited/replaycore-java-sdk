/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Json;

/**
 * Loads the cloud contract fixtures under {@code contract/} on the test classpath.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /**
     * Reads a fixture as text.
     *
     * @param name the file name, for example {@code collection.json}
     * @return the fixture's UTF-8 text
     */
    public static String text(String name) {
        InputStream in = Fixtures.class.getResourceAsStream("/contract/" + name);
        if (in == null) {
            throw new IllegalStateException("missing contract fixture " + name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("could not read contract fixture " + name, e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // Nothing useful to do when a read-only stream will not close.
            }
        }
    }

    /**
     * Reads a fixture and parses it as a JSON object.
     *
     * @param name the file name, for example {@code collection.json}
     * @return the parsed tree
     */
    public static Map<String, Object> object(String name) {
        return Json.parseObject(text(name));
    }
}
