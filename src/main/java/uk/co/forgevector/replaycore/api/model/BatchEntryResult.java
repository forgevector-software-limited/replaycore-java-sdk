/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of one entry in a batch write.
 *
 * Immutable. Results are positional: result {@code n} describes entry {@code n} of the request.
 */
public final class BatchEntryResult {

    private final BatchEntryStatus status;
    private final String code;
    private final String detail;

    /**
     * Constructs a batch entry result. Intended for internal deserialisation and tests.
     *
     * @param status the entry's outcome
     * @param code the code explaining a rejected or retryable entry
     * @param detail a human-readable explanation of a rejected or retryable entry
     */
    public BatchEntryResult(BatchEntryStatus status, String code, String detail) {
        this.status = status != null ? status : BatchEntryStatus.UNKNOWN;
        this.code = code;
        this.detail = detail;
    }

    /**
     * Returns the entry's outcome.
     *
     * @return the entry's outcome
     */
    public BatchEntryStatus getStatus() {
        return status;
    }

    /**
     * Returns the code explaining a rejected or retryable entry.
     *
     * @return the code explaining a rejected or retryable entry, or an empty optional
     */
    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }

    /**
     * Returns a human-readable explanation of a rejected or retryable entry.
     *
     * @return a human-readable explanation of a rejected or retryable entry, or an empty optional
     */
    public Optional<String> getDetail() {
        return Optional.ofNullable(detail);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BatchEntryResult)) {
            return false;
        }
        BatchEntryResult that = (BatchEntryResult) o;
        return Objects.equals(status, that.status)
                && Objects.equals(code, that.code)
                && Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, code, detail);
    }

    @Override
    public String toString() {
        return "BatchEntryResult{status=" + status
                + ", code=" + code + '}';
    }
}
