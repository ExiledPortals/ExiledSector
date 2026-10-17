package exiledsector.skills.npc;

import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;

import java.util.function.Predicate;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount, int hullItemCount, Predicate<SkillType> lockedTypes, String hullId) {

    private static final Predicate<SkillType> NOTHING_LOCKED = type -> false;

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
        hullItemCount = Math.max(0, hullItemCount);
        lockedTypes = lockedTypes == null ? NOTHING_LOCKED : lockedTypes;
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, int hullItemCount, Predicate<SkillType> lockedTypes) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, hullItemCount, lockedTypes, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, int hullItemCount) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, hullItemCount, null, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, 0, null, null);
    }
}
