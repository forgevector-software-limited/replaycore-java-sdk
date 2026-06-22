/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

/**
 * The in-process extension contract for addons that run alongside the ReplayCore
 * recorder on a Bukkit/Spigot/Paper/Folia server.
 *
 * <p><strong>Status: forward-looking contract.</strong> The types in this package
 * describe the supported way for a companion plugin to observe and annotate the
 * recorder in process. They are designed to be wired into the recorder runtime;
 * the recorder version on a given server determines which parts are live. Each
 * type documents what it requires from the host, so an addon can degrade
 * gracefully (and detect availability with {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreProvider})
 * rather than assume a capability is present.
 *
 * <p>This contract is grounded in the recorder's real structure:
 * <ul>
 *   <li>The recorder already registers services through the platform's
 *       {@code ServicesManager} (for example its kill-replay lookup service).
 *       {@link uk.co.forgevector.replaycore.api.plugin.ReplayCoreProvider}
 *       follows that same discovery pattern, without forcing this SDK to depend
 *       on the Bukkit API.</li>
 *   <li>The recorder already exposes an internal capture sink that can place a
 *       bookmark on the active recording and report whether recording is live and
 *       at which tick. {@link uk.co.forgevector.replaycore.api.plugin.RecordingService}
 *       is the public projection of that capability.</li>
 *   <li>The recorder drives a clear session lifecycle (start, segment rotation,
 *       stop). {@link uk.co.forgevector.replaycore.api.plugin.RecordingListener}
 *       and {@link uk.co.forgevector.replaycore.api.plugin.RecordingSession}
 *       project those boundaries to addons.</li>
 * </ul>
 *
 * <p>Crucially, the in-process contract is annotation-only: an addon may observe
 * sessions and add timeline bookmarks to recordings that the host already chose
 * to capture. It cannot start, stop, download, delete, or read the bytes of a
 * recording, and it cannot reach another tenant. Those operations stay with the
 * authenticated REST surface
 * ({@link uk.co.forgevector.replaycore.api.client.ReplayCoreClient}) and the
 * server-side capture policy, which keeps the addon surface free of any privilege
 * escalation path.
 */
package uk.co.forgevector.replaycore.api.plugin;
