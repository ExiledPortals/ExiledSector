package exiledsector.skills;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocatedNodeTest {

    private SkillNode root;
    private SkillNode optional;
    private SkillType hullOption;

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        hullOption = new SkillType.Builder("hull_option", "Hull", "a.png", SkillTier.SMALL).build();
        SkillTree.registerType(hullOption);
        root = new SkillNode("root_1", new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        optional = new SkillNode("optional_1", new SkillType.Builder("optional", "Optional", "a.png", SkillTier.SMALL)
                .optionalOptionIds(List.of("hull_option")).build(), List.of("root_1"), 0f, 0f);
        SkillTree.register(root);
        SkillTree.register(optional);
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @Test
    void walksAllocatedNodesInAllocationOrderWithTheirChosenOptionResolved() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.selectOption(optional, hullOption, 0);

        List<AllocatedNode> allocated = AllocatedNode.of(data);

        assertEquals(List.of(new AllocatedNode(root, root.getType()), new AllocatedNode(optional, hullOption)), allocated);
    }

    @Test
    void skipsAllocatedIdsThatAreNoLongerInTheTree() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        SkillTree.unregister("root_1");

        assertTrue(AllocatedNode.of(data).isEmpty());
    }
}
