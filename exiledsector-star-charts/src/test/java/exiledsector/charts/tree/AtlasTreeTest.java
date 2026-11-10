package exiledsector.charts.tree;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void theChartLinesAreTheAllocatedNodesLinesSummedPerEffect() {
        assertEquals(List.of(), AtlasTree.chartLines(state));

        state.addAtlasPoints(3);
        AtlasTree.allocatePath(state, data.node("c"));

        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 20f)), AtlasTree.chartLines(state));
    }

    @Test
    void atlasLinesAreSummedApartFromTheChartLines() {
        assertEquals(AtlasEffects.NONE, AtlasTree.atlasEffects(state));

        state.addAtlasPoints(3);
        AtlasTree.allocatePath(state, data.node("c"));

        assertEquals(1, AtlasTree.atlasEffects(state).count(AtlasModifier.ATLAS_VISIBILITY_LANES));
        assertTrue(AtlasTree.chartLines(state).stream().allMatch(line -> line.effect() == ChartModifier.CHART_ITEM_PERCENT));
    }

    @Test
    void theAtlasGeneratorIsRebuiltOnlyWhenTheAllocatedAtlasLinesChange() {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>(Map.of(AtlasState.PERSISTENT_KEY, state)));
        AtlasRuntime.invalidate();
        try {
            AtlasGenerator before = AtlasRuntime.generator(sector);
            state.addAtlasPoints(3);
            AtlasTree.allocatePath(state, data.node("b"));
            assertSame(before, AtlasRuntime.generator(sector));

            AtlasTree.allocate(state, data.node("c"));
            AtlasGenerator after = AtlasRuntime.generator(sector);
            assertNotSame(before, after);
            assertEquals(1, after.effects().count(AtlasModifier.ATLAS_VISIBILITY_LANES));
        } finally {
            AtlasRuntime.invalidate();
        }
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
