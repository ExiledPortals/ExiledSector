package exiledsector.skills;

import java.util.List;

public record ReleasedNodes(List<String> nodeIds, List<SkillItemCost> charges) {

    public static final ReleasedNodes NONE = new ReleasedNodes(List.of(), List.of());

    public ReleasedNodes {
        nodeIds = List.copyOf(nodeIds);
        charges = List.copyOf(charges);
    }

    public boolean isEmpty() {
        return nodeIds.isEmpty();
    }
}
