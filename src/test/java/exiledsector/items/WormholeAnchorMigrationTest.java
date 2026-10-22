package exiledsector.items;

import exiledsector.skills.AnchorLinks;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WormholeAnchorMigrationTest {

    private SkillNode root;
    private SkillNode coreSmall;
    private SkillNode strandedCore;
    private SkillNode exit;
    private SkillNode volumeSmall;
    private SkillTreeTopology topology;

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT, "core");
        coreSmall = node("core_a", SkillTier.SMALL, "core", "root");
        strandedCore = node("core_island", SkillTier.SMALL, "core", "nowhere");
        exit = node("hegemony_2", SkillTier.WORMHOLE_EXIT, "hegemony", "heg_a");
        volumeSmall = node("heg_a", SkillTier.SMALL, "hegemony", "hegemony_2");
        topology = SkillTreeTopology.of(List.of(root, coreSmall, strandedCore, exit, volumeSmall));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, String region, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "", tier).build();
        return new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    private ShipSkillData oldSaveShip() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, coreSmall, 1);
        ShipTrees.allocate(data, strandedCore, 1);
        ShipTrees.allocate(data, exit, 0);
        ShipTrees.allocate(data, volumeSmall, 1);
        ShipTrees.recordItemCharge(data, "heg_a", new SkillItemCost("alpha_core", 1f));
        return data;
    }

    @Test
    void volumeNodesNoAnchorReachesAreReleasedAndRefundedWhileCoreNodesStay() {
        ShipSkillData owned = oldSaveShip();
        ShipSkillData sold = oldSaveShip();
        Map<String, ShipSkillData> ships = new LinkedHashMap<>();
        ships.put("owned", owned);
        ships.put("sold", sold);
        List<SkillItemCost> refunds = new ArrayList<>();

        WormholeAnchorMigration.Summary summary = WormholeAnchorMigration.releaseStrandedVolumes(ships, topology, "owned"::equals, refunds::add);

        assertEquals(new WormholeAnchorMigration.Summary(2, 4), summary);
        for (ShipSkillData ship : List.of(owned, sold)) {
            assertFalse(ship.isAllocated("hegemony_2") || ship.isAllocated("heg_a"));
            assertTrue(ship.isAllocated("core_a") && ship.isAllocated("core_island"));
        }
        assertEquals(List.of(new SkillItemCost("alpha_core", 1f)), refunds);
    }

    @Test
    void theMigrationRunsOnceAndLaterLoadsNeverSweepAgain() {
        Map<String, Object> persistentData = new HashMap<>();
        ShipSkillData firstShip = oldSaveShip();

        assertEquals(new WormholeAnchorMigration.Summary(1, 2), WormholeAnchorMigration.runOnce(persistentData, Map.of("owned", firstShip),
                topology, shipId -> true, refund -> { }));

        ShipSkillData strandedLater = oldSaveShip();
        assertNull(WormholeAnchorMigration.runOnce(persistentData, Map.of("owned", strandedLater), topology, shipId -> true, refund -> { }));
        assertTrue(strandedLater.isAllocated("hegemony_2") && strandedLater.isAllocated("heg_a"));
    }

    @Test
    void anAnchorWhoseUnlockReadsFalseAtLoadKeepsItsOpenVolume() {
        SkillNode socket = node("socket", SkillTier.SOCKET, "core", "core_a");
        topology = SkillTreeTopology.of(List.of(root, coreSmall, strandedCore, socket, exit, volumeSmall));
        AnchorLinks.install(new AnchorLinks.ExitLookup() {
            @Override
            public String exitNodeId(String socketedItemId) {
                return "anchor".equals(socketedItemId) ? "hegemony_2" : null;
            }

            @Override
            public boolean canOpen(String socketedItemId) {
                return false;
            }

            @Override
            public int revision() {
                return 0;
            }
        });
        try {
            ShipSkillData data = new ShipSkillData();
            ShipTrees.chooseStartingRoot(data, root);
            ShipTrees.allocate(data, coreSmall, 1);
            ShipTrees.allocate(data, socket, 1);
            data.socketItem("socket", "anchor");
            ShipTrees.allocate(data, volumeSmall, 1);
            ShipTrees.allocate(data, exit, 0);

            assertEquals(new WormholeAnchorMigration.Summary(0, 0),
                    WormholeAnchorMigration.runOnce(new HashMap<>(), Map.of("owned", data), topology, shipId -> true, refund -> { }));
            assertTrue(data.isAllocated("hegemony_2") && data.isAllocated("heg_a"));
        } finally {
            AnchorLinks.install(null);
        }
    }
}
