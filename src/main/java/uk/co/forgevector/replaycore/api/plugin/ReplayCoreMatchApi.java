/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.concurrent.CompletionStage;

/**
 * Programmatic surface for a network integration to open, update and end a logical match scope over the
 * server's one continuous recording, the in-process twin of the network-integration REST surface.
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

    /**
     * Whether this backend implements the scope-addressed event surface below ({@link #tagScopeEvent} and
     * {@link #recordScopeClip}).
     *
     * <p>Check this once at startup and branch on the result, rather than inspecting a returned stage per
     * event: on a recorder that predates these methods both of them complete exceptionally on every call,
     * and a kill handler should not be discovering that thousands of times a match.
     *
     * @return {@code true} if scope-addressed events are implemented here; {@code false} on any recorder
     *         older than the release that added them
     */
    default boolean supportsScopeEvents() {
        return false;
    }

    /**
     * Tags a timeline marker onto one named scope, at the moment of the call.
     *
     * <p><strong>Why this exists alongside {@link ReplayCoreTimelineApi#tagTimelineEvent}</strong>
     * <p>{@link ReplayCoreTimelineApi#tagTimelineEvent} writes into the recording's own archive byte stream,
     * which carries one tick timeline for the whole server and has no concept of a scope. That is
     * unambiguous only while a single match is in progress. With several open at once, a marker raised for
     * one duel lands on the same shared timeline as every other duel's, and
     * {@link IntegrationBookmark#arenaId()} does not change that: the recorder stores that field as
     * descriptive metadata and never reads it as a routing key.
     *
     * <p>This method routes instead. The marker is written against the collection the {@code scopeId} names,
     * and each match's timeline is read back by selecting on that collection, so a marker tagged for one
     * match is not selected by another match's query.
     *
     * <p>Two consequences are worth stating, because they are easy to assume the other way round. A marker
     * written here does not appear on the shared per-archive timeline that the replay-addressed API reads,
     * because that read selects on the archive rather than on the match; putting every concurrent match's
     * markers on one archive timeline is the problem this method exists to solve. And the per-match read
     * applies the same availability rules as any other read of a match's contents, so a held match's markers
     * are not returned until it is released.
     *
     * <p><strong>The routing guarantee, and its one limit</strong>
     * <p>Given a {@code scopeId} returned by {@link #beginScope}, a marker passed here is written to the
     * collection that {@code scopeId} names, or to nothing at all. No input, timing or concurrency causes it
     * to be written to a different collection: the destination is resolved purely from the argument, never
     * inferred from an arena id, a tick, a player, or the most recently opened scope, and the routing address
     * is captured before the call returns, so a recording that rotates onto a new archive mid-call cannot
     * re-attribute it.
     *
     * <p>The limit is the argument itself. Passing one match's {@code scopeId} while handling another match's
     * event routes the marker to the match named. Keep the scope id on the match object it belongs to, rather
     * than in a shared "current match" field.
     *
     * <p><strong>What the returned stage means</strong>
     * <p>It completes promptly, before or shortly after this method returns, and reports whether the marker
     * was accepted for delivery, not that it has reached the cloud. Delivery is batched and retried in the
     * background. A marker accepted here is lost only if the process is killed before the next flush; a
     * permanent delivery failure is reported through {@link RecordingListener#onAssetFailed}. Safe to call
     * from the main thread at gameplay frequency: the call performs no file or network IO.
     *
     * @param scopeId  the scope to tag, from {@link ReplayScope#scopeId()}; must not be {@code null}
     * @param bookmark the marker to write; must not be {@code null}
     * @return a stage that completes promptly; completes exceptionally, never throws synchronously, if
     *         {@code scopeId} does not identify a known, still-open scope, if this backend's bounded event
     *         buffer is saturated, or if this backend does not implement scope-addressed events at all (see
     *         {@link #supportsScopeEvents()})
     */
    default CompletionStage<Void> tagScopeEvent(String scopeId, IntegrationBookmark bookmark) {
        // Allocated per call, deliberately. A shared pre-completed instance would be reachable by every
        // caller, and one integrator calling obtrudeValue/obtrudeException/cancel on it would corrupt the
        // result every later caller sees. This is a short-lived object on the fast path of an interface
        // whose real implementations never reach this body at all.
        java.util.concurrent.CompletableFuture<Void> unsupported =
                new java.util.concurrent.CompletableFuture<Void>();
        unsupported.completeExceptionally(new UnsupportedOperationException(
                "this ReplayCore build does not implement scope-addressed events; check "
                        + "ReplayCoreMatchApi#supportsScopeEvents() once at startup"));
        return unsupported;
    }

    /**
     * Mints one playable event clip on one named scope, around the moment of the call, with per-player
     * killer/victim/participant relationships: the building block for a kills-and-deaths catalog.
     *
     * <p>One moment produces exactly one asset carrying one relationship row per player, reachable from the
     * killer's kill feed, the victim's death feed and the match timeline. Minting a separate clip per viewer
     * is never correct: it doubles storage and processing, and it makes retention and revocation inconsistent
     * between rows representing the same real event.
     *
     * <p>The window is described relatively. See {@link ScopeClipRequest} for why no absolute tick is
     * accepted, and for exactly how the recorder clamps the window at a scope start, an archive rotation and
     * a scope end. The routing guarantee and the single limit stated on {@link #tagScopeEvent} apply here
     * unchanged, as does the meaning of the returned stage.
     *
     * <p><strong>Routing is guaranteed; footage is not filtered.</strong> This clip is a tick window over the
     * one shared recording, so it renders everything captured in those ticks, including an unrelated match
     * running simultaneously in the same world. See {@link ScopeClipRequest}'s own doc comment.
     *
     * @param scopeId the scope to mint the clip on, from {@link ReplayScope#scopeId()}; must not be
     *                {@code null}
     * @param request the moment to capture; must not be {@code null}
     * @return a stage that completes promptly; completes exceptionally, never throws synchronously, under
     *         the same conditions as {@link #tagScopeEvent}
     */
    default CompletionStage<Void> recordScopeClip(String scopeId, ScopeClipRequest request) {
        // Allocated per call for the same reason tagScopeEvent's body is; see its comment.
        java.util.concurrent.CompletableFuture<Void> unsupported =
                new java.util.concurrent.CompletableFuture<Void>();
        unsupported.completeExceptionally(new UnsupportedOperationException(
                "this ReplayCore build does not implement scope-addressed events; check "
                        + "ReplayCoreMatchApi#supportsScopeEvents() once at startup"));
        return unsupported;
    }
}
