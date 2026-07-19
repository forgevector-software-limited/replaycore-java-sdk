/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.concurrent.CompletionStage;

/**
 * Programmatic surface for a network integration to open, update and end a logical match scope over the
 * server's one continuous recording, the in-process twin of the network-integration REST surface (RFC-0009).
 * A scope is a tick-window over the recording, independent of any other scope open at the same time:
 * beginning or ending a scope never starts, stops, rotates or cuts the physical recording, which is the
 * decisive difference from the existing arena/duel rotation behaviour and is what lets dozens of scopes run
 * at once on one backend.
 *
 * <p>Obtain the instance from the running recorder, either through the umbrella
 * {@link ReplayCoreApi#matches()} or directly from Bukkit's services manager, and note that it is present
 * only when this server has network-integration scopes enabled and cloud upload configured:
 * <pre>{@code
 * ReplayCoreApi api = ReplayCoreProvider.get()
 *         .orElseThrow(() -> new IllegalStateException("ReplayCore not present"));
 * ReplayCoreMatchApi matches = api.matches()
 *         .orElseThrow(() -> new IllegalStateException("network-integration scopes not available"));
 * }</pre>
 *
 * <h2>Two concurrent scopes on one backend</h2>
 * <p>Nothing below correlates the two calls; each scope is a fully independent tick-window that can begin,
 * receive updates and end in any interleaving with the other:
 * <pre>{@code
 * // Arena 7 kicks off a ranked duel.
 * CompletionStage<ReplayScope> arena7 = matches.beginScope(
 *         BeginScopeRequest.builder("match-arena7-2026-07-18T20:10Z", "ext-9f21", "duels", "ranked-1v1", "post-match")
 *                 .worlds(Collections.singletonList("arena_world"))
 *                 .participants(Arrays.asList(
 *                         ReplayParticipant.builder(steveUuid, "Steve").build(),
 *                         ReplayParticipant.builder(alexUuid, "Alex").build()))
 *                 .build());
 *
 * // Arena 12 kicks off an unrelated casual match at the same moment, on the same recording.
 * CompletionStage<ReplayScope> arena12 = matches.beginScope(
 *         BeginScopeRequest.builder("match-arena12-2026-07-18T20:10Z", "ext-9f22", "duels", "casual-1v1", "post-match")
 *                 .worlds(Collections.singletonList("arena_world"))
 *                 .participants(Arrays.asList(
 *                         ReplayParticipant.builder(notchUuid, "Notch").build(),
 *                         ReplayParticipant.builder(jebUuid, "Jeb").build()))
 *                 .build());
 *
 * // Arena 7 finishes first; arena 12 is untouched by this call.
 * arena7.thenCompose(scope -> matches.endScope(scope.scopeId(),
 *         EndScopeRequest.builder("match-arena7-2026-07-18T20:10Z-end")
 *                 .teams(Collections.singletonList(ReplayTeam.builder("steve-team").result("won").placement(1).build()))
 *                 .build()));
 * }</pre>
 *
 * <h2>Reliability contract</h2>
 * <p>Every method here is safe to call from the server's main thread: it never blocks on cloud I/O, never
 * throws for an ordinary operational condition, and fails open, so ReplayCore being unavailable can never
 * cancel a match, delay a death event or leave a player stuck loading. A {@code null} argument is the one exception a call may throw synchronously for, since that is a
 * programmer error rather than a runtime condition; every other outcome, including cloud unavailability, an
 * unknown {@code scopeId}, or an update to an already-ended scope, is delivered through the returned
 * {@link CompletionStage} instead of thrown. The returned stage itself always completes promptly with an
 * immediate, local outcome: on cloud unavailability the backend spools the request durably and the stage
 * still resolves rather than hanging, so a caller never blocks waiting on the network. The eventual
 * ready/failed outcome of spooled or still-processing work is delivered separately, through
 * {@link RecordingListener#onAssetReady} and {@link RecordingListener#onAssetFailed}, since the process that
 * issued the original call may have restarted before that outcome is known.
 *
 * <p>Every operation is idempotent: {@link #beginScope} keyed by {@link BeginScopeRequest#idempotencyKey()}
 * and {@link #endScope} keyed by {@link EndScopeRequest#idempotencyKey()} both return the original durable
 * outcome on a repeated call with the same key, rather than doing the work twice.
 */
public interface ReplayCoreMatchApi {

    /**
     * Opens a logical match scope over the server's current recording.
     *
     * <p>Re-sending a {@link BeginScopeRequest} with an idempotency key already seen on this backend
     * returns the original {@link ReplayScope} rather than opening a second one, so a plugin unsure whether
     * an earlier call reached the backend (for example after a timeout) can safely retry with the identical
     * key.
     *
     * @param request the scope to open; must not be {@code null}
     * @return a stage that completes promptly with the opened (or, on a repeated idempotency key, the
     *         original) {@link ReplayScope}; never blocks. Completes exceptionally, never throws
     *         synchronously, for an ordinary operational condition such as this backend being at its bounded
     *         limit of concurrently open scopes or no archive currently being recorded to attach the scope
     *         to; in both cases no scope is opened.
     */
    CompletionStage<ReplayScope> beginScope(BeginScopeRequest request);

    /**
     * Applies an incremental change to an open scope: participants joining or leaving, team snapshots to
     * upsert, or metadata to merge.
     *
     * @param scopeId the id of the scope to update, from {@link ReplayScope#scopeId()}; must not be
     *                {@code null}
     * @param update  the change to apply; must not be {@code null}
     * @return a stage that completes promptly once the change is durably accepted; completes exceptionally
     *         if {@code scopeId} does not identify a known, still-open scope on this backend
     */
    CompletionStage<Void> updateScope(String scopeId, ScopeUpdate update);

    /**
     * Ends a logical match scope, finalising the collection it backs. Ending a scope never starts, stops,
     * rotates or cuts the physical recording; only the logical scope closes.
     *
     * <p>Re-sending an {@link EndScopeRequest} with an idempotency key already seen for this scope returns
     * the original {@link FinalizeResult} rather than finalising a second time.
     *
     * @param scopeId the id of the scope to end, from {@link ReplayScope#scopeId()}; must not be
     *                {@code null}
     * @param request the closing details; must not be {@code null}
     * @return a stage that completes promptly with an immediate {@link FinalizeResult} reflecting the
     *         outcome known at the moment this call returns (which may be an in-progress state rather than
     *         a terminal one); completes exceptionally if {@code scopeId} does not identify a known scope
     *         on this backend, or if the scope was already ended with a different idempotency key
     */
    CompletionStage<FinalizeResult> endScope(String scopeId, EndScopeRequest request);
}
