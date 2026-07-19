/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The durable outcome of a create, finalize or clip operation: what was produced, how far it got, and
 * where an integrator can find it.
 *
 * <p>Every asynchronous operation in the network-integration surface completes with a value shaped like
 * this one (see {@link FinalizeResult} for the type {@link ReplayCoreMatchApi#endScope} completes with).
 * The plugin receives this directly; delivering a link only as a chat message to a player is not an API
 * and does not satisfy the requirement this type exists for.
 *
 * <p>Results are durable: the backend persists each operation keyed by its idempotency key, so a result
 * survives a plugin restart and a repeated request with the same idempotency key returns the original
 * outcome instead of doing the work twice.
 *
 * <p>Build one with the fluent {@link #builder(String, String)} (an {@code operationId} and a
 * {@code collectionId} are required), or with the all-arguments constructor. {@link #assetId()},
 * {@link #failureCode()} and the two URL fields are validated as identifiers or bounded text and rejected
 * outright rather than truncated, for the same reason described on {@link ReplayParticipant#teamId()}: a
 * truncated id or URL is worse than a rejected one, because it can silently resolve to the wrong resource
 * instead of visibly failing.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ReplayOperationResult {

    /** Maximum length (characters) of {@link #operationId()}, {@link #collectionId()}, {@link #assetId()}
     *  and {@link #failureCode()}; over-length values are rejected, not truncated. */
    public static final int MAX_ID_LENGTH = 128;
    /** Maximum length (characters) of {@link #resourceUrl()} and {@link #watchUrl()}; over-length values
     *  are rejected, not truncated. */
    public static final int MAX_URL_LENGTH = 2048;

    private final String operationId;
    private final String collectionId;
    private final String assetId;
    private final ProcessingState processingState;
    private final String failureCode;
    private final boolean retryable;
    private final Instant playableAt;
    private final String resourceUrl;
    private final String watchUrl;
    private final boolean watchTicketEligible;

    /**
     * Creates an operation result directly. Prefer {@link #builder(String, String)} for readability; this
     * constructor exists for callers that already hold every field.
     *
     * @param operationId         the durable operation id (required, non-blank)
     * @param collectionId        the collection this operation produced or acted on (required, non-blank)
     * @param assetId             the specific asset this operation produced, or {@code null} if none yet
     *                            exists (for example a collection-level operation still in progress)
     * @param processingState     the operation's outcome processing state (required)
     * @param failureCode         a machine-readable failure code, or {@code null} when not
     *                            {@link ProcessingState#FAILED}
     * @param retryable           whether re-sending the same request may succeed after a transient failure
     * @param playableAt          the instant the result became (or will become) playable, or {@code null}
     *                            if not yet known
     * @param resourceUrl         the API resource URL for the produced collection or asset, or
     *                            {@code null} if not yet known
     * @param watchUrl            a ready-to-use browser watch URL, or {@code null} when only watch-ticket
     *                            eligibility is known
     * @param watchTicketEligible whether a watch ticket can currently be minted for this result (see
     *                            the watch-ticket endpoint); {@code false} while held, revoked or not ready
     */
    public ReplayOperationResult(String operationId, String collectionId, String assetId,
                                  ProcessingState processingState, String failureCode, boolean retryable,
                                  Instant playableAt, String resourceUrl, String watchUrl,
                                  boolean watchTicketEligible) {
        this.operationId = requiredIdentifier(operationId, "operationId", MAX_ID_LENGTH);
        this.collectionId = requiredIdentifier(collectionId, "collectionId", MAX_ID_LENGTH);
        this.assetId = assetId == null ? null : requiredIdentifier(assetId, "assetId", MAX_ID_LENGTH);
        this.processingState = Objects.requireNonNull(processingState, "processingState must not be null");
        this.failureCode = failureCode == null ? null : requiredIdentifier(failureCode, "failureCode", MAX_ID_LENGTH);
        this.retryable = retryable;
        this.playableAt = playableAt;
        this.resourceUrl = resourceUrl == null ? null : requiredIdentifier(resourceUrl, "resourceUrl", MAX_URL_LENGTH);
        this.watchUrl = watchUrl == null ? null : requiredIdentifier(watchUrl, "watchUrl", MAX_URL_LENGTH);
        this.watchTicketEligible = watchTicketEligible;
    }

    /**
     * Starts building a result with the two required fields.
     *
     * @param operationId  the durable operation id (required, non-blank)
     * @param collectionId the collection this operation produced or acted on (required, non-blank)
     * @return a new builder
     */
    public static Builder builder(String operationId, String collectionId) {
        return new Builder(operationId, collectionId);
    }

    /** @return the durable operation id; never {@code null} */
    public String operationId() { return operationId; }
    /** @return the collection this operation produced or acted on; never {@code null} */
    public String collectionId() { return collectionId; }
    /** @return the specific asset this operation produced, or an empty optional if none yet exists */
    public Optional<String> assetId() { return Optional.ofNullable(assetId); }
    /** @return the operation's outcome processing state; never {@code null} */
    public ProcessingState processingState() { return processingState; }
    /** @return a machine-readable failure code, or an empty optional when not failed */
    public Optional<String> failureCode() { return Optional.ofNullable(failureCode); }
    /** @return whether re-sending the same request may succeed after a transient failure */
    public boolean retryable() { return retryable; }
    /** @return the instant the result became or will become playable, or an empty optional if not yet known */
    public Optional<Instant> playableAt() { return Optional.ofNullable(playableAt); }
    /** @return the API resource URL, or an empty optional if not yet known */
    public Optional<String> resourceUrl() { return Optional.ofNullable(resourceUrl); }
    /** @return a ready-to-use browser watch URL, or an empty optional when only watch-ticket eligibility is known */
    public Optional<String> watchUrl() { return Optional.ofNullable(watchUrl); }
    /** @return whether a watch ticket can currently be minted for this result */
    public boolean watchTicketEligible() { return watchTicketEligible; }

    private static String requiredIdentifier(String value, String name, int maxLength) {
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

    /** A fluent builder for {@link ReplayOperationResult}. Not thread-safe; build one result per builder. */
    public static final class Builder {
        private final String operationId;
        private final String collectionId;
        private String assetId;
        private ProcessingState processingState = ProcessingState.PROCESSING;
        private String failureCode;
        private boolean retryable;
        private Instant playableAt;
        private String resourceUrl;
        private String watchUrl;
        private boolean watchTicketEligible;

        private Builder(String operationId, String collectionId) {
            this.operationId = operationId;
            this.collectionId = collectionId;
        }

        /**
         * Sets the specific asset this operation produced.
         *
         * @param assetId the asset id, or {@code null} to clear
         * @return this builder
         */
        public Builder assetId(String assetId) {
            this.assetId = assetId;
            return this;
        }

        /**
         * Sets the operation's outcome processing state (defaults to {@link ProcessingState#PROCESSING}).
         *
         * @param processingState the processing state; must not be {@code null}
         * @return this builder
         */
        public Builder processingState(ProcessingState processingState) {
            this.processingState = processingState;
            return this;
        }

        /**
         * Sets a machine-readable failure code.
         *
         * @param failureCode the failure code, or {@code null} to clear
         * @return this builder
         */
        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        /**
         * Sets whether re-sending the same request may succeed after a transient failure.
         *
         * @param retryable {@code true} if a retry may succeed
         * @return this builder
         */
        public Builder retryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        /**
         * Sets the instant the result became or will become playable.
         *
         * @param playableAt the instant, or {@code null} to clear
         * @return this builder
         */
        public Builder playableAt(Instant playableAt) {
            this.playableAt = playableAt;
            return this;
        }

        /**
         * Sets the API resource URL.
         *
         * @param resourceUrl the URL, or {@code null} to clear
         * @return this builder
         */
        public Builder resourceUrl(String resourceUrl) {
            this.resourceUrl = resourceUrl;
            return this;
        }

        /**
         * Sets a ready-to-use browser watch URL.
         *
         * @param watchUrl the URL, or {@code null} to clear
         * @return this builder
         */
        public Builder watchUrl(String watchUrl) {
            this.watchUrl = watchUrl;
            return this;
        }

        /**
         * Sets whether a watch ticket can currently be minted for this result.
         *
         * @param watchTicketEligible {@code true} if a watch ticket can currently be minted
         * @return this builder
         */
        public Builder watchTicketEligible(boolean watchTicketEligible) {
            this.watchTicketEligible = watchTicketEligible;
            return this;
        }

        /**
         * Builds the immutable result, applying all field validation.
         *
         * @return a new {@link ReplayOperationResult}
         * @throws IllegalArgumentException if a required field is blank, an identifier or URL field is too
         *                                   long or contains a control character, or {@code processingState}
         *                                   is {@code null}
         */
        public ReplayOperationResult build() {
            return new ReplayOperationResult(operationId, collectionId, assetId, processingState,
                    failureCode, retryable, playableAt, resourceUrl, watchUrl, watchTicketEligible);
        }
    }
}
