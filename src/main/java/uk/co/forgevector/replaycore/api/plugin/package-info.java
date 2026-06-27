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
 *       live, the current tick, and the active session (control verbs are on the roadmap, each gated
 *       behind server config or permission).</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreClipApi}: save an on-demand clip from
 *       code, the in-process twin of {@code /replaycore save}. Present only when clips are enabled.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.KillReplayApi}: resolve a player's most recent
 *       death replay to surface in a custom death message.</li>
 *   <li>{@link uk.co.forgevector.replaycore.api.plugin.RecordingListener} and
 *       {@link uk.co.forgevector.replaycore.api.plugin.RecordingSession}: observe the session lifecycle
 *       (start, stop) on the server's main thread.</li>
 * </ul>
 *
 * <p>Capability negotiation is built in: the umbrella hands back the always-present surfaces directly and
 * the optional ones as a {@link java.util.Optional}, so an addon detects what a given recorder build and
 * server config offer and degrades gracefully when ReplayCore is absent or a feature is off.
 *
 * <p>The contract is read and annotate only: an addon may observe sessions, annotate the timeline, and ask
 * for a clip of a recording the host already chose to capture. It cannot start, stop, download, delete, or
 * read the bytes of a recording, and it cannot reach another tenant. Those operations stay with the
 * authenticated REST surface ({@link uk.co.forgevector.replaycore.api.client.ReplayCoreClient}) and the
 * server-side capture policy, which keeps the addon surface free of any privilege-escalation path.
 */
package uk.co.forgevector.replaycore.api.plugin;
