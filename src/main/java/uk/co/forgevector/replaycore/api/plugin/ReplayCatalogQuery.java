/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * The facets and pagination applied to {@link ReplayCatalogApi#listForPlayer}, mirroring the query
 * parameters of {@code GET /v1/players/{uuid}/replays} (RFC-0009 section 9): {@link #artifactKind()} is
 * {@code asset_kind}, {@link #viewerRelation()} is {@code viewer_relation}, {@link #category()} and
 * {@link #mode()} together are the match's extensible "game kind", {@link #parentEvent()} is
 * {@code party_event_id}, and {@link #processingState()} is {@code processing_state}.
 *
 * <p>Every field is optional; a caller sets only what it wants to narrow by. Build one with the fluent
 * {@link #builder()}; every field left unset omits the corresponding query parameter entirely, matching the
 * cloud's own "no filter on that condition" default.
 *
 * <p>{@link #category()}, {@link #mode()} and {@link #parentEvent()} are bounded and character-class folded
 * exactly like {@link BeginScopeRequest}'s own {@code category}/{@code mode} fields: trimmed, control
 * characters stripped, capped at {@link #MAX_FIELD_LENGTH} characters, and any character outside
 * {@code [A-Za-z0-9_.:-]} replaced with {@code '_'}. {@link #limit()} is clamped to
 * {@code [}{@link #MIN_LIMIT}{@code , }{@link #MAX_LIMIT}{@code ]} rather than rejected, matching
 * {@link ScopeClipRequest}'s own pre-roll/post-roll clamping: an out-of-range page size is a caller asking
 * for more or less than the catalogue allows, not a programmer error. {@link #cursor()} is the one field
 * that is rejected rather than truncated or clamped when over-length: it is an opaque token the cloud
 * minted, so a truncated cursor is not a smaller valid cursor but a corrupt one.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ReplayCatalogQuery {

    /** Minimum accepted {@link #limit()}; a supplied value below this is clamped up. */
    public static final int MIN_LIMIT = 1;
    /** Maximum accepted {@link #limit()}, matching the cloud catalogue's own page-size ceiling; a supplied
     *  value above this is clamped down. */
    public static final int MAX_LIMIT = 100;
    /** Maximum length (characters) of {@link #category()}, {@link #mode()} and {@link #parentEvent()};
     *  over-length values are truncated. */
    public static final int MAX_FIELD_LENGTH = 128;
    /** Maximum length (characters) of {@link #cursor()}; an over-length value is rejected, not truncated. */
    public static final int MAX_CURSOR_LENGTH = 4096;

    private final AssetKind artifactKind;
    private final String collectionId;
    private final String externalMatchId;
    private final AssetRelationship viewerRelation;
    private final String category;
    private final String mode;
    private final String parentEvent;
    private final ProcessingState processingState;
    private final Integer limit;
    private final String cursor;

    private ReplayCatalogQuery(Builder b) {
        this.collectionId = b.collectionId;
        this.externalMatchId = b.externalMatchId;
        this.artifactKind = b.artifactKind;
        this.viewerRelation = b.viewerRelation;
        this.category = b.category == null || b.category.trim().isEmpty() ? null : foldedField(b.category);
        this.mode = b.mode == null || b.mode.trim().isEmpty() ? null : foldedField(b.mode);
        this.parentEvent = b.parentEvent == null || b.parentEvent.trim().isEmpty() ? null : foldedField(b.parentEvent);
        this.processingState = b.processingState;
        this.limit = b.limit == null ? null : Integer.valueOf(clampLimit(b.limit.intValue()));
        this.cursor = b.cursor == null || b.cursor.trim().isEmpty() ? null : requiredCursor(b.cursor);
    }

    /** @return a new builder with every facet unset */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the {@code asset_kind} facet, or {@code null} for no filter */
    public AssetKind artifactKind() { return artifactKind; }
    public String collectionId() { return collectionId; }
    public String externalMatchId() { return externalMatchId; }
    /** @return the {@code viewer_relation} facet, or {@code null} for no filter */
    public AssetRelationship viewerRelation() { return viewerRelation; }
    /** @return the extensible category facet, or {@code null} for no filter */
    public String category() { return category; }
    /** @return the extensible mode facet, or {@code null} for no filter */
    public String mode() { return mode; }
    /** @return the {@code party_event_id} facet, or {@code null} for no filter */
    public String parentEvent() { return parentEvent; }
    /** @return the {@code processing_state} facet, or {@code null} for no filter */
    public ProcessingState processingState() { return processingState; }
    /** @return the requested page size, clamped to {@code [}{@link #MIN_LIMIT}{@code , }{@link #MAX_LIMIT}{@code ]},
     *          or {@code null} to let the cloud apply its own default */
    public Integer limit() { return limit; }
    /** @return the opaque pagination cursor from a previous page's {@link ReplayCatalogPage#nextCursor()},
     *          or {@code null} to request the first page */
    public String cursor() { return cursor; }

    private static int clampLimit(int value) {
        if (value < MIN_LIMIT) return MIN_LIMIT;
        if (value > MAX_LIMIT) return MAX_LIMIT;
        return value;
    }

    private static String requiredCursor(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > MAX_CURSOR_LENGTH) {
            throw new IllegalArgumentException("cursor must be at most " + MAX_CURSOR_LENGTH + " characters");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new IllegalArgumentException("cursor must not contain control characters");
            }
        }
        return trimmed;
    }

    private static String foldedField(String value) {
        String stripped = stripControl(value.trim());
        String bounded = stripped.length() <= MAX_FIELD_LENGTH ? stripped : stripped.substring(0, MAX_FIELD_LENGTH);
        StringBuilder out = new StringBuilder(bounded.length());
        for (int i = 0; i < bounded.length(); i++) {
            char c = bounded.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == ':' || c == '-';
            out.append(allowed ? c : '_');
        }
        return out.toString();
    }

    private static String stripControl(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 0x20 && c != 0x7f) || c == '\t') out.append(c);
        }
        return out.toString();
    }

    /** A fluent builder for {@link ReplayCatalogQuery}. Not thread-safe; build one query per builder. */
    public static final class Builder {
        private String collectionId;
        private String externalMatchId;

        /** Exact cloud collection identity. No local scope id is accepted. */
        public Builder collectionId(String value) {
            if (value != null && !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
                throw new IllegalArgumentException("collectionId must be a UUID");
            this.collectionId = value; return this;
        }
        /** Exact external match identity, mapped from externalRecordingId. */
        public Builder externalMatchId(String value) {
            if (value != null && (value.isEmpty() || value.length() > MAX_FIELD_LENGTH || !value.matches("[A-Za-z0-9_.:-]+")))
                throw new IllegalArgumentException("externalMatchId must be a bounded exact identity");
            this.externalMatchId = value; return this;
        }
        private AssetKind artifactKind;
        private AssetRelationship viewerRelation;
        private String category;
        private String mode;
        private String parentEvent;
        private ProcessingState processingState;
        private Integer limit;
        private String cursor;

        private Builder() {
        }

        /**
         * Narrows to one {@code asset_kind}.
         *
         * @param artifactKind the asset kind to filter on, or {@code null} to clear
         * @return this builder
         */
        public Builder artifactKind(AssetKind artifactKind) {
            this.artifactKind = artifactKind;
            return this;
        }

        /**
         * Narrows to one {@code viewer_relation} - how {@code viewer} relates to the returned assets.
         *
         * @param viewerRelation the relation to filter on, or {@code null} to clear
         * @return this builder
         */
        public Builder viewerRelation(AssetRelationship viewerRelation) {
            this.viewerRelation = viewerRelation;
            return this;
        }

        /**
         * Narrows to one extensible category.
         *
         * @param category the category to filter on, or {@code null} to clear
         * @return this builder
         */
        public Builder category(String category) {
            this.category = category;
            return this;
        }

        /**
         * Narrows to one extensible mode.
         *
         * @param mode the mode to filter on, or {@code null} to clear
         * @return this builder
         */
        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Narrows to one {@code party_event_id} (competition hierarchy, RFC-0009 section 2.5).
         *
         * @param parentEvent the party/event id to filter on, or {@code null} to clear
         * @return this builder
         */
        public Builder parentEvent(String parentEvent) {
            this.parentEvent = parentEvent;
            return this;
        }

        /**
         * Narrows to one {@code processing_state}.
         *
         * @param processingState the processing state to filter on, or {@code null} to clear
         * @return this builder
         */
        public Builder processingState(ProcessingState processingState) {
            this.processingState = processingState;
            return this;
        }

        /**
         * Sets the requested page size, clamped to {@code [}{@link #MIN_LIMIT}{@code , }{@link #MAX_LIMIT}{@code ]}
         * on {@link #build()}.
         *
         * @param limit the requested page size
         * @return this builder
         */
        public Builder limit(int limit) {
            this.limit = Integer.valueOf(limit);
            return this;
        }

        /**
         * Sets the pagination cursor from a previous page's {@link ReplayCatalogPage#nextCursor()}.
         *
         * @param cursor the cursor, or {@code null} to request the first page
         * @return this builder
         */
        public Builder cursor(String cursor) {
            this.cursor = cursor;
            return this;
        }

        /**
         * Builds the immutable query, applying all field bounds.
         *
         * @return a new {@link ReplayCatalogQuery}
         * @throws IllegalArgumentException if {@code cursor} is set and exceeds {@link #MAX_CURSOR_LENGTH}
         *                                   characters or contains a control character
         */
        public ReplayCatalogQuery build() {
            return new ReplayCatalogQuery(this);
        }
    }
}
