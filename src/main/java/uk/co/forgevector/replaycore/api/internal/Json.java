/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal, dependency-free JSON reader and writer scoped to exactly what the
 * ReplayCore API surface needs: objects, arrays, strings, numbers, booleans and
 * null. It exists so the SDK ships with zero third-party dependencies, which
 * keeps it safe to drop into a Minecraft plugin's shaded classpath without
 * version clashes against Gson, Jackson or the platform's own bundled JSON.
 *
 * <p>This is an internal helper and is not part of the public API. Parsed values
 * map to {@link Map}{@code <String,Object>} for objects, {@link List}{@code <Object>}
 * for arrays, {@link String}, {@link Long} or {@link Double} for numbers,
 * {@link Boolean}, and {@code null}.
 *
 * <p>The implementation favours correctness and clarity over raw throughput; the
 * payloads it handles are small replay-metadata documents, not bulk data.
 */
public final class Json {

    private Json() {
    }

    /**
     * Parses a JSON document into a tree of {@link Map}, {@link List} and scalar
     * values.
     *
     * @param input the JSON text; never {@code null}
     * @return the parsed value (a {@code Map}, {@code List}, {@code String},
     *         {@code Long}, {@code Double}, {@code Boolean}, or {@code null})
     * @throws JsonParseException if the text is not well-formed JSON
     */
    public static Object parse(String input) {
        if (input == null) {
            throw new JsonParseException("input is null");
        }
        Parser parser = new Parser(input);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new JsonParseException("trailing content after JSON value at position " + parser.pos);
        }
        return value;
    }

    /**
     * Parses a JSON document that is expected to be an object.
     *
     * @param input the JSON text; never {@code null}
     * @return the parsed object as an ordered map
     * @throws JsonParseException if the text is not a well-formed JSON object
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String input) {
        Object value = parse(input);
        if (!(value instanceof Map)) {
            throw new JsonParseException("expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    /**
     * Serialises a value tree (the same shape {@link #parse(String)} produces)
     * back into compact JSON text.
     *
     * @param value the value to serialise; may be {@code null}
     * @return the compact JSON representation
     */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean || value instanceof Long
                || value instanceof Integer || value instanceof Short
                || value instanceof Byte) {
            sb.append(value.toString());
        } else if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("cannot serialise non-finite number");
            }
            sb.append(value.toString());
        } else if (value instanceof Map) {
            writeObject(sb, (Map<?, ?>) value);
        } else if (value instanceof List) {
            writeArray(sb, (List<?>) value);
        } else {
            // Fall back to a quoted string for any other type rather than
            // emitting an unquoted token that would corrupt the document.
            writeString(sb, value.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            writeValue(sb, list.get(i));
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** A recursive-descent parser over a JSON document. */
    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
        }

        boolean atEnd() {
            return pos >= src.length();
        }

        void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        Object readValue() {
            skipWhitespace();
            if (atEnd()) {
                throw new JsonParseException("unexpected end of input");
            }
            char c = src.charAt(pos);
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case '"':
                    return readString();
                case 't':
                case 'f':
                    return readBoolean();
                case 'n':
                    return readNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return readNumber();
                    }
                    throw new JsonParseException("unexpected character '" + c + "' at position " + pos);
            }
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw new JsonParseException("expected object key at position " + pos);
                }
                String key = readString();
                skipWhitespace();
                expect(':');
                Object value = readValue();
                map.put(key, value);
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new JsonParseException("expected ',' or '}' at position " + (pos - 1));
                }
            }
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<Object>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = readValue();
                list.add(value);
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new JsonParseException("expected ',' or ']' at position " + (pos - 1));
                }
            }
        }

        private String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new JsonParseException("unterminated string");
                }
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (atEnd()) {
                        throw new JsonParseException("unterminated escape sequence");
                    }
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'u':
                            sb.append(readUnicodeEscape());
                            break;
                        default:
                            throw new JsonParseException("invalid escape '\\" + esc + "'");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private char readUnicodeEscape() {
            if (pos + 4 > src.length()) {
                throw new JsonParseException("truncated unicode escape");
            }
            String hex = src.substring(pos, pos + 4);
            pos += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw new JsonParseException("invalid unicode escape '\\u" + hex + "'");
            }
        }

        private Object readNumber() {
            int start = pos;
            boolean floating = false;
            if (peek() == '-') {
                pos++;
            }
            while (!atEnd()) {
                char c = src.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    floating = true;
                    pos++;
                } else {
                    break;
                }
            }
            String token = src.substring(start, pos);
            try {
                if (floating) {
                    return Double.valueOf(token);
                }
                return Long.valueOf(token);
            } catch (NumberFormatException e) {
                throw new JsonParseException("invalid number '" + token + "'");
            }
        }

        private Boolean readBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonParseException("invalid literal at position " + pos);
        }

        private Object readNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonParseException("invalid literal at position " + pos);
        }

        private char peek() {
            if (atEnd()) {
                throw new JsonParseException("unexpected end of input");
            }
            return src.charAt(pos);
        }

        private char next() {
            if (atEnd()) {
                throw new JsonParseException("unexpected end of input");
            }
            return src.charAt(pos++);
        }

        private void expect(char expected) {
            char actual = next();
            if (actual != expected) {
                throw new JsonParseException("expected '" + expected + "' but found '" + actual
                        + "' at position " + (pos - 1));
            }
        }
    }
}
