/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * Opaque, recording-scoped handle for one caller-registered player-shaped subject.
 *
 * <p>Only ReplayCore creates valid handles. Addons must retain the returned instance and pass that exact
 * instance back to the publication methods. Implementing this interface, serialising a handle, or using a
 * handle with another scope or recorder instance never grants access to a subject.
 *
 * <p>A handle becomes permanently invalid when its subject is unregistered, its scope ends, or the recorder
 * stops. It deliberately exposes no identity fields: external correlation belongs to
 * {@link ExternalSubjectDescriptor#subjectId()}, while the archive identity remains private to the
 * recording.
 */
public interface ExternalSubjectHandle {
}
