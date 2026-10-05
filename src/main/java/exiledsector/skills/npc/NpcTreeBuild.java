package exiledsector.skills.npc;

import exiledsector.skills.ShipSkillData;

import java.util.List;

public record NpcTreeBuild(ShipSkillData data, List<NpcBuildStep> steps, List<String> strippedHullModIds, List<String> claimedSockets) {

    public NpcTreeBuild {
        steps = List.copyOf(steps);
        strippedHullModIds = strippedHullModIds == null ? List.of() : List.copyOf(strippedHullModIds);
        claimedSockets = claimedSockets == null ? List.of() : List.copyOf(claimedSockets);
    }

    public NpcTreeBuild(ShipSkillData data, List<NpcBuildStep> steps, List<String> strippedHullModIds) {
        this(data, steps, strippedHullModIds, List.of());
    }
}
