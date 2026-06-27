/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.UUID;

/**
 * One player's most recent kill or death replay: the death-cam session a server owner can surface in
 * their own plugin so a victim (or a watching admin) can jump straight to the replay of that specific
 * death.
 *
 * <p>{@link #command()} is the ready-to-run watch command the recorder provides for this session, and
 * {@link #expiresAtMillis()} is the epoch-millis after which the session token is no longer valid. Both
 * {@link KillReplayApi} and the matching placeholder treat an expired record as "no replay available", so
 * a stale link is never offered.
 */
public final class KillReplay {

    private final UUID replayId;
    private final String command;
    private final long expiresAtMillis;

    /**
     * Creates a kill-replay value.
     *
     * @param replayId        the replay id; must not be {@code null}
     * @param command         the ready-to-run watch command; must not be blank
     * @param expiresAtMillis the epoch-millis after which the session token expires
     */
    public KillReplay(UUID replayId, String command, long expiresAtMillis) {
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        this.command = command.trim();
        this.expiresAtMillis = expiresAtMillis;
    }

    /** @return the replay id; never {@code null} */
    public UUID replayId() {
        return replayId;
    }

    /** @return the ready-to-run watch command for this death replay; never {@code null} */
    public String command() {
        return command;
    }

    /** @return the epoch-millis after which the session token is no longer valid */
    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    /**
     * Reports whether this value is still valid at {@code nowMillis}.
     *
     * @param nowMillis the current epoch-millis
     * @return {@code true} if the session token has not yet expired
     */
    public boolean valid(long nowMillis) {
        return nowMillis < expiresAtMillis;
    }
}
