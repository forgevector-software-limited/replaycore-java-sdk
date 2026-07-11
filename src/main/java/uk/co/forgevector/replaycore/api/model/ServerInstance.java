/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One Minecraft server instance connected to a ReplayCore account.
 *
 * <p>The live player count is absent while an instance is offline. The plugin
 * version is the last version reported by that instance and may still be
 * available while it is offline.
 */
public final class ServerInstance {

    private final String id;
    private final String name;
    private final ServerStatus status;
    private final Instant lastSeenAt;
    private final String pluginVersion;
    private final Integer playerCount;
    private final long replayCount;

    /**
     * Constructs a server instance from the public API response.
     *
     * @param id            the stable server id; never {@code null}
     * @param name          the display name; never {@code null}
     * @param status        whether the instance is online; never {@code null}
     * @param lastSeenAt    when it last reported in, or {@code null}
     * @param pluginVersion the last reported ReplayCore plugin version, or {@code null}
     * @param playerCount   the live player count, or {@code null} while unavailable
     * @param replayCount   the number of replays recorded by this instance
     */
    public ServerInstance(String id, String name, ServerStatus status, Instant lastSeenAt,
                          String pluginVersion, Integer playerCount, long replayCount) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.lastSeenAt = lastSeenAt;
        this.pluginVersion = pluginVersion;
        this.playerCount = playerCount;
        this.replayCount = replayCount;
    }

    /** @return the stable server id */
    public String getId() {
        return id;
    }

    /** @return the server's display name */
    public String getName() {
        return name;
    }

    /** @return the current reporting status */
    public ServerStatus getStatus() {
        return status;
    }

    /** @return when the server last reported in, or an empty optional */
    public Optional<Instant> getLastSeenAt() {
        return Optional.ofNullable(lastSeenAt);
    }

    /** @return the last reported plugin version, or an empty optional */
    public Optional<String> getPluginVersion() {
        return Optional.ofNullable(pluginVersion);
    }

    /**
     * Returns the live player count. It is absent when the server is offline or
     * has not yet reported a count.
     *
     * @return the player count, or an empty optional
     */
    public Optional<Integer> getPlayerCount() {
        return Optional.ofNullable(playerCount);
    }

    /** @return the number of replays recorded by this server */
    public long getReplayCount() {
        return replayCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServerInstance)) {
            return false;
        }
        ServerInstance that = (ServerInstance) other;
        return replayCount == that.replayCount
                && id.equals(that.id)
                && name.equals(that.name)
                && status == that.status
                && Objects.equals(lastSeenAt, that.lastSeenAt)
                && Objects.equals(pluginVersion, that.pluginVersion)
                && Objects.equals(playerCount, that.playerCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, status, lastSeenAt, pluginVersion, playerCount, replayCount);
    }

    @Override
    public String toString() {
        return "ServerInstance{id=" + id + ", name=" + name + ", status=" + status
                + ", lastSeenAt=" + lastSeenAt + ", pluginVersion=" + pluginVersion
                + ", playerCount=" + playerCount + ", replayCount=" + replayCount + '}';
    }
}
