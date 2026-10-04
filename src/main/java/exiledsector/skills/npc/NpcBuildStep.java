package exiledsector.skills.npc;

public record NpcBuildStep(String nodeId, String outcome) {

    public static final String ALLOCATED = "allocated";
    public static final String GOAL = "allocated: goal";
    public static final String PATH_TO_GOAL = "allocated: path to goal";
    public static final String FACTION_GOAL = "allocated: faction volume goal";
    public static final String WORMHOLE_EXIT = "allocated: wormhole exit";
    public static final String PATH_TO_CONVERTED_HULLMOD = "allocated: path to converted hullmod ";
    public static final String CONVERTED_HULLMOD = "allocated: converts hullmod ";
    public static final String FREED_OP_SUFFIX = " (OP freed by stripped hullmods)";
    public static final String HULLMOD_KEPT = "hullmod kept, equivalent node out of reach: ";
    public static final String INVALID_ROOT = "invalid root: ";
    public static final String MISSING_OPTION = "missing option";
    public static final String INVALID_OPTION = "invalid option: ";
    public static final String UNEXPECTED_OPTION = "unexpected option: ";

    public boolean isAllocated() {
        return outcome != null && outcome.startsWith(ALLOCATED);
    }

    public boolean isCharged() {
        return isAllocated() && !WORMHOLE_EXIT.equals(outcome);
    }
}
