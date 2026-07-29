/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/** Immutable durable lookup result for one integration-key/external-recording-id identity. */
public final class ReplayResult {
    private final String localCollectionId;
    private final String cloudCollectionId;
    private final String integrationKey;
    private final String externalRecordingId;
    private final ReplayState state;
    private final URI watchUrl;
    private final String failureCode;
    private final boolean retryable;
    private final String operationId;
    private final String assetId;
    private final ProcessingState processingState;
    private final Instant playableAt;
    private final URI resourceUrl;
    private final boolean watchTicketEligible;

    public ReplayResult(String localCollectionId, String cloudCollectionId, String integrationKey,
                        String externalRecordingId, ReplayState state, URI watchUrl,
                        String failureCode, boolean retryable) {
        this(localCollectionId, cloudCollectionId, integrationKey, externalRecordingId, state,
                watchUrl, failureCode, retryable, null, null, processingFor(state), null, null, false);
    }

    /**
     * Complete durable result shape. The shorter constructor remains source-compatible for integrations
     * that only need lifecycle and watch identity.
     */
    public ReplayResult(String localCollectionId, String cloudCollectionId, String integrationKey,
                        String externalRecordingId, ReplayState state, URI watchUrl,
                        String failureCode, boolean retryable, String operationId, String assetId,
                        ProcessingState processingState, Instant playableAt, URI resourceUrl,
                        boolean watchTicketEligible) {
        this.localCollectionId = optional(localCollectionId, "localCollectionId", 128);
        this.cloudCollectionId = optional(cloudCollectionId, "cloudCollectionId", 128);
        this.integrationKey = required(integrationKey, "integrationKey", 64);
        this.externalRecordingId = required(externalRecordingId, "externalRecordingId", 128);
        if (state == null) throw new IllegalArgumentException("state must not be null");
        this.state = state;
        this.watchUrl = watchUrl;
        this.failureCode = optional(failureCode, "failureCode", 128);
        this.retryable = retryable;
        this.operationId = optional(operationId, "operationId", 128);
        this.assetId = optional(assetId, "assetId", 128);
        if (processingState == null) throw new IllegalArgumentException("processingState must not be null");
        this.processingState = processingState;
        this.playableAt = playableAt;
        this.resourceUrl = resourceUrl;
        this.watchTicketEligible = watchTicketEligible;
    }

    /** May be null before the local scope has been durably opened. */
    public String localCollectionId() { return localCollectionId; }
    /** May be null until cloud reconciliation creates the collection. */
    public String cloudCollectionId() { return cloudCollectionId; }
    public String integrationKey() { return integrationKey; }
    public String externalRecordingId() { return externalRecordingId; }
    public ReplayState state() { return state; }
    /** May be null until the result reaches LINK_READY or PLAYABLE. */
    public URI watchUrl() { return watchUrl; }
    /** May be null when no failure applies. */
    public String failureCode() { return failureCode; }
    public boolean retryable() { return retryable; }
    /** Stable operation id assigned by the durable finalisation outbox, or null while still recording. */
    public String operationId() { return operationId; }
    /** Full-match asset id, or null until the cloud has created it. */
    public String assetId() { return assetId; }
    /** Exact collection/asset processing state reported by the finalisation operation. */
    public ProcessingState processingState() { return processingState; }
    /** When playable processing completed, or null until known. */
    public Instant playableAt() { return playableAt; }
    /** Cloud resource URI for the collection/result, or null until known. */
    public URI resourceUrl() { return resourceUrl; }
    public boolean watchTicketEligible() { return watchTicketEligible; }

    public Optional<String> optionalLocalCollectionId() { return Optional.ofNullable(localCollectionId); }
    public Optional<String> optionalCloudCollectionId() { return Optional.ofNullable(cloudCollectionId); }
    public Optional<URI> optionalWatchUrl() { return Optional.ofNullable(watchUrl); }
    public Optional<String> optionalFailureCode() { return Optional.ofNullable(failureCode); }
    public Optional<String> optionalOperationId() { return Optional.ofNullable(operationId); }
    public Optional<String> optionalAssetId() { return Optional.ofNullable(assetId); }
    public Optional<Instant> optionalPlayableAt() { return Optional.ofNullable(playableAt); }
    public Optional<URI> optionalResourceUrl() { return Optional.ofNullable(resourceUrl); }
    public boolean terminal() {
        return state == ReplayState.PLAYABLE || state == ReplayState.FAILED
                || state == ReplayState.EXPIRED || state == ReplayState.DELETED;
    }

    private static String required(String value, String name, int maximum) {
        String result = optional(value, name, maximum);
        if (result == null) throw new IllegalArgumentException(name + " must not be empty");
        return result;
    }

    private static ProcessingState processingFor(ReplayState state) {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        switch (state) {
            case RECORDING: return ProcessingState.RECORDING;
            case PLAYABLE: return ProcessingState.READY;
            case FAILED: return ProcessingState.FAILED;
            case EXPIRED: return ProcessingState.EXPIRED;
            case DELETED: return ProcessingState.DELETED;
            default: return ProcessingState.PROCESSING;
        }
    }

    private static String optional(String value, String name, int maximum) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > maximum) throw new IllegalArgumentException(name + " is too long");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) throw new IllegalArgumentException(name + " contains a control character");
        }
        return trimmed;
    }
}
