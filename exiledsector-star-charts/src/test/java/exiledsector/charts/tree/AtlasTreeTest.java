package exiledsector.charts.tree;

import exiledsector.charts.atlas.AtlasState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeTest {

    private AtlasTreeData data;
    private AtlasState state;

    @BeforeEach
    void setUp() {
        data = AtlasTreeFixtures.data();
        AtlasTreeData.use(data);
        state = new AtlasState(1L);
    }

    @AfterEach
    void tearDown() {
        AtlasTreeData.use(AtlasTreeData.empty());
    }

    @Test
    void allocatingSpendsAnAtlasSkillPointAndNeedsOne() {
        assertFalse(AtlasTree.allocate(state, data.node("a")));

        state.addAtlasPoints(1);

        assertTrue(AtlasTree.allocate(state, data.node("a")));
        assertEquals(Set.of("a"), state.atlasTreeNodeIds());
        assertEquals(0, AtlasTree.unspentPoints(state, AtlasTree.allocation(state)));
        assertFalse(AtlasTree.allocate(state, data.node("b")));
    }

    @Test
    void aPathIsAllocatedOnlyWhenEveryNodeOnItCanBePaidFor() {
        state.addAtlasPoints(2);
        assertFalse(AtlasTree.allocatePath(state, data.node("c")));

        state.addAtlasPoints(1);

        assertTrue(AtlasTree.allocatePath(state, data.node("c")));
        assertEquals(Set.of("a", "b", "c"), state.atlasTreeNodeIds());
    }

    @Test
    void refundingGivesThePointBackButNeverStrandsANode() {
        state.addAtlasPoints(3);
        AtlasTree.allocatePath(state, data.node("c"));

        assertFalse(AtlasTree.refund(state, data.node("a")));
        assertTrue(AtlasTree.refund(state, data.node("c")));

        assertEquals(Set.of("a", "b"), state.atlasTreeNodeIds());
        assertEquals(1, AtlasTree.unspentPoints(state, AtlasTree.allocation(state)));
    }

    @Test
    void atlasSkillPointsStopAtOneHundred() {
        state.addAtlasPoints(150);

        assertEquals(AtlasState.MAX_ATLAS_SKILL_POINTS, state.atlasPoints());
    }
}
