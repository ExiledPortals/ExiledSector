package exiledsector.skills;

import exiledsector.skills.skilleffect.DefenseSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeTest {

    private static final SkillType TYPE = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
            .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 1f)))
            .build();

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
    }

    @Test
    void getReturnsNullForAnUnregisteredNode() {
        assertNull(SkillTree.get("does_not_exist"));
    }

    @Test
    void registerMakesANodeRetrievableById() {
        SkillNode node = new SkillNode("hull_1", TYPE, List.of(), 0f, 0f);

        SkillTree.register(node);

        assertSame(node, SkillTree.get("hull_1"));
    }

    @Test
    void registeringANodeWithAnExistingIdReplacesIt() {
        SkillNode original = new SkillNode("hull_1", TYPE, List.of(), 0f, 0f);
        SkillNode replacement = new SkillNode("hull_1", TYPE, List.of(), 100f, 100f);
        SkillTree.register(original);

        SkillTree.register(replacement);

        assertSame(replacement, SkillTree.get("hull_1"));
        assertEquals(1, SkillTree.getAllNodes().size());
    }

    @Test
    void getAllNodesReflectsEveryRegisteredNode() {
        SkillTree.register(new SkillNode("hull_1", TYPE, List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("hull_2", TYPE, List.of(), 10f, 10f));

        assertEquals(2, SkillTree.getAllNodes().size());
        assertTrue(SkillTree.getAllNodes().containsKey("hull_1"));
        assertTrue(SkillTree.getAllNodes().containsKey("hull_2"));
    }

    @Test
    void curveKeyIsOrderIndependent() {
        assertEquals(SkillTree.curveKey("a", "b"), SkillTree.curveKey("b", "a"));
    }

    @Test
    void getCurveReturnsNullWhenConnectorIsNotCurved() {
        assertNull(SkillTree.getCurve("hull_1", "hull_2"));
    }

    @Test
    void connectorsAreVisibleByDefault() {
        assertTrue(SkillTree.isConnectorVisible("hull_1", "hull_2"));
    }
}
