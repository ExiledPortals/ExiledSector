package exiledsector.skills.npc;

import exiledsector.skills.tags.ShipProfile;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount) {

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
    }
}
