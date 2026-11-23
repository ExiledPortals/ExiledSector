package exiledsector.skills;

import exiledsector.MockSector;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcStationSubsystem;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static exiledsector.skills.TestTrees.small;
import static exiledsector.skills.TestTrees.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class ResearchStationNodesTest {

    private static final float STATION_Y = 950f;
    private static final float RING_RADIUS = 260f;

    private MockSector mockSector;
    private SkillNode root;
    private SkillNode station;
    private SkillNode top;
    private SkillNode right;
    private SkillNode left;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SkillType stationType = type(ResearchStationNodes.STATION_TYPE_ID, SkillTier.NOTABLE)
                .integratedEffects(new IntegratedEffects(List.of(), List.of()))
                .tags(List.of(SkillTags.AI_SUBSYSTEM_INTEGRATION)).build();
        root = register(new SkillNode("root_a", type("root_a", SkillTier.ROOT).build(), List.of(), 0f, 0f));
        station = register(new SkillNode("ai_subsystem_integration_1", stationType, List.of(), 0f, STATION_Y));
        left = subsystem("left", -RING_RADIUS, 0f);
        top = subsystem("top", 0f, -RING_RADIUS);
        right = subsystem("right", RING_RADIUS, 0f);
    }

    @AfterEach
    void tearDown() {
        IntegrationCostSetting.setModeForTests(IntegrationCostSetting.Mode.EXPONENTIAL);
        mockSector.close();
    }

    private ShipSkillData rooted() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        return data;
    }

    private static SkillNode register(SkillNode node) {
        SkillTree.register(node);
        return node;
    }

    private static SkillNode subsystem(String name, float offsetX, float offsetY) {
        SkillType subsystemType = type("adaptive_" + name, SkillTier.NOTABLE).tags(List.of(SkillTags.STATION_SUBSYSTEM)).build();
        return register(new SkillNode(subsystemType.getId() + "_1", subsystemType, List.of(), offsetX, STATION_Y + offsetY,
                SkillNodeDecoration.NONE, List.of()));
    }

    @Test
    void indexesTheStationAndItsSubsystemsClockwiseFromTheTop() {
        assertSame(station, ResearchStationNodes.station());
        assertEquals(List.of(top, right, left), ResearchStationNodes.subsystems());
        assertTrue(ResearchStationNodes.isStationNode(station));
        assertTrue(ResearchStationNodes.isStationNodeId(top.getId()));
        assertFalse(ResearchStationNodes.isStationNode(small("hull_1")));
    }

    @Test
    void aCrewedShipGetsTheStationOnlyOnceItIsIntegrated() {
        ShipSkillData data = rooted();
        int rootSlots = data.usedNodeSlots();

        assertFalse(ResearchStationNodes.sync(data, false));
        assertFalse(data.isAllocated(station.getId()));
        assertTrue(IntegratedNodes.canIntegrate(data, station));

        data.integrate(station.getId());
        assertTrue(ResearchStationNodes.sync(data, false));
        assertTrue(data.isAllocated(station.getId()));
        assertTrue(data.isGrantFree(station.getId()));
        assertEquals(rootSlots, data.usedNodeSlots());
        assertTrue(ResearchStationNodes.countsAsIntegration(data));
    }

    @Test
    void anAutomatedShipGetsTheStationFreeWithoutCountingAnIntegration() {
        ShipSkillData data = rooted();
        int rootSlots = data.usedNodeSlots();
        int costBefore = IntegratedNodes.nextCost(data);

        assertTrue(ResearchStationNodes.sync(data, true));

        assertTrue(data.isAllocated(station.getId()));
        assertFalse(ResearchStationNodes.countsAsIntegration(data));
        assertEquals(costBefore, IntegratedNodes.nextCost(data));
        assertEquals(rootSlots, data.usedNodeSlots());
        assertFalse(ResearchStationNodes.sync(data, true));
    }

    @Test
    void selectingASubsystemKeepsOnlyThatOneForFree() {
        ShipSkillData data = rooted();
        int rootSlots = data.usedNodeSlots();
        ResearchStationNodes.sync(data, true);

        ResearchStationNodes.select(data, top);
        ResearchStationNodes.select(data, right);

        assertSame(right, ResearchStationNodes.selectedSubsystem(data));
        assertFalse(data.isAllocated(top.getId()));
        assertTrue(data.isGrantFree(right.getId()));
        assertEquals(rootSlots, data.usedNodeSlots());

        assertTrue(ResearchStationNodes.release(data));
        assertNull(ResearchStationNodes.selectedSubsystem(data));
        assertFalse(ResearchStationNodes.release(data));
    }

    @Test
    void syncDropsExtraPicksAndKeepsTheFirst() {
        ShipSkillData data = rooted();
        ResearchStationNodes.sync(data, true);
        ShipTrees.allocateGranted(data, top);
        ShipTrees.allocateGranted(data, left);

        assertTrue(ResearchStationNodes.sync(data, true));

        assertSame(top, ResearchStationNodes.selectedSubsystem(data));
        assertFalse(data.isAllocated(left.getId()));
    }

    @Test
    void losingTheIntegrationDropsTheStationAndItsPick() {
        ShipSkillData data = rooted();
        data.integrate(station.getId());
        ResearchStationNodes.sync(data, false);
        ResearchStationNodes.select(data, top);

        data.forgetIntegration(station.getId());

        assertTrue(ResearchStationNodes.sync(data, false));
        assertFalse(data.isAllocated(station.getId()));
        assertNull(ResearchStationNodes.selectedSubsystem(data));
    }

    @Test
    void ungrantedFreeNodeReleaseLeavesTheStationAlone() {
        ShipSkillData data = rooted();
        ResearchStationNodes.sync(data, true);
        ResearchStationNodes.select(data, top);

        ShipTrees.releaseUngrantedFreeNodes(data, SkillTree.topology(), null);

        assertTrue(data.isAllocated(station.getId()));
        assertTrue(data.isAllocated(top.getId()));
    }

    @Test
    void aShipWithoutAStartingRootNeverGetsTheStation() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(ResearchStationNodes.sync(data, true));
        assertFalse(data.isAllocated(station.getId()));
        assertFalse(ResearchStationNodes.isActive(data, true));
    }

    @Test
    void anAutomatedShipCanStillUnchooseItsRootAndLosesTheFreeStation() {
        ShipSkillData data = rooted();
        ResearchStationNodes.sync(data, true);
        ResearchStationNodes.select(data, top);

        assertTrue(ShipTrees.canUnchooseStartingRoot(data));
        assertTrue(ShipTrees.unchooseStartingRoot(data));

        assertFalse(data.isAllocated(station.getId()));
        assertFalse(data.isAllocated(top.getId()));
        assertFalse(ResearchStationNodes.sync(data, true));
    }

    @Test
    void anIntegratedStationKeepsTheRootChosen() {
        ShipSkillData data = rooted();
        data.integrate(station.getId());
        ResearchStationNodes.sync(data, false);

        assertFalse(ShipTrees.canUnchooseStartingRoot(data));
    }

    @Test
    void npcTreesCarryTheirStationPickInTheirTag() {
        ShipSkillData data = new ShipSkillData();

        NpcStationSubsystem.restore(data, top.getId());
        String tag = NpcTreeTag.encode(data);

        assertTrue(data.isAllocated(station.getId()));
        assertTrue(data.isIntegrated(station.getId()));
        assertSame(top, ResearchStationNodes.selectedSubsystem(data));
        assertTrue(tag.contains("station:" + top.getId()), tag);
        assertFalse(tag.contains("integrated:"), tag);
        assertFalse(tag.contains(station.getId()), tag);
    }
}
