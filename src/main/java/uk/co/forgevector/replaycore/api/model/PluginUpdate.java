/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The plugin-update advice the cloud computes for one connected server.
 *
 * Immutable.
 */
public final class PluginUpdate {

    private final PluginUpdateStatus status;
    private final String minimumSupportedVersion;
    private final String latestClassicVersion;
    private final boolean loaderAvailable;
    private final String loaderDownloadUrl;

    /**
     * Constructs a plugin update. Intended for internal deserialisation and tests.
     *
     * @param status whether the server's plugin needs an update
     * @param minimumSupportedVersion the oldest plugin version the cloud still supports
     * @param latestClassicVersion the newest classic plugin version
     * @param loaderAvailable whether the Loader is available to migrate to
     * @param loaderDownloadUrl where the Loader can be downloaded
     */
    public PluginUpdate(
            PluginUpdateStatus status,
            String minimumSupportedVersion,
            String latestClassicVersion,
            boolean loaderAvailable,
            String loaderDownloadUrl) {
        this.status = status != null ? status : PluginUpdateStatus.UNKNOWN;
        this.minimumSupportedVersion = minimumSupportedVersion != null ? minimumSupportedVersion : "";
        this.latestClassicVersion = latestClassicVersion != null ? latestClassicVersion : "";
        this.loaderAvailable = loaderAvailable;
        this.loaderDownloadUrl = loaderDownloadUrl;
    }

    /**
     * Returns whether the server's plugin needs an update.
     *
     * @return whether the server's plugin needs an update
     */
    public PluginUpdateStatus getStatus() {
        return status;
    }

    /**
     * Returns the oldest plugin version the cloud still supports.
     *
     * @return the oldest plugin version the cloud still supports; never {@code null}
     */
    public String getMinimumSupportedVersion() {
        return minimumSupportedVersion;
    }

    /**
     * Returns the newest classic plugin version.
     *
     * @return the newest classic plugin version; never {@code null}
     */
    public String getLatestClassicVersion() {
        return latestClassicVersion;
    }

    /**
     * Returns whether the Loader is available to migrate to.
     *
     * @return whether the Loader is available to migrate to
     */
    public boolean isLoaderAvailable() {
        return loaderAvailable;
    }

    /**
     * Returns where the Loader can be downloaded.
     *
     * @return where the Loader can be downloaded, or an empty optional
     */
    public Optional<String> getLoaderDownloadUrl() {
        return Optional.ofNullable(loaderDownloadUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PluginUpdate)) {
            return false;
        }
        PluginUpdate that = (PluginUpdate) o;
        return Objects.equals(status, that.status)
                && Objects.equals(minimumSupportedVersion, that.minimumSupportedVersion)
                && Objects.equals(latestClassicVersion, that.latestClassicVersion)
                && loaderAvailable == that.loaderAvailable
                && Objects.equals(loaderDownloadUrl, that.loaderDownloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, minimumSupportedVersion, latestClassicVersion, loaderAvailable, loaderDownloadUrl);
    }

    @Override
    public String toString() {
        return "PluginUpdate{status=" + status
                + ", latestClassicVersion=" + latestClassicVersion + '}';
    }
}
