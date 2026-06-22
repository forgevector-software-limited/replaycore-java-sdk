/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TimelineEventRequestTest {

    @Test
    void buildsReplayTargetedMarker() {
        TimelineEventRequest request = TimelineEventRequest.forReplay("replay-1")
                .tick(500)
                .label("Clutch")
                .colour("#ff0000")
                .actor("AntiCheat")
                .build();
        Map<String, Object> body = request.toBody();
        assertEquals("replay-1", body.get("replayId"));
        assertEquals(500L, body.get("tick"));
        assertEquals("Clutch", body.get("label"));
        assertEquals("#ff0000", body.get("colour"));
        assertEquals("AntiCheat", body.get("actor"));
        assertFalse(body.containsKey("serverId"));
    }

    @Test
    void serverTargetClearsReplayTarget() {
        TimelineEventRequest request = TimelineEventRequest.forActiveRecording("server-1")
                .tick(0)
                .label("Round start")
                .build();
        Map<String, Object> body = request.toBody();
        assertEquals("server-1", body.get("serverId"));
        assertFalse(body.containsKey("replayId"));
    }

    @Test
    void switchingTargetIsExclusive() {
        // Setting a server target after a replay target clears the replay target,
        // so the request carries exactly one, never both.
        TimelineEventRequest request = TimelineEventRequest.forReplay("replay-1")
                .serverId("server-1")
                .tick(1)
                .label("x")
                .build();
        Map<String, Object> body = request.toBody();
        assertEquals("server-1", body.get("serverId"));
        assertFalse(body.containsKey("replayId"));
    }

    @Test
    void rejectsNegativeTick() {
        assertThrows(IllegalArgumentException.class,
                () -> TimelineEventRequest.forReplay("r").tick(-1));
    }

    @Test
    void rejectsOverlongLabel() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 121; i++) {
            tooLong.append('x');
        }
        assertThrows(IllegalArgumentException.class,
                () -> TimelineEventRequest.forReplay("r").label(tooLong.toString()));
    }

    @Test
    void rejectsBadColour() {
        assertThrows(IllegalArgumentException.class,
                () -> TimelineEventRequest.forReplay("r").colour("red"));
    }

    @Test
    void rejectsMissingTickAndLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> TimelineEventRequest.forReplay("r").build());
        assertThrows(IllegalArgumentException.class,
                () -> TimelineEventRequest.forReplay("r").tick(1).build());
    }
}
