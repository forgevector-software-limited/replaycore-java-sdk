/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;

/**
 * The discovery point through which an addon obtains the running recorder's
 * {@link ReplayCoreApi}.
 *
 * <p>When the recorder enables, it publishes its {@link ReplayCoreApi}
 * implementation here (and, on a Bukkit platform, also through the server's
 * services manager &mdash; the same mechanism the recorder already uses for its
 * other services). An addon retrieves it with {@link #get()}:
 *
 * <pre>{@code
 * Optional<ReplayCoreApi> api = ReplayCoreProvider.get();
 * if (!api.isPresent()) {
 *     getLogger().warning("ReplayCore is not installed; addon features disabled.");
 *     return;
 * }
 * }</pre>
 *
 * <p>Using a holder keeps the addon decoupled from how the recorder is wired: the
 * addon depends only on this SDK, never on the recorder's internals, and tolerates
 * the recorder being absent. {@link #get()} returns an empty optional whenever the
 * recorder has not registered, so an addon can guard its features cleanly.
 *
 * <p>Forward-looking contract: see the package documentation for status. The
 * {@link #set(ReplayCoreApi)} and {@link #clear()} methods are the recorder's
 * registration hooks, not for addon use.
 */
public final class ReplayCoreProvider {

    private static volatile ReplayCoreApi instance;

    private ReplayCoreProvider() {
    }

    /**
     * Returns the running recorder's API, if one has been registered.
     *
     * @return the API, or an empty optional if the recorder is absent or not yet
     *         enabled
     */
    public static Optional<ReplayCoreApi> get() {
        return Optional.ofNullable(instance);
    }

    /**
     * Registers the recorder's API implementation. Called by the recorder during
     * enable; not for addon use.
     *
     * @param api the implementation to publish; must not be {@code null}
     */
    public static void set(ReplayCoreApi api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        instance = api;
    }

    /**
     * Clears the registered API. Called by the recorder during disable; not for
     * addon use.
     */
    public static void clear() {
        instance = null;
    }
}
