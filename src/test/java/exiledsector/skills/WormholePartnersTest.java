package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.template.TemplateCapture;
import exiledsector.skills.template.TemplateStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WormholePartnersTest {

    private SkillNode root;
    private SkillNode socket;
    private SkillNode exit;
    private SkillNode beyondExit;
    private SkillNode farSide;
    private SkillTreeTopology topology;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT);
        socket = node("socket", SkillTier.SMALL, "root");
        farSide = node("far_side", SkillTier.SMALL);
        exit = node("exit", SkillTier.SMALL, "far_side");
        beyondExit = node("beyond_exit", SkillTier.SMALL, "exit");
        topology = SkillTreeTopology.of(List.of(root, socket, farSide, exit, beyondExit));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, String... prerequisiteIds) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(prerequisiteIds), 0f, 0f);
    }

    private static SkillNode paired(String id, SkillTier tier, String pairedNodeId) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(), 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedNodeId));
    }

    private ShipSkillData linkedSocketToExit() {
        ShipSkillData data = new ShipSkillData() {
            @Override
            public String wormholePartner(SkillNode node) {
                return switch (node.getId()) {
                    case "socket" -> "exit";
                    case "exit" -> "socket";
                    default -> super.wormholePartner(node);
                };
            }
        };
        data.chooseStartingRoot(root);
        return data;
    }

    private static AllocationGate gate(ShipSkillData data, SkillTreeTopology topology, int maxAllocatedNodes) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.CRUISER, ShieldType.FRONT, false, 0f, hullModId -> false), null);
        return new AllocationGate(data, topology, "root", new AllocationGate.Budget(100, 1, maxAllocatedNodes), eligibility);
    }

    @Test
    void theStaticPartnerOfAWormholeIsItsPairedWithId() {
        assertEquals("wormhole_b", WormholePartners.pairedWith(paired("wormhole_a", SkillTier.WORMHOLE, "wormhole_b")));
    }

    @Test
    void aNodeOutsideTheWormholeTierHasNoStaticPartner() {
        assertNull(WormholePartners.pairedWith(paired("stray", SkillTier.SMALL, "wormhole_b")));
        assertNull(WormholePartners.pairedWith(paired("unpaired", SkillTier.WORMHOLE, null)));
    }

    @Test
    void aShipsPartnerIsTheStaticPairing() {
        ShipSkillData data = new ShipSkillData();

        assertEquals("wormhole_b", data.wormholePartner(paired("wormhole_a", SkillTier.WORMHOLE, "wormhole_b")));
        assertNull(data.wormholePartner(socket));
    }

    @Test
    void aShipsWormholePairsAreTheTopologysPairs() {
        SkillTreeTopology pairedTopology = SkillTreeTopology.of(List.of(
                paired("wormhole_a", SkillTier.WORMHOLE, "wormhole_b"), paired("wormhole_b", SkillTier.WORMHOLE, "wormhole_a")));

        assertSame(pairedTopology.wormholePairs(), new ShipSkillData().wormholePairs(pairedTopology));
    }

    @Test
    void allocatingAnEndGrantsTheShipsPartnerForFreeAndDeallocatingReleasesIt() {
        ShipSkillData data = linkedSocketToExit();

        data.allocate(socket, 1);

        assertTrue(data.isAllocated("exit"));
        assertTrue(data.isFreeNode("exit"));

        data.deallocate(socket);

        assertFalse(data.isAllocated("exit"));
    }

    @Test
    void reachabilityCrossesTheShipsPartnerSoRemovingTheEntranceWouldStrandNodesBeyondTheExit() {
        ShipSkillData data = linkedSocketToExit();
        data.allocate(socket, 1);
        data.allocate(beyondExit, 1);

        assertTrue(data.reachableAllocatedNodeIds(topology, "root", Set.of()).contains("beyond_exit"));
        assertFalse(data.canDeallocate(socket, topology, "root"));
    }

    @Test
    void theGateCountsTheShipsPartnerAgainstTheNodeCap() {
        ShipSkillData data = linkedSocketToExit();

        assertEquals(AllocationGate.Refusal.NODE_CAP, gate(data, topology, 2).allocation(socket).refusal());
    }

    @Test
    void aRespecRemovesWhatLiesBeyondTheShipsPartnerFirst() {
        ShipSkillData data = linkedSocketToExit();
        data.allocate(socket, 1);
        data.allocate(beyondExit, 1);

        List<String> planIds = RespecPlan.of(data, topology, "root", socket).stream().map(SkillNode::getId).toList();

        assertEquals(List.of("beyond_exit", "socket"), planIds);
    }

    @Test
    void aCapturedTemplatePlacesTheShipsPartnerRightAfterItsEntrance() {
        Map<String, SkillNode> nodesById = new LinkedHashMap<>();
        for (SkillNode node : List.of(root, socket, farSide, exit, beyondExit)) {
            nodesById.put(node.getId(), node);
        }
        WormholePartners socketToExit = node -> switch (node.getId()) {
            case "socket" -> "exit";
            case "exit" -> "socket";
            default -> null;
        };

        List<TemplateStep> steps = TemplateCapture.capture(List.of("socket", "beyond_exit", "exit"), nodeId -> null, socketToExit, "root",
                nodesById);

        assertEquals(List.of("socket", "exit", "beyond_exit"), steps.stream().map(TemplateStep::nodeId).toList());
    }
}
