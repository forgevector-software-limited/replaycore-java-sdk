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
 *        .ifPresent(replay -> player.sendMessage(
 *            replay.webUrl() != null ? replay.webUrl() : replay.command()));
 * }
 * }</pre>
 *
 * <p>The lookup is non-blocking and safe from the server thread.
 *
 */
public interface KillReplayApi {

    /**
     * Returns the most recent still-valid kill or death replay for the player, or an empty optional when
     * the player has no recent death-cam session (or the last one has expired). "Valid" means the session
     * token has not yet expired.
     *
     * @param playerId the player whose latest death replay is requested; must not be {@code null}
     * @return the latest valid replay, or an empty optional
     */
    Optional<KillReplay> latestKillReplay(UUID playerId);

    /**
     * Returns the most recent still-valid kill or death replay, including a
     * web-only value. Unlike {@link #latestKillReplay(UUID)}, a returned value
     * may have an empty {@link KillReplay#command() command}; use
     * {@link KillReplay#webUrl()} for its browser link.
     *
     * <p>The default preserves compatibility with providers that only support
     * in-game commands.
     *
     * @param playerId the player whose latest death replay is requested; must not be {@code null}
     * @return the latest valid replay, or an empty optional
     */
    default Optional<KillReplay> latestKillReplayWithWebUrl(UUID playerId) {
        return latestKillReplay(playerId);
    }
}
