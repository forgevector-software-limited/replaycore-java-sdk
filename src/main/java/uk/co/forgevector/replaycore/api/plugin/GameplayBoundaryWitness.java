/* Copyright (c) ForgeVector Software Limited. Author: ForgeVector Software Limited. */
package uk.co.forgevector.replaycore.api.plugin;

/** Immutable recorder-issued gameplay cursor. Preserve this exact object through delayed admission.
 * START includes the next captured local frame N. END excludes N and includes N-1. It certifies
 * capture ordering, not world/region simultaneity or durable storage. Unavailable witnesses have no ticks.
 * Public factories do not confer authority: the recorder validates its own bounded issuance record.
 */
public final class GameplayBoundaryWitness {
    private final String status, kind, recorderEpoch, recordingSessionId, archiveId;
    private final String integrationKey, externalRecordingId;
    private final int segmentSequence;
    private final long segmentFirstTick, exclusiveLocalTick, captureSequence, generation;
    private GameplayBoundaryWitness(String status, String kind, String epoch, String session,
            String archive, int segment, long origin, long cursor, long sequence, long generation,
            String integration, String external) {
        this.status=status; this.kind=kind; this.recorderEpoch=epoch; this.recordingSessionId=session;
        this.archiveId=archive; this.segmentSequence=segment; this.segmentFirstTick=origin;
        this.exclusiveLocalTick=cursor; this.captureSequence=sequence; this.generation=generation;
        this.integrationKey=integration; this.externalRecordingId=external;
    }
    public static GameplayBoundaryWitness unavailable(String status) {
        return new GameplayBoundaryWitness(status,null,null,null,null,-1,-1,-1,-1,-1,null,null);
    }
    public static GameplayBoundaryWitness captured(String kind, String epoch, String session,
            String archive, int segment, long origin, long cursor, long sequence, long generation,
            String integration, String external) {
        return new GameplayBoundaryWitness("CAPTURED",kind,epoch,session,archive,segment,origin,cursor,
                sequence,generation,integration,external);
    }
    public String status() { return status; }
    public String kind() { return kind; }
    public String recorderEpoch() { return recorderEpoch; }
    public String recordingSessionId() { return recordingSessionId; }
    public String archiveId() { return archiveId; }
    public int segmentSequence() { return segmentSequence; }
    public long segmentFirstTick() { return segmentFirstTick; }
    public long exclusiveLocalTick() { return exclusiveLocalTick; }
    public long captureSequence() { return captureSequence; }
    public long generation() { return generation; }
    public String integrationKey() { return integrationKey; }
    public String externalRecordingId() { return externalRecordingId; }
}
