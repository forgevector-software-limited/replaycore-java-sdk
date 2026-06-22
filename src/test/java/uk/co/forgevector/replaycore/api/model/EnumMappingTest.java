/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnumMappingTest {

    @Test
    void qualityMapsKnownAndUnknown() {
        assertEquals(Quality.STANDARD, Quality.fromWire("standard"));
        assertEquals(Quality.HD, Quality.fromWire("hd"));
        assertEquals(Quality.UNKNOWN, Quality.fromWire("4k"));
        assertEquals(Quality.UNKNOWN, Quality.fromWire(null));
    }

    @Test
    void storageTierMapsInfrequentAccess() {
        assertEquals(StorageTier.HOT, StorageTier.fromWire("hot"));
        assertEquals(StorageTier.R2_INFREQUENT_ACCESS, StorageTier.fromWire("r2_ia"));
        assertEquals(StorageTier.UNKNOWN, StorageTier.fromWire("glacier"));
    }

    @Test
    void visibilityMapsAllKnownValues() {
        assertEquals(Visibility.STAFF, Visibility.fromWire("staff"));
        assertEquals(Visibility.PRIVATE, Visibility.fromWire("private"));
        assertEquals(Visibility.UNLISTED, Visibility.fromWire("unlisted"));
        assertEquals(Visibility.PUBLIC, Visibility.fromWire("public"));
        assertEquals(Visibility.UNKNOWN, Visibility.fromWire("secret"));
    }

    @Test
    void archiveStatusMapsHyphenatedValue() {
        assertEquals(ArchiveStatus.CRASH_FINALISED, ArchiveStatus.fromWire("crash-finalised"));
        assertEquals(ArchiveStatus.ORIGINAL, ArchiveStatus.fromWire("original"));
        assertEquals(ArchiveStatus.REDACTED, ArchiveStatus.fromWire("redacted"));
    }

    @Test
    void scopesExposeWireValues() {
        assertEquals("replays:read", ApiScope.REPLAYS_READ.wireValue());
        assertEquals("replays:write", ApiScope.REPLAYS_WRITE.wireValue());
        assertEquals("servers:read", ApiScope.SERVERS_READ.wireValue());
        assertEquals("analytics:read", ApiScope.ANALYTICS_READ.wireValue());
    }
}
