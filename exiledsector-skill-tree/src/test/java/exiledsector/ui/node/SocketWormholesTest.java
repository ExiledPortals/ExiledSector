package exiledsector.ui.node;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocketWormholesTest {

    private static final float EPSILON = 1e-4f;

    private final SkillNode exit = node("hegemony_2", "wormhole_exit");
    private final SkillNode exoship = node("exoship_1", ExoshipNodes.EXOSHIP_TYPE_ID);
    private MockedStatic<SkillTree> skillTreeMock;
    private MockedStatic<ShipTrees> shipTreesMock;
    private ShipSkillData data;
    private SocketWormholes wormholes;

    private static SkillNode node(String id, String typeId) {
        return new SkillNode(id, new SkillType.Builder(typeId, id, "", SkillTier.WORMHOLE_EXIT).build(), List.of(), 0f, 0f);
    }

    @BeforeEach
    void setUp() {
        skillTreeMock = Mockito.mockStatic(SkillTree.class);
        shipTreesMock = Mockito.mockStatic(ShipTrees.class);
        skillTreeMock.when(() -> SkillTree.get(exit.getId())).thenReturn(exit);
        skillTreeMock.when(() -> SkillTree.get(exoship.getId())).thenReturn(exoship);
        data = mock(ShipSkillData.class);
        shipTreesMock.when(() -> ShipTrees.anchorExitsBySocket(data)).thenReturn(Map.of());
        wormholes = new SocketWormholes();
    }

    @AfterEach
    void tearDown() {
        skillTreeMock.close();
        shipTreesMock.close();
    }

    @Test
    void anAnchorAlreadySocketedWhenTheTreeOpensIsAWormholeStraightAway() {
        shipTreesMock.when(() -> ShipTrees.anchorExitsBySocket(data)).thenReturn(Map.of("socket_1", exit.getId()));

        wormholes.advance(0.01f, data);

        assertEquals(1f, wormholes.morph("socket_1"), EPSILON);
        assertSame(exit, wormholes.exit("socket_1"));
    }

    @Test
    void socketingMorphsIntoAWormholeOverThreeSecondsAndRemovingMorphsBack() {
        wormholes.advance(0.01f, data);
        shipTreesMock.when(() -> ShipTrees.anchorExitsBySocket(data)).thenReturn(Map.of("socket_1", exit.getId()));

        wormholes.advance(0.5f * SocketWormholes.MORPH_SECONDS, data);
        assertEquals(0.5f, wormholes.morph("socket_1"), EPSILON);
        wormholes.advance(0.5f * SocketWormholes.MORPH_SECONDS, data);
        assertEquals(1f, wormholes.morph("socket_1"), EPSILON);

        shipTreesMock.when(() -> ShipTrees.anchorExitsBySocket(data)).thenReturn(Map.of());
        wormholes.advance(0.5f * SocketWormholes.MORPH_SECONDS, data);
        assertEquals(0.5f, wormholes.morph("socket_1"), EPSILON);
        assertSame(exit, wormholes.exit("socket_1"));
        wormholes.advance(SocketWormholes.MORPH_SECONDS, data);
        assertEquals(0f, wormholes.morph("socket_1"), EPSILON);
        assertNull(wormholes.exit("socket_1"));
    }

    @Test
    void theWormholeOpensOnlyOnceItsExitIsAllocated() {
        shipTreesMock.when(() -> ShipTrees.anchorExitsBySocket(data)).thenReturn(Map.of("socket_1", exit.getId()));
        wormholes.advance(0.01f, data);
        assertEquals(0f, wormholes.openness("socket_1"), EPSILON);

        when(data.isAllocated(exit.getId())).thenReturn(true);
        wormholes.advance(WormholeOpenness.OPEN_SECONDS, data);

        assertEquals(1f, wormholes.openness("socket_1"), EPSILON);
    }

    @Test
    void theExoshipBeaconStaysASocket() {
        wormholes.advance(0.01f, data);
        shipTreesMock.when(() -> ShipTrees.anchorExitsBySocket(data)).thenReturn(Map.of("socket_1", exoship.getId()));

        wormholes.advance(SocketWormholes.MORPH_SECONDS, data);

        assertEquals(0f, wormholes.morph("socket_1"), EPSILON);
    }
}
