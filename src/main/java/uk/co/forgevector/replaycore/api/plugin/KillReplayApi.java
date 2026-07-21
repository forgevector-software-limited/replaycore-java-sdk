/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Programmatic surface a server owner's plugin can call to wire the death-cam into its own messages. It
 * exposes the most recent valid kill or death replay for a player, so the owner can (for example) add a
 * "[Click to view replay]" link to their custom death message without depending on PlaceholderAPI.
 *
 * <p>Obtain it from {@link ReplayCoreApi#killReplay()} (empty when the death-cam feature is disabled), or
 * directly from Bukkit's services manager:
 * <pre>{@code
 * RegisteredServiceProvider<KillReplayApi> rsp =
 *     getServer().getServicesManager().getRegistration(KillReplayApi.class);
 * if (rsp != null) {
 *     rsp.getProvider().latestKillReplayWithWebUrl(playerId)
 *        .ifPresent(replay -> player.sendMessage(replay.webUrl() != null ? replay.webUrl() : replay.command()));
 * }
 * }</pre>
 *
 * <p>The API never blocks and never touches the main-thread hot path: it reads an in-memory registry
 * populated by the death-cam flow.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface KillReplayApi {

    /**
     * Returns the most recent still-valid kill or death replay for the player, or an empty optional when
     * the player has no recent death-cam session (or the last one has expired). "Valid" means the session
     * token has not yet expired.
     *
     * @param playerId the player whose latest death replay is requested; must not be {@code null}
     * @return the latest valid replay with a ready-to-run, non-blank command, or an empty optional
     */
    Optional<KillReplay> latestKillReplay(UUID playerId);

    /**
     * Returns the most recent still-valid kill or death replay for the player, including web-link-only
     * records. Unlike {@link #latestKillReplay(UUID)}, a returned record may have an empty
     * {@link KillReplay#command() command}; use {@link KillReplay#webUrl()} for its browser watch link.
     *
     * <p>This additive method has a default implementation so existing providers remain source and binary
     * compatible. Providers that support web-link-only death-cam sessions override it.
     *
     * @param playerId the player whose latest death replay is requested; must not be {@code null}
     * @return the latest valid replay, including a web-link-only replay, or an empty optional
     */
    default Optional<KillReplay> latestKillReplayWithWebUrl(UUID playerId) {
        return latestKillReplay(playerId);
    }
}
