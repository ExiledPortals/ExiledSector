package exiledsector.skills.npc;

import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;

import java.util.function.Predicate;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes, String hullId) {

    private static final Predicate<SkillType> NOTHING_LOCKED = type -> false;

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
        lockedTypes = lockedTypes == null ? NOTHING_LOCKED : lockedTypes;
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, factionAnchorItemId, lockedTypes, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, factionAnchorItemId, null, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, null, null, null);
    }
}
