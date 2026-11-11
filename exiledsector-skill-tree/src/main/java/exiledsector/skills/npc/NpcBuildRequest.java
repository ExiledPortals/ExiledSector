package exiledsector.skills.npc;

import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionVolume, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes, String hullId,
                              List<Map<String, Float>> focusSources, List<String> grantedNodeIds) {

    private static final Predicate<SkillType> NOTHING_LOCKED = type -> false;

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
        lockedTypes = lockedTypes == null ? NOTHING_LOCKED : lockedTypes;
        focusSources = focusSources == null ? List.of() : List.copyOf(focusSources);
        grantedNodeIds = grantedNodeIds == null ? List.of() : List.copyOf(grantedNodeIds);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionVolume, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes, String hullId, List<Map<String, Float>> focusSources) {
        this(profile, designType, factionVolume, hullMods, freedOp, nodeCount, factionAnchorItemId, lockedTypes, hullId, focusSources, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionVolume, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes, String hullId) {
        this(profile, designType, factionVolume, hullMods, freedOp, nodeCount, factionAnchorItemId, lockedTypes, hullId, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionVolume, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId, Predicate<SkillType> lockedTypes) {
        this(profile, designType, factionVolume, hullMods, freedOp, nodeCount, factionAnchorItemId, lockedTypes, null, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionVolume, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount, String factionAnchorItemId) {
        this(profile, designType, factionVolume, hullMods, freedOp, nodeCount, factionAnchorItemId, null, null, null);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionVolume, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount) {
        this(profile, designType, factionVolume, hullMods, freedOp, nodeCount, null, null, null, null);
    }
}
