/* Copyright (c) ForgeVector Software Limited. Author: William Bowyer / Fluid. */
package uk.co.forgevector.replaycore.api.plugin;

/** Opaque recorder-issued callback position. The recorder validates object identity before adoption. */
public final class ScopeEventPositionWitness {
    private final String status;
    private final String reason;
    private ScopeEventPositionWitness(String status,String reason) {this.status=status;this.reason=reason;}
    public String status() {return status;}
    public String reason() {return reason;}
    /** Creating a marker does not create authority; only the issuing recorder can adopt its own object. */
    public static ScopeEventPositionWitness issued() {return new ScopeEventPositionWitness("ISSUED",null);}
    public static ScopeEventPositionWitness unavailable(String reason) {
        return new ScopeEventPositionWitness("UNAVAILABLE",reason);
    }
}
