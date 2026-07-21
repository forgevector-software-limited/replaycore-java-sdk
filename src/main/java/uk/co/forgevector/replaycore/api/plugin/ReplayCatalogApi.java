/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Programmatic surface for a network's own plugin to read the RFC-0009 replay catalogue and mint a
 * playback ticket, the in-process twin of the network-integration catalogue and watch-ticket routes
 * ({@code GET /v1/players/{uuid}/replays} and {@code POST /v1/replay-assets/{id}/watch-ticket} on the
 * Bearer developer API). A call through this interface is carried to the cloud over the recorder's own
 * HMAC-signed recording-key credential, against the recorder-lane mirror of those same two routes
 * ({@code /v1/recorder/players/{uuid}/replays}, {@code /v1/recorder/replay-assets/{id}/watch-ticket}), so
 * no separate {@code rc_live_} developer key is needed on the server for either call.
 *
 * <p>Obtain the instance from the running recorder, either through Bukkit's services manager or (once
 * wired) the umbrella {@link ReplayCoreApi}, and note that it is present only when this server has
 * network-integration scopes enabled and cloud upload configured, exactly like {@link ReplayCoreMatchApi}:
 * <pre>{@code
 * RegisteredServiceProvider<ReplayCatalogApi> rsp =
 *         getServer().getServicesManager().getRegistration(ReplayCatalogApi.class);
 * if (rsp != null) {
 *     ReplayCatalogApi catalog = rsp.getProvider();
 *     catalog.listForPlayer(playerUuid, ReplayCatalogQuery.builder()
 *                     .artifactKind(AssetKind.EVENT_CLIP)
 *                     .viewerRelation(AssetRelationship.KILL)
 *                     .limit(10)
 *                     .build())
 *             .thenAccept(page -> page.entries().forEach(entry -> {
 *                 // Render one row of a kill-feed GUI.
 *             }));
 * }
 * }</pre>
 *
 * <h2>Held content is never returned</h2>
 * <p>Every row {@link #listForPlayer} can return has already passed the cloud's embargo gate (RFC-0009
 * section 7): a held, revoked or otherwise unauthorized asset is omitted server-side, never filtered
 * client-side, and its existence is indistinguishable from an id that never resolved at all. Nothing here
 * mints a second copy of an asset per viewer either - the server derives {@link ReplayCatalogEntry#viewerRelation()}
 * PER ROW for the requested {@code viewer}, so the SAME combat clip is returned as {@link AssetRelationship#KILL}
 * when queried for the actor and {@link AssetRelationship#DEATH} when queried for the subject, exactly as
 * {@link ScopeClipRequest}'s relationship map already models on the write side (RFC-0009 section 2.4).
 *
 * <h2>Minting is separate from redeeming</h2>
 * <p>{@link #createWatchTicket} mints a short-lived, single-use ticket authorising ONE viewer to watch ONE
 * asset (RFC-0009 section 10); it never returns a permanent storage URL. Redeeming a minted ticket into
 * playable bytes is a separate, viewer-side REST call ({@code POST /v1/replay-assets/{id}/watch-ticket/redeem})
 * outside this interface's scope - this method only ever performs the mint half of that exchange.
 *
 * <h2>Reliability contract</h2>
 * <p>Both methods are safe to call from the server's main thread: neither blocks on cloud I/O, and neither
 * throws for an ordinary operational condition, matching {@link ReplayCoreMatchApi}'s own contract. A
 * {@code null} argument is the one exception a call may throw synchronously for, since that is a
 * programmer error rather than a runtime condition; every other outcome, including cloud unavailability, an
 * unknown or unauthorized id, or a saturated background boundary, is delivered through the returned
 * {@link CompletionStage} completing exceptionally instead.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface ReplayCatalogApi {

    /**
     * Lists the assets {@code viewer} relates to (as {@link AssetRelationship#KILL}, {@link AssetRelationship#DEATH}
     * or {@link AssetRelationship#PARTICIPANT}), newest first, narrowed by {@code query}'s optional facets.
     *
     * <p>Only rows the caller's cloud credentials are authorised to see are ever returned; a held, revoked
     * or otherwise unauthorized row is omitted server-side, not filtered afterwards, so nothing here can be
     * used to enumerate footage the caller does not already have access to.
     *
     * @param viewer the player whose replays to list; must not be {@code null}
     * @param query  the facets and pagination to apply; must not be {@code null} (use
     *               {@link ReplayCatalogQuery#builder()} with no facets set for an unfiltered first page)
     * @return a stage that completes promptly with the requested {@link ReplayCatalogPage}; never blocks.
     *         Completes exceptionally, never throws synchronously, for an ordinary operational condition
     *         such as cloud unavailability or a saturated background boundary
     */
    CompletionStage<ReplayCatalogPage> listForPlayer(UUID viewer, ReplayCatalogQuery query);

    /**
     * Mints a short-lived, single-use watch ticket for one asset, or reports that the asset is still being
     * processed.
     *
     * @param request the asset to mint a ticket for and the viewer identity to authorise against; must not
     *                be {@code null}
     * @return a stage that completes promptly with a {@link WatchTicketResult} - either a minted ticket
     *         ({@link WatchTicketResult#ready()} {@code true}) or a bounded preparing status
     *         ({@link WatchTicketResult#ready()} {@code false}); never blocks. Completes exceptionally,
     *         never throws synchronously, for an ordinary operational condition such as cloud
     *         unavailability, an unknown asset id, or the viewer not being authorised to watch it - RFC-0009
     *         section 7 requires "does not exist" and "exists but not authorised" to be indistinguishable,
     *         so neither is ever reported differently from the other
     */
    CompletionStage<WatchTicketResult> createWatchTicket(WatchTicketRequest request);
}
