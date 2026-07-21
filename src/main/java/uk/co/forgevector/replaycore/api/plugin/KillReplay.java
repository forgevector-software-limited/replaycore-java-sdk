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
 * <p>{@link #command()} is the ready-to-run watch command when an in-game relay is available, and is
 * never {@code null}. {@link #webUrl()} is the browser death-watch link when one is available. A value
 * returned by {@link KillReplayApi#latestKillReplay(UUID)} always has a non-blank command. The additive
 * {@link KillReplayApi#latestKillReplayWithWebUrl(UUID)} lookup can return a web-link-only value, whose
 * command is the empty string. {@link #expiresAtMillis()} is the epoch-millis after which the session
 * token is no longer valid.
 */
public final class KillReplay {

    private final UUID replayId;
    private final String command;
    private final long expiresAtMillis;
    private final String webUrl;

    /**
     * Creates a kill-replay value.
     *
     * @param replayId        the replay id; must not be {@code null}
     * @param command         the ready-to-run watch command; must not be blank
     * @param expiresAtMillis the epoch-millis after which the session token expires
     */
    public KillReplay(UUID replayId, String command, long expiresAtMillis) {
        this(replayId, command, expiresAtMillis, null);
    }

    /**
     * Creates a kill-replay value with an in-game relay command, a browser link, or both.
     *
     * @param replayId        the replay id; must not be {@code null}
     * @param command         the ready-to-run watch command, or blank when {@code webUrl} is present
     * @param expiresAtMillis the epoch-millis after which the session token expires
     * @param webUrl          the browser death-watch link, or blank when {@code command} is present
     */
    public KillReplay(UUID replayId, String command, long expiresAtMillis, String webUrl) {
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        String trimmedCommand = command == null ? "" : command.trim();
        String trimmedWebUrl = webUrl == null ? "" : webUrl.trim();
        if (trimmedCommand.isEmpty() && trimmedWebUrl.isEmpty()) {
            throw new IllegalArgumentException("command or webUrl must not be empty");
        }
        this.command = trimmedCommand;
        this.expiresAtMillis = expiresAtMillis;
        this.webUrl = trimmedWebUrl.isEmpty() ? null : trimmedWebUrl;
    }

    /** @return the replay id; never {@code null} */
    public UUID replayId() {
        return replayId;
    }

    /**
     * @return the ready-to-run watch command, never {@code null}; empty only for a web-link-only value
     *         obtained through {@link KillReplayApi#latestKillReplayWithWebUrl(UUID)}
     */
    public String command() {
        return command;
    }

    /** @return the browser death-watch link, or {@code null} when only the relay command is available */
    public String webUrl() {
        return webUrl;
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
