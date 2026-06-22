/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A single player who took part in a recording.
 *
 * <p>Immutable. Mirrors an element of the cloud's {@code participants} array. The
 * {@code uuid} is always present; the last-known display name and an optional
 * free-form role (for example {@code "player"} or {@code "spectator"}) may be
 * absent.
 */
public final class Participant {

    private final String uuid;
    private final String name;
    private final String role;

    /**
     * Constructs a participant. Intended for internal deserialisation and tests;
     * callers normally receive instances from {@link ReplayMetadata}.
     *
     * @param uuid the player's UUID; never {@code null}
     * @param name the last-known display name, or {@code null} if unknown
     * @param role an optional free-form role label, or {@code null}
     */
    public Participant(String uuid, String name, String role) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name;
        this.role = role;
    }

    /**
     * Returns the player's UUID (a v4 UUID string).
     *
     * @return the UUID; never {@code null}
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Returns the player's last-known display name, if the recorder captured one.
     *
     * @return the name, or an empty optional
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the participant's role label, if one was assigned.
     *
     * @return the role, or an empty optional
     */
    public Optional<String> getRole() {
        return Optional.ofNullable(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Participant)) {
            return false;
        }
        Participant that = (Participant) o;
        return uuid.equals(that.uuid)
                && Objects.equals(name, that.name)
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, role);
    }

    @Override
    public String toString() {
        return "Participant{uuid=" + uuid + ", name=" + name + ", role=" + role + '}';
    }
}
