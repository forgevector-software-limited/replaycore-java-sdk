/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesNestedObjectWithMixedTypes() {
        String input = "{\"a\":1,\"b\":2.5,\"c\":true,\"d\":null,"
                + "\"e\":\"hi\",\"f\":[1,2,3],\"g\":{\"nested\":\"yes\"}}";
        Map<String, Object> obj = Json.parseObject(input);
        assertEquals(1L, obj.get("a"));
        assertEquals(2.5, obj.get("b"));
        assertEquals(Boolean.TRUE, obj.get("c"));
        assertNull(obj.get("d"));
        assertEquals("hi", obj.get("e"));
        assertTrue(obj.get("f") instanceof List);
        assertEquals(3, ((List<?>) obj.get("f")).size());
        assertTrue(obj.get("g") instanceof Map);
    }

    @Test
    void parsesEscapesAndUnicode() {
        Map<String, Object> obj = Json.parseObject("{\"k\":\"line1\\ntab\\t\\u0041\"}");
        assertEquals("line1\ntab\tA", obj.get("k"));
    }

    @Test
    void rejectsTrailingContent() {
        assertThrows(JsonParseException.class, () -> Json.parse("{} extra"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(JsonParseException.class, () -> Json.parse("{\"k\":\"oops}"));
    }

    @Test
    void rejectsNonObjectForParseObject() {
        assertThrows(JsonParseException.class, () -> Json.parseObject("[1,2,3]"));
    }

    @Test
    void writesObjectWithEscaping() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("label", "say \"hi\"\n");
        obj.put("tick", 1200L);
        obj.put("ok", Boolean.TRUE);
        String json = Json.write(obj);
        assertEquals("{\"label\":\"say \\\"hi\\\"\\n\",\"tick\":1200,\"ok\":true}", json);
    }

    @Test
    void roundTripsThroughParseAndWrite() {
        String input = "{\"a\":[1,2],\"b\":\"x\"}";
        assertEquals(input, Json.write(Json.parse(input)));
    }
}
