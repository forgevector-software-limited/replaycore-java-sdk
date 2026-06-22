/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One page of replay results from {@code listReplays}.
 *
 * <p>ReplayCore paginates with opaque cursors, not offsets. To fetch the next
 * page, pass {@link #getNextPageToken()} back into a new
 * {@link ReplayQuery} via {@link ReplayQuery.Builder#pageToken(String)}. When
 * {@link #hasNextPage()} is {@code false}, the current page is the last one.
 *
 * <p>Results are ordered newest-first (by start time descending, then id).
 */
public final class ReplayPage {

    private final List<ReplayMetadata> results;
    private final String nextPageToken;
    private final int pageSize;

    /**
     * Constructs a page. Intended for internal deserialisation and tests.
     *
     * @param results       the replays in this page; never {@code null}
     * @param nextPageToken the cursor for the next page, or {@code null}/empty if
     *                      this is the last page
     * @param pageSize      the page size the server applied
     */
    public ReplayPage(List<ReplayMetadata> results, String nextPageToken, int pageSize) {
        this.results = results != null
                ? Collections.unmodifiableList(results)
                : Collections.<ReplayMetadata>emptyList();
        this.nextPageToken = (nextPageToken != null && !nextPageToken.isEmpty()) ? nextPageToken : null;
        this.pageSize = pageSize;
    }

    /**
     * Returns the replays in this page, newest first.
     *
     * @return an unmodifiable list of replay metadata; never {@code null}
     */
    public List<ReplayMetadata> getResults() {
        return results;
    }

    /**
     * Returns the cursor used to fetch the next page, if any.
     *
     * @return the next page token, or an empty optional on the last page
     */
    public Optional<String> getNextPageToken() {
        return Optional.ofNullable(nextPageToken);
    }

    /**
     * Returns whether a further page is available.
     *
     * @return {@code true} if {@link #getNextPageToken()} is present
     */
    public boolean hasNextPage() {
        return nextPageToken != null;
    }

    /**
     * Returns the page size the server applied to this request.
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReplayPage)) {
            return false;
        }
        ReplayPage that = (ReplayPage) o;
        return pageSize == that.pageSize
                && results.equals(that.results)
                && Objects.equals(nextPageToken, that.nextPageToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, nextPageToken, pageSize);
    }

    @Override
    public String toString() {
        return "ReplayPage{count=" + results.size()
                + ", pageSize=" + pageSize
                + ", hasNextPage=" + hasNextPage() + '}';
    }
}
