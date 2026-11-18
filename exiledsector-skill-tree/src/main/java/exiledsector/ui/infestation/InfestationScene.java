package exiledsector.ui.infestation;

import exiledsector.skills.SkillNode;

import java.util.List;

public record InfestationScene(SkillNode socket, float worldRadius, List<SkillNode> takenNodes, List<TakenLink> takenLinks) {

    public InfestationScene {
        takenNodes = takenNodes == null ? List.of() : List.copyOf(takenNodes);
        takenLinks = takenLinks == null ? List.of() : List.copyOf(takenLinks);
    }

    public record TakenLink(SkillNode from, SkillNode to) {
    }
}
