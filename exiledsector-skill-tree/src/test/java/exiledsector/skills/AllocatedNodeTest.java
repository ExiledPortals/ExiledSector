package exiledsector.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static exiledsector.skills.TestTrees.node;
import static exiledsector.skills.TestTrees.registered;
import static exiledsector.skills.TestTrees.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class AllocatedNodeTest {

    private SkillNode root;
    private SkillNode choice;
    private SkillType hullOption;

    @BeforeEach
    void setUp() {
        hullOption = registered(type("hull_option", SkillTier.SMALL).build());
        root = node("root_1", type("root", SkillTier.ROOT).build());
        choice = node("optional_1", type("optional", SkillTier.SMALL).choiceOptionIds(List.of("hull_option")).build(), "root_1");
    }

    @Test
    void walksAllocatedNodesInAllocationOrderWithTheirChosenOptionResolved() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.selectOption(data, choice, hullOption, 0);

        List<AllocatedNode> allocated = AllocatedNode.of(data);

        assertEquals(List.of(new AllocatedNode(root, root.getType()), new AllocatedNode(choice, hullOption)), allocated);
    }

    @Test
    void skipsAllocatedIdsThatAreNoLongerInTheTree() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        SkillTree.unregister("root_1");

        assertTrue(AllocatedNode.of(data).isEmpty());
    }
}
