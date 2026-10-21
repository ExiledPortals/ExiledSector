package exiledsector.skills.npc;

import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;

import java.util.Map;
import java.util.function.Predicate;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes, String hullId,
                              Map<String, Float> focus) {

    private static final Predicate<SkillType> NOTHING_LOCKED = type -> false;

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
        lockedTypes = lockedTypes == null ? NOTHING_LOCKED : lockedTypes;
        focus = focus == null ? Map.of() : Map.copyOf(focus);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes, String hullId) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, factionAnchorItemId, lockedTypes, hullId, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, factionAnchorItemId, lockedTypes, null, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, factionAnchorItemId, null, null, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, null, null, null, null);
    }
}
