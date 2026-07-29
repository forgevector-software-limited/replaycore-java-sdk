/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Generic, Bukkit-free API for recording authoritative player-shaped subjects that are not online players.
 *
 * <p>Publication methods are local, lightweight and non-blocking. {@link ExternalSubjectPublishResult#ACCEPTED}
 * means the complete record was accepted by ReplayCore's bounded durable capture seam; a saturated seam
 * reports {@link ExternalSubjectPublishResult#BACKPRESSURE} instead of dropping an accepted record later.
 */
public interface ReplayCoreExternalSubjectApi {

    ReplayScope openScope(String integrationKey, String externalRecordingId,
                          ReplayScopeOptions options);

    /** Java 8-compatible convenience for integrations that prefer a completion-stage call chain. */
    default CompletionStage<ReplayScope> openScopeAsync(String integrationKey,
                                                        String externalRecordingId,
                                                        ReplayScopeOptions options) {
        return CompletableFuture.completedFuture(openScope(integrationKey, externalRecordingId, options));
    }

    /**
     * Reads the newest durable result by compound integration identity. A live recorder may perform one
     * bounded recorder-HMAC cloud lookup when its local snapshot is not terminal, so call this from an
     * integration worker rather than a gameplay tick callback.
     */
    Optional<ReplayResult> findByExternalRecordingId(String integrationKey,
                                                     String externalRecordingId);

    /** Asynchronously repeats bounded durable waits until the recording reaches a terminal state. */
    CompletionStage<ReplayResult> awaitTerminalState(String integrationKey,
                                                     String externalRecordingId);

    /**
     * Asynchronously waits up to {@code timeout} for a durable terminal result.
     * Implementations must honour cancellation and must never retain an
     * unbounded waiter after the returned stage completes.
     */
    default CompletionStage<ReplayResult> awaitTerminalState(String integrationKey,
                                                             String externalRecordingId,
                                                             Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            CompletableFuture<ReplayResult> invalid = new CompletableFuture<ReplayResult>();
            invalid.completeExceptionally(new IllegalArgumentException(
                    "timeout must be positive"));
            return invalid;
        }
        return awaitTerminalState(integrationKey, externalRecordingId);
    }

    /**
     * Ends this logical external recording without stopping, rotating or cutting the physical recorder.
     * The request's idempotency key owns the close operation: repeating the same scope and key returns the
     * same durable outcome, while attempting to close the scope under a different key fails.
     *
     * <p>Before delegating to {@link ReplayCoreMatchApi#endScope(String, EndScopeRequest)}, ReplayCore
     * atomically unregisters only the external subjects owned by this scope at the recorder's authoritative
     * current tick. Other concurrent scopes and ordinary player capture continue unchanged.
     *
     * @return a promptly-completing stage containing the immediate durable finalisation result. Processing
     *         may still be in progress; use {@link #awaitTerminalState(String, String)} for the eventual
     *         restart-safe cloud outcome.
     */
    default CompletionStage<ReplayResult> endScope(ReplayScope scope, EndScopeRequest request) {
        CompletableFuture<ReplayResult> unsupported = new CompletableFuture<ReplayResult>();
        unsupported.completeExceptionally(new UnsupportedOperationException(
                "this ReplayCore external-subject implementation does not support scope finalisation"));
        return unsupported;
    }

    ExternalSubjectHandle registerExternalSubject(ReplayScope scope, ExternalSubjectDescriptor descriptor);

    ExternalSubjectPublishResult publishExternalSubjectFrame(ExternalSubjectHandle handle,
                                                              ExternalSubjectFrame frame);

    /**
     * Publishes a visual event. Accepted TELEPORT, EFFECT_ADD and EFFECT_REMOVE events are folded into the
     * authoritative state used by later keyframes. SPAWN, RESPAWN and DESPAWN are also supported here and
     * atomically update lifecycle state; the dedicated despawn and respawn methods are convenience forms
     * with the same canonical record and keyframe behaviour.
     *
     * @return the local capture result
     */
    ExternalSubjectPublishResult publishExternalSubjectEvent(ExternalSubjectHandle handle,
                                                              ExternalSubjectEvent event);

    ExternalSubjectPublishResult updateExternalSubjectPresentation(ExternalSubjectHandle handle,
                                                                   ExternalSubjectPresentation presentation);

    ExternalSubjectPublishResult despawnExternalSubject(ExternalSubjectHandle handle, long replayTick);

    ExternalSubjectPublishResult respawnExternalSubject(ExternalSubjectHandle handle,
                                                        ExternalSubjectFrame frame);

    ExternalSubjectPublishResult unregisterExternalSubject(ExternalSubjectHandle handle,
                                                           RemovalReason reason,
                                                           long replayTick);
}
