/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

/**
 * The in-process developer API for addons that run alongside the ReplayCore recorder on a
 * Bukkit/Spigot/Paper/Folia server.
 *
 * <p>A companion plugin obtains the umbrella
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreApi} from
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreProvider} (or the platform's
 * {@code ServicesManager}) once the recorder has enabled, and from there reaches each capability surface:
 * <ul>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreTimelineApi}: tag a custom timeline
 *       event onto the live recording (a kill, a round start, an objective taken). Build the event with
 *       {@link uk.co.forgevector.replaycore.api.plugin.IntegrationBookmark}, the canonical type the
 *       recorder writes, so a plugin's marker renders on the viewer's scrubber exactly like a built-in
 *       event.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.RecordingControlApi}: read whether recording is
 *       live, the current tick, and the active session, and call
 *       {@link uk.co.forgevector.replaycore.api.plugin.RecordingControlApi#updateSubject} to set a
 *       per-subject {@link uk.co.forgevector.replaycore.api.plugin.CaptureVisibility} override for
 *       vanish/disguise redaction. Every OTHER control verb remains on the roadmap, each gated behind
 *       server config or permission.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreClipApi}: save an on-demand clip from
 *       code, the in-process twin of {@code /replaycore save}. Present only when clips are enabled.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.KillReplayApi}: resolve a player's most recent
 *       death replay to surface in a custom death message.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreMatchApi}: open, update and close a
 *       logical match scope over the continuous recording, so several matches running at once each get
 *       their own replay without cutting the recording. Present only when cloud upload is configured and
 *       {@code network-integration.enabled} is not set to false.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogApi}: read the RFC-0009 replay
 *       catalogue for one player and mint a short-lived watch ticket, the in-process twin of the developer
 *       API's {@code GET /v1/players/{uuid}/replays} and {@code POST /v1/replay-assets/{id}/watch-ticket}
 *       routes. The call itself goes out over the recorder's own HMAC-signed recording-key credential
 *       against {@code /v1/recorder/players/{uuid}/replays} and
 *       {@code /v1/recorder/replay-assets/{id}/watch-ticket}, never the Bearer {@code rc_live_} lane, so an
 *       addon calling this interface never needs a separate developer key on the server. Registered under
 *       the same condition as {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreMatchApi}, but unlike
 *       the surfaces above it is obtained from the platform's {@code ServicesManager} with
 *       {@code getRegistration(ReplayCatalogApi.class)}, not from the umbrella
 *       {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreApi}, which has no catalogue accessor.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.RecordingListener} and
 *       {@link uk.co.forgevector.replaycore.api.plugin.RecordingSession}: observe the lifecycle. Four
 *       callbacks fire. {@code onRecordingStarted} and {@code onRecordingStopped} run on the server's main
 *       thread, so they may touch the platform API directly. {@code onAssetReady} and
 *       {@code onAssetFailed} run on the scope finalisation worker instead, because the outcome only
 *       becomes known once the cloud confirms it. A listener that needs the platform API from those two
 *       must schedule the work back onto the main thread itself.</li>
 * </ul>
 *
 * <p>Capability negotiation is built in: the umbrella hands back the always-present surfaces directly and
 * the optional ones as a {@link java.util.Optional}, so an addon detects what a given recorder build and
 * server config offer and degrades gracefully when ReplayCore is absent or a feature is off.
 *
 * <h2>Status of each surface</h2>
 *
 * <p>Several types in this package carry the line "Forward-looking contract: see the package documentation
 * for status". This is that status, current for this release.
 *
 * <p><strong>Implemented and callable today, on both the modern and legacy recorder lanes:</strong>
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreProvider},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreApi},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreTimelineApi},
 * {@link uk.co.forgevector.replaycore.api.plugin.IntegrationBookmark},
 * {@link uk.co.forgevector.replaycore.api.plugin.RecordingControlApi},
 * {@link uk.co.forgevector.replaycore.api.plugin.CaptureVisibility},
 * {@link uk.co.forgevector.replaycore.api.plugin.RecordingSession},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreClipApi},
 * {@link uk.co.forgevector.replaycore.api.plugin.KillReplayApi},
 * {@link uk.co.forgevector.replaycore.api.plugin.KillReplay},
 * {@link uk.co.forgevector.replaycore.api.plugin.RecordingListener},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreMatchApi} and the request, update and result
 * types it names, including {@link uk.co.forgevector.replaycore.api.plugin.ScopeClipRequest};
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogApi} and the query, page, entry and
 * watch-ticket types it names ({@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogQuery},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogPage},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogEntry},
 * {@link uk.co.forgevector.replaycore.api.plugin.WatchTicketRequest} and
 * {@link uk.co.forgevector.replaycore.api.plugin.WatchTicketResult}).
 *
 * <p><strong>Reached through {@link uk.co.forgevector.replaycore.api.plugin.ScopeClipRequest}:</strong>
 * {@link uk.co.forgevector.replaycore.api.plugin.EventKind} and
 * {@link uk.co.forgevector.replaycore.api.plugin.AssetRelationship}. A clip is built with
 * {@code ScopeClipRequest.builder(EventKind)}, and each participant's part in it is declared with
 * {@code relationship(UUID, AssetRelationship)}, so both are ordinary in-process arguments. Write code
 * against them.
 *
 * <p><strong>Reached through {@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogApi}:</strong>
 * {@link uk.co.forgevector.replaycore.api.plugin.AssetKind} ({@code artifactKind}),
 * {@link uk.co.forgevector.replaycore.api.plugin.AssetRelationship} ({@code viewerRelation}, resolved PER
 * ROW for the queried player - see {@link uk.co.forgevector.replaycore.api.plugin.ReplayCatalogEntry}'s own
 * doc comment), {@link uk.co.forgevector.replaycore.api.plugin.ProcessingState},
 * {@link uk.co.forgevector.replaycore.api.plugin.ReleaseState} and
 * {@link uk.co.forgevector.replaycore.api.plugin.ReplayVisibility}. Every one of these previously named
 * only the fields of the REST network integration API, with no in-process method accepting or returning
 * one; {@code ReplayCatalogApi} is what makes them ordinary in-process arguments and return values.
 *
 * <p><strong>Not available in-process:</strong> holding, releasing or revoking embargoed footage, and
 * redeeming a minted watch ticket into playable bytes (that half of the exchange stays a viewer-side REST
 * call - see {@link uk.co.forgevector.replaycore.api.plugin.WatchTicketResult}'s own doc comment). Those
 * remain REST endpoints. Forcing or locking server-wide capture settings from code remains on the roadmap.
 * Those settings still come from configuration only. The one exception is per-subject capture visibility
 * ({@link uk.co.forgevector.replaycore.api.plugin.RecordingControlApi#updateSubject}), which IS settable
 * from code today.
 *
 * <p><strong>Deprecated:</strong> {@link uk.co.forgevector.replaycore.api.plugin.RecordingService} and
 * {@link uk.co.forgevector.replaycore.api.plugin.Bookmark}, superseded by
 * {@link uk.co.forgevector.replaycore.api.plugin.RecordingControlApi} and
 * {@link uk.co.forgevector.replaycore.api.plugin.IntegrationBookmark}. Neither is reachable from the
 * umbrella; new code should not use them.
 *
 * <p>The contract is read, annotate and (for one narrow, per-subject case) redact: an addon may observe
 * sessions, annotate the timeline, ask for a clip of a recording the host already chose to capture, and set
 * a subject's own {@link uk.co.forgevector.replaycore.api.plugin.CaptureVisibility}. It cannot start, stop,
 * download, delete, or read the bytes of a recording, cannot change capture policy for any subject other
 * than the UUID it names, and cannot reach another tenant. Those broader operations stay with the
 * authenticated REST surface ({@link uk.co.forgevector.replaycore.api.client.ReplayCoreClient}) and the
 * server-side capture policy, which keeps the addon surface free of any privilege-escalation path.
 */
package uk.co.forgevector.replaycore.api.plugin;
