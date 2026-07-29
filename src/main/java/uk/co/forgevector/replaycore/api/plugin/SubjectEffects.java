/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Complete immutable active-effect snapshot for a player-shaped subject. */
public final class SubjectEffects {
    public static final int MAX_EFFECTS = 64;
    private static final SubjectEffects EMPTY = new SubjectEffects(Collections.<ExternalSubjectEffect>emptyList());

    private final List<ExternalSubjectEffect> values;

    public SubjectEffects(Collection<ExternalSubjectEffect> source) {
        if (source != null && source.size() > MAX_EFFECTS) {
            throw new IllegalArgumentException("effects must contain at most " + MAX_EFFECTS + " entries");
        }
        ArrayList<ExternalSubjectEffect> copy = new ArrayList<ExternalSubjectEffect>();
        if (source != null) {
            for (ExternalSubjectEffect effect : source) {
                if (effect == null) throw new IllegalArgumentException("effects must not contain null entries");
                copy.add(effect);
            }
        }
        this.values = Collections.unmodifiableList(copy);
    }

    public static SubjectEffects empty() { return EMPTY; }
    public List<ExternalSubjectEffect> values() { return values; }
}
