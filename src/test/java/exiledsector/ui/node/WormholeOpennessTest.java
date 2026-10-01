package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WormholeOpennessTest {

    private static final float EPSILON = 1e-4f;

    private MockedStatic<SkillTree> skillTreeMock;
    private ShipSkillData data;
    private WormholeOpenness openness;

    private static SkillNode node(String id, SkillTier tier) {
        return new SkillNode(id, new SkillType.Builder(id + "_type", id, "", tier).build(), List.of(), 0f, 0f);
    }

    @BeforeEach
    void setUp() {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        for (SkillNode node : List.of(node("wormhole_a", SkillTier.WORMHOLE), node("regular", SkillTier.NOTABLE))) {
            nodes.put(node.getId(), node);
        }
        SkillTreeTopology topology = SkillTreeTopology.of(nodes.values());
        skillTreeMock = Mockito.mockStatic(SkillTree.class);
        skillTreeMock.when(SkillTree::topology).thenReturn(topology);
        data = mock(ShipSkillData.class);
        openness = new WormholeOpenness();
    }

    @AfterEach
    void tearDown() {
        skillTreeMock.close();
    }

    @Test
    void aWormholeAlreadyAllocatedWhenFirstSeenIsFullyOpenStraightAway() {
        when(data.isAllocated("wormhole_a")).thenReturn(true);

        assertEquals(0f, openness.of("wormhole_a"), EPSILON);
        openness.advance(0.01f, data);

        assertEquals(1f, openness.of("wormhole_a"), EPSILON);
    }

    @Test
    void allocatingOpensAndDeallocatingClosesOverOneSecond() {
        openness.advance(0.01f, data);
        when(data.isAllocated("wormhole_a")).thenReturn(true);

        openness.advance(0.25f * WormholeOpenness.OPEN_SECONDS, data);
        assertEquals(0.25f, openness.of("wormhole_a"), EPSILON);
        openness.advance(5f, data);
        assertEquals(1f, openness.of("wormhole_a"), EPSILON);

        when(data.isAllocated("wormhole_a")).thenReturn(false);
        openness.advance(0.5f * WormholeOpenness.OPEN_SECONDS, data);
        assertEquals(0.5f, openness.of("wormhole_a"), EPSILON);
        openness.advance(5f, data);
        assertEquals(0f, openness.of("wormhole_a"), EPSILON);
    }

    @Test
    void onlyWormholeNodesAreTracked() {
        when(data.isAllocated("regular")).thenReturn(true);

        openness.advance(1f, data);
        openness.advance(1f, data);

        assertEquals(0f, openness.of("regular"), EPSILON);
    }
}
