package exiledsector.skills.npc;

import exiledsector.skills.tags.ShipProfile;

public record NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods,
                              NpcFreedOp freedOp, int nodeCount, int socketables) {

    public NpcBuildRequest {
        hullMods = hullMods == null ? NpcHullMods.NONE : hullMods;
        freedOp = freedOp == null ? NpcFreedOp.NONE : freedOp;
        socketables = Math.max(0, socketables);
    }

    public NpcBuildRequest(ShipProfile profile, String designType, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp,
                           int nodeCount) {
        this(profile, designType, factionRegion, hullMods, freedOp, nodeCount, 0);
    }
}
