package exiledsector.skills.npc;

import exiledsector.skills.ShipSkillData;

import java.util.List;

public record NpcTreeBuild(ShipSkillData shipData, List<NpcBuildStep> steps) {

    public NpcTreeBuild {
        steps = List.copyOf(steps);
    }
}
