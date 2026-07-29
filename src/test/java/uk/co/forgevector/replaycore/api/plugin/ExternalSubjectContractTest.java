/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalSubjectContractTest {

    @Test
    void exactPlayerShapedVocabularyIsImmutableAndJava8Compatible() {
        UUID subjectId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID worldId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        SignedTexture skin = new SignedTexture("captured-value", "captured-signature");
        SubjectTransform transform = SubjectTransform.builder(worldId, 1.25D, 64.0D, -8.5D)
                .rotation(90.0F, 12.0F)
                .headYaw(95.0F)
                .velocity(0.1D, 0.0D, -0.2D)
                .onGround(true)
                .pose(SubjectPose.CROUCHING)
                .build();
        SubjectMetadata metadata = new SubjectMetadata(Collections.singletonMap("glowing", "true"));
        SubjectEquipment equipment = SubjectEquipment.empty();

        ExternalSubjectDescriptor descriptor = new ExternalSubjectDescriptor(subjectId,
                SubjectType.PLAYER_SHAPED, "ReplayActor", skin, transform, metadata, equipment);

        assertEquals(subjectId, descriptor.subjectId());
        assertEquals(SubjectType.PLAYER_SHAPED, descriptor.type());
        assertEquals("ReplayActor", descriptor.profileName());
        assertSame(skin, descriptor.skin());
        assertSame(transform, descriptor.initialTransform());
        assertSame(metadata, descriptor.initialMetadata());
        assertSame(equipment, descriptor.initialEquipment());
        assertEquals(worldId, descriptor.initialTransform().worldId());
    }

    @Test
    void frameCarriesReplayTickAndOptionalFullState() {
        UUID worldId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        SubjectTransform transform = SubjectTransform.builder(worldId, 0D, 70D, 0D).build();
        SubjectMetadata metadata = new SubjectMetadata(Collections.singletonMap("invisible", "false"));
        SubjectEffects effects = SubjectEffects.empty();

        ExternalSubjectFrame minimal = ExternalSubjectFrame.builder(40L, transform).build();
        ExternalSubjectFrame full = ExternalSubjectFrame.builder(41L, transform)
                .metadata(metadata)
                .equipment(SubjectEquipment.empty())
                .effects(effects)
                .build();

        assertEquals(40L, minimal.replayTick());
        assertEquals(worldId, minimal.worldId());
        assertEquals(0D, minimal.x());
        assertEquals(70D, minimal.y());
        assertEquals(0D, minimal.z());
        assertEquals(0F, minimal.yaw());
        assertEquals(0F, minimal.pitch());
        assertEquals(0F, minimal.headYaw());
        assertFalse(minimal.onGround());
        assertEquals(SubjectPose.STANDING, minimal.pose());
        assertFalse(minimal.metadata().isPresent());
        assertTrue(full.metadata().isPresent());
        assertTrue(full.equipment().isPresent());
        assertTrue(full.effects().isPresent());
    }

    @Test
    void presentationCarriesWholeSubjectVisibilityAndEffectiveTickAliases() {
        ExternalSubjectPresentation defaultPresentation = ExternalSubjectPresentation
                .builder(24L, 1L, "ReplayActor")
                .build();
        ExternalSubjectPresentation hidden = ExternalSubjectPresentation
                .builder(25L, 2L, "ReplayActor")
                .visible(false)
                .nameVisible(true)
                .build();

        assertTrue(defaultPresentation.visible());
        assertEquals(25L, hidden.replayTick());
        assertEquals(25L, hidden.effectiveTick());
        assertFalse(hidden.visible());
        assertTrue(hidden.nameVisible());
    }

    @Test
    void durableResultsExposeSeparateLocalAndCloudIdentity() {
        URI watch = URI.create("https://api.replaycore.com/v1/replay-assets/asset-1/watch-ticket/redeem");
        ReplayResult replay = new ReplayResult("local-1", "cloud-1", "npc-plugin",
                "match-77", ReplayState.PLAYABLE, watch, null, false,
                "op-1", "asset-1", ProcessingState.READY,
                Instant.parse("2026-07-28T20:00:00Z"),
                URI.create("/v1/replay-assets/asset-1"), true);
        FinalizeResult result = FinalizeResult.builder("op-1", "legacy-collection")
                .localCollectionId("local-1")
                .cloudCollectionId("cloud-1")
                .integrationKey("npc-plugin")
                .externalRecordingId("match-77")
                .state(ReplayState.PLAYABLE)
                .watchUrl(watch.toString())
                .build();

        assertEquals("local-1", replay.localCollectionId());
        assertEquals("cloud-1", replay.cloudCollectionId());
        assertEquals(ReplayState.PLAYABLE, replay.state());
        assertEquals(watch, replay.watchUrl());
        assertEquals("op-1", replay.operationId());
        assertEquals("asset-1", replay.assetId());
        assertEquals(ProcessingState.READY, replay.processingState());
        assertTrue(replay.watchTicketEligible());
        assertEquals("local-1", result.localCollectionId());
        assertEquals("cloud-1", result.cloudCollectionId());
        assertEquals("npc-plugin", result.integrationKey());
        assertEquals("match-77", result.externalRecordingId());
        assertEquals(ReplayState.PLAYABLE, result.state());
        assertEquals(watch.toString(), result.watchUrl().orElse(null));
        assertFalse(result.failureCode().isPresent());
        assertNull(replay.failureCode());
    }

    @Test
    void externalRecordingIdentityMustBeSuppliedAsAnAtomicPair() {
        BeginScopeRequest request = BeginScopeRequest
                .builder("open-1", "match-77", "external", "npc", "post-match")
                .externalRecording("npc-plugin", "match-77")
                .build();
        assertEquals("npc-plugin", request.externalRecordingIntegrationKey());
        assertEquals("match-77", request.externalRecordingId());
        assertThrows(IllegalArgumentException.class, () -> BeginScopeRequest
                .builder("open-2", "match-78", "external", "npc", "post-match")
                .externalRecording("npc-plugin", null)
                .build());
    }

    @Test
    void watchTicketCarriesDirectSingleUseRedemptionContract() {
        URI redemption = URI.create("https://api.replaycore.com/v1/replay-assets/asset-1/watch-ticket/redeem");
        Instant expiresAt = Instant.parse("2026-07-28T20:00:00Z");
        WatchTicketResult result = WatchTicketResult.ready("asset-1", "secret-token", redemption,
                Collections.<WatchTicketResult.PlaybackSegment>emptyList(), expiresAt, 60L, true);

        assertEquals("secret-token", result.token());
        assertEquals(redemption, result.redemptionUrl());
        assertEquals(expiresAt, result.expiresAt().orElse(null));
        assertTrue(result.singleUse());
        assertEquals("secret-token", result.ticket().orElse(null));
    }
}
