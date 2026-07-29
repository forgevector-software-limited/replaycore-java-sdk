/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable scope-finalisation result, including durable external-recording correlation when applicable. */
public final class FinalizeResult {
    public static final int MAX_ID_LENGTH = ReplayOperationResult.MAX_ID_LENGTH;
    public static final int MAX_URL_LENGTH = ReplayOperationResult.MAX_URL_LENGTH;

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
    private final String localCollectionId;
    private final String cloudCollectionId;
    private final String integrationKey;
    private final String externalRecordingId;
    private final ReplayState state;

    /** Existing match-scope constructor. External-correlation fields are derived where possible. */
    public FinalizeResult(String operationId, String collectionId, String assetId,
                          ProcessingState processingState, String failureCode, boolean retryable,
                          Instant playableAt, String resourceUrl, String watchUrl,
                          boolean watchTicketEligible) {
        this(operationId, collectionId, assetId, processingState, failureCode, retryable,
                playableAt, resourceUrl, watchUrl, watchTicketEligible, collectionId,
                null, null, null, ReplayState.fromProcessingState(processingState));
    }

    private FinalizeResult(String operationId, String collectionId, String assetId,
                           ProcessingState processingState, String failureCode, boolean retryable,
                           Instant playableAt, String resourceUrl, String watchUrl,
                           boolean watchTicketEligible, String localCollectionId,
                           String cloudCollectionId, String integrationKey,
                           String externalRecordingId, ReplayState state) {
        this.operationId = requiredIdentifier(operationId, "operationId", MAX_ID_LENGTH);
        this.collectionId = requiredIdentifier(collectionId, "collectionId", MAX_ID_LENGTH);
        this.assetId = optionalIdentifier(assetId, "assetId", MAX_ID_LENGTH);
        this.processingState = Objects.requireNonNull(processingState, "processingState must not be null");
        this.failureCode = optionalIdentifier(failureCode, "failureCode", MAX_ID_LENGTH);
        this.retryable = retryable;
        this.playableAt = playableAt;
        this.resourceUrl = optionalIdentifier(resourceUrl, "resourceUrl", MAX_URL_LENGTH);
        this.watchUrl = optionalIdentifier(watchUrl, "watchUrl", MAX_URL_LENGTH);
        this.watchTicketEligible = watchTicketEligible;
        this.localCollectionId = optionalIdentifier(localCollectionId, "localCollectionId", MAX_ID_LENGTH);
        this.cloudCollectionId = optionalIdentifier(cloudCollectionId, "cloudCollectionId", MAX_ID_LENGTH);
        this.integrationKey = optionalIdentifier(integrationKey, "integrationKey", MAX_ID_LENGTH);
        this.externalRecordingId = optionalIdentifier(externalRecordingId, "externalRecordingId", MAX_ID_LENGTH);
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    public static Builder builder(String operationId, String collectionId) {
        return new Builder(operationId, collectionId);
    }

    public String operationId() { return operationId; }
    public String collectionId() { return collectionId; }
    public Optional<String> assetId() { return Optional.ofNullable(assetId); }
    /** Compatibility view for established match/catalogue callers. */
    public ProcessingState processingState() { return processingState; }
    public Optional<String> failureCode() { return Optional.ofNullable(failureCode); }
    public boolean retryable() { return retryable; }
    public Optional<Instant> playableAt() { return Optional.ofNullable(playableAt); }
    public Optional<String> resourceUrl() { return Optional.ofNullable(resourceUrl); }
    public Optional<String> watchUrl() { return Optional.ofNullable(watchUrl); }
    public boolean watchTicketEligible() { return watchTicketEligible; }

    /** May be null only on legacy results created outside the external-recording surface. */
    public String localCollectionId() { return localCollectionId; }
    /** May be null until the cloud collection has been reconciled. */
    public String cloudCollectionId() { return cloudCollectionId; }
    /** Non-null for every result opened through the external-recording surface. */
    public String integrationKey() { return integrationKey; }
    /** Non-null for every result opened through the external-recording surface. */
    public String externalRecordingId() { return externalRecordingId; }
    public ReplayState state() { return state; }


    private static String requiredIdentifier(String value, String name, int maximum) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (trimmed.length() > maximum) {
            throw new IllegalArgumentException(name + " must be at most " + maximum + " characters");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return trimmed;
    }

    private static String optionalIdentifier(String value, String name, int maximum) {
        return value == null ? null : requiredIdentifier(value, name, maximum);
    }

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
        private String localCollectionId;
        private String cloudCollectionId;
        private String integrationKey;
        private String externalRecordingId;
        private ReplayState state;

        private Builder(String operationId, String collectionId) {
            this.operationId = operationId;
            this.collectionId = collectionId;
            this.localCollectionId = collectionId;
        }

        public Builder assetId(String value) { this.assetId = value; return this; }
        public Builder processingState(ProcessingState value) { this.processingState = value; return this; }
        public Builder failureCode(String value) { this.failureCode = value; return this; }
        public Builder retryable(boolean value) { this.retryable = value; return this; }
        public Builder playableAt(Instant value) { this.playableAt = value; return this; }
        public Builder resourceUrl(String value) { this.resourceUrl = value; return this; }
        public Builder watchUrl(String value) { this.watchUrl = value; return this; }
        public Builder watchTicketEligible(boolean value) { this.watchTicketEligible = value; return this; }
        public Builder localCollectionId(String value) { this.localCollectionId = value; return this; }
        public Builder cloudCollectionId(String value) { this.cloudCollectionId = value; return this; }
        public Builder integrationKey(String value) { this.integrationKey = value; return this; }
        public Builder externalRecordingId(String value) { this.externalRecordingId = value; return this; }
        public Builder state(ReplayState value) { this.state = value; return this; }

        public FinalizeResult build() {
            ReplayState resolvedState = state == null
                    ? ReplayState.fromProcessingState(processingState) : state;
            return new FinalizeResult(operationId, collectionId, assetId, processingState,
                    failureCode, retryable, playableAt, resourceUrl, watchUrl,
                    watchTicketEligible, localCollectionId, cloudCollectionId,
                    integrationKey, externalRecordingId, resolvedState);
        }
    }
}
