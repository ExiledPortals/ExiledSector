package exiledsector.skills.npc;

import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;

import java.util.function.Predicate;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount, int socketables, Predicate<SkillType> locked) {

    private static final Predicate<SkillType> NOTHING_LOCKED = type -> false;

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
        socketables = Math.max(0, socketables);
        locked = locked == null ? NOTHING_LOCKED : locked;
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, int socketables) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, socketables, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, 0, null);
    }
}
