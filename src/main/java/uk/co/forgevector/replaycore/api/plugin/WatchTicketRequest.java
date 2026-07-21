/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;
import java.util.UUID;

/**
 * The request that mints a watch ticket with {@link ReplayCatalogApi#createWatchTicket}, mirroring
 * {@code POST /v1/replay-assets/{id}/watch-ticket}'s optional {@code viewer_player_uuid} body field
 * (RFC-0009 section 10).
 *
 * <h2>What {@link #viewerPlayerUuid()} can and cannot do</h2>
 * <p>This backend's own authenticated credentials (tenant, key scopes) decide release state, staff bypass,
 * and every visibility tier except {@code participants}. {@link #viewerPlayerUuid()} is consulted ONLY to
 * satisfy the narrowest tier, {@link ReplayVisibility#PARTICIPANTS}: it identifies which Minecraft player
 * the recorder is vouching for, resolved upstream by the integrating network's own authentication (a
 * website login, a linked Discord/Minecraft account) before this call is ever made. Supplying one can never
 * widen access beyond what this server's credentials already grant, and omitting it (or supplying one that
 * is not a participant of the asset's collection) is evaluated as a non-participant - strictly narrower than
 * omitting it would be for any other gate.
 *
 * <p>Build one with the fluent {@link #builder(String)}; {@code assetId} is the only required field.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class WatchTicketRequest {

    /** Maximum length (characters) of {@link #assetId()}; an over-length value is rejected, not truncated. */
    public static final int MAX_ASSET_ID_LENGTH = 128;

    private final String assetId;
    private final UUID viewerPlayerUuid;

    private WatchTicketRequest(Builder b) {
        this.assetId = requiredIdentifier(b.assetId, "assetId", MAX_ASSET_ID_LENGTH);
        this.viewerPlayerUuid = b.viewerPlayerUuid;
    }

    /**
     * Starts building a watch-ticket request for one asset.
     *
     * @param assetId the asset to mint a ticket for (required, non-blank, at most
     *                {@link #MAX_ASSET_ID_LENGTH} characters)
     * @return a new builder
     */
    public static Builder builder(String assetId) {
        return new Builder(assetId);
    }

    /** @return the asset to mint a ticket for; never {@code null} */
    public String assetId() { return assetId; }
    /** @return the Minecraft player identity this backend is vouching for, or an empty optional when none
     *          is known - see this type's documentation for exactly what this field can and cannot unlock */
    public Optional<UUID> viewerPlayerUuid() { return Optional.ofNullable(viewerPlayerUuid); }

    private static String requiredIdentifier(String value, String name, int maxLength) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " must be at most " + maxLength + " characters");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) < 0x20 || trimmed.charAt(i) == 0x7f) {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return trimmed;
    }

    /** A fluent builder for {@link WatchTicketRequest}. Not thread-safe; build one request per builder. */
    public static final class Builder {
        private final String assetId;
        private UUID viewerPlayerUuid;

        private Builder(String assetId) {
            this.assetId = assetId;
        }

        /**
         * Sets the Minecraft player identity this backend is vouching for. See this type's documentation
         * for exactly what this field can and cannot unlock.
         *
         * @param viewerPlayerUuid the player's id, or {@code null} for none
         * @return this builder
         */
        public Builder viewerPlayerUuid(UUID viewerPlayerUuid) {
            this.viewerPlayerUuid = viewerPlayerUuid;
            return this;
        }

        /**
         * Builds the immutable request, applying all field validation.
         *
         * @return a new {@link WatchTicketRequest}
         * @throws IllegalArgumentException if {@code assetId} is blank, too long, or contains a control
         *                                   character
         */
        public WatchTicketRequest build() {
            return new WatchTicketRequest(this);
        }
    }
}
