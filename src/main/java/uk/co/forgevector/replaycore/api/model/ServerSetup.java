/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;

/**
 * A newly created pending server setup.
 */
public final class ServerSetup {

    private final String serverId;
    private final String name;

    /**
     * @param serverId the recorder server id/fingerprint
     * @param name     the cleaned display name
     */
    public ServerSetup(String serverId, String name) {
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.name = Objects.requireNonNull(name, "name");
    }

    /** @return the id to place in the recorder configuration */
    public String getServerId() {
        return serverId;
    }

    /** @return the cleaned server display name */
    public String getName() {
        return name;
    }
}
