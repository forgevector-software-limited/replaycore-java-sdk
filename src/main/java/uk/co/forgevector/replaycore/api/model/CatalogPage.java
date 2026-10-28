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
 * One page of catalogue results: collections, or the assets of a collection or
 * player.
 *
 * <p>The catalogue paginates with opaque cursors, exactly as {@link ReplayPage}
 * does. To fetch the next page, pass {@link #getNextPageToken()} back through
 * {@code CollectionQuery.nextPageOf}, {@code AssetQuery.nextPageOf} or
 * {@code PlayerReplayQuery.nextPageOf}. When {@link #hasNextPage()} is
 * {@code false}, the current page is the last one.
 *
 * <p>Results are ordered newest first. A held or revoked row the caller may not
 * see is never returned, never counted towards a page and never given a cursor
 * position.
 *
 * @param <T> the type of result on the page
 */
public final class CatalogPage<T> {

    private final List<T> results;
    private final String nextPageToken;
    private final int pageSize;

    /**
     * Constructs a page. Intended for internal deserialisation and tests.
     *
     * @param results       the results in this page; never {@code null}
     * @param nextPageToken the cursor for the next page, or {@code null}/empty if
     *                      this is the last page
     * @param pageSize      the page size the server applied
     */
    public CatalogPage(List<T> results, String nextPageToken, int pageSize) {
        this.results = results != null
                ? Collections.unmodifiableList(results)
                : Collections.<T>emptyList();
        this.nextPageToken = (nextPageToken != null && !nextPageToken.isEmpty()) ? nextPageToken : null;
        this.pageSize = pageSize;
    }

    /**
     * Returns the results in this page, newest first.
     *
     * @return an unmodifiable list of results; never {@code null}
     */
    public List<T> getResults() {
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
        if (!(o instanceof CatalogPage)) {
            return false;
        }
        CatalogPage<?> that = (CatalogPage<?>) o;
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
        return "CatalogPage{count=" + results.size()
                + ", pageSize=" + pageSize
                + ", hasNextPage=" + hasNextPage() + '}';
    }
}
