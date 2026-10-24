package exiledsector.persistence;

import exiledlib.MockSector;
import exiledsector.skills.ItemChargeLedger;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeRepairs;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipSkillDataManagerTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void createsFreshDataForAnUnknownShip() {
        ShipSkillData data = ShipSkillDataManager.get("ship-a");

        assertNotNull(data);
        assertNotNull(data.getAllocatedNodeIds());
    }

    @Test
    void returnsTheSameInstanceOnRepeatedLookupsForTheSameShip() {
        ShipSkillData first = ShipSkillDataManager.get("ship-a");
        ShipSkillData second = ShipSkillDataManager.get("ship-a");

        assertSame(first, second);
    }

    @Test
    void keepsDataSeparateForDifferentShips() {
        ShipSkillData shipA = ShipSkillDataManager.get("ship-a");
        ShipSkillData shipB = ShipSkillDataManager.get("ship-b");

        ShipTrees.allocate(shipA, node("armor_1"), 1);

        assertNotNull(shipB);
        assertNotEquals(ShipTrees.getSpentOp(shipA, 1), ShipTrees.getSpentOp(shipB, 1));
    }

    @Test
    void survivesAcrossLookupsViaThePersistentDataMap() {
        ShipSkillData data = ShipSkillDataManager.get("ship-a");
        ShipTrees.allocate(data, node("armor_1"), 1);

        ShipSkillData reread = ShipSkillDataManager.get("ship-a");

        assertEquals(1, ShipTrees.getSpentOp(reread, 1));
    }

    private static SkillNode node(String id) {
        SkillType type = new SkillType.Builder(id, id, "graphics/hullmods/heavy_armor.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, List.of(), 0f, 0f);
    }

    @Test
    void putReplacesTheStoredTreeForAShip() {
        ShipSkillData replacement = new ShipSkillData();
        ShipSkillDataManager.get("ship-a");

        ShipSkillDataManager.put("ship-a", replacement);

        assertSame(replacement, ShipSkillDataManager.get("ship-a"));
    }

    @Test
    void findLooksUpAShipWithoutCreatingARecordForIt() {
        assertNull(ShipSkillDataManager.find("temporary-copy"));

        ShipSkillData data = ShipSkillDataManager.get("ship-a");

        assertSame(data, ShipSkillDataManager.find("ship-a"));
        assertNull(ShipSkillDataManager.find("temporary-copy"));
    }

    @Test
    void aShipHasProgressOnceItHasAnyNodesOrXp() {
        ShipTrees.allocate(ShipSkillDataManager.get("allocated"), node("armor_1"), 3);
        ShipSkillDataManager.get("levelled").addXp(1f);
        ShipSkillDataManager.get("blank");

        assertTrue(ShipSkillDataManager.hasProgress("allocated"));
        assertTrue(ShipSkillDataManager.hasProgress("levelled"));
        assertFalse(ShipSkillDataManager.hasProgress("blank"));
        assertFalse(ShipSkillDataManager.hasProgress("unknown"));
    }

    @Test
    void removingBlankRecordsKeepsEveryShipWithProgress() {
        ShipSkillDataManager.get("blank-a");
        ShipSkillDataManager.get("blank-b");
        ShipSkillDataManager.get("levelled").addXp(10f);
        ShipTrees.allocate(ShipSkillDataManager.get("allocated"), node("armor_1"), 3);

        ShipSkillDataManager.removeBlankRecords();

        assertNull(ShipSkillDataManager.find("blank-a"));
        assertNull(ShipSkillDataManager.find("blank-b"));
        assertNotNull(ShipSkillDataManager.find("levelled"));
        assertNotNull(ShipSkillDataManager.find("allocated"));
    }

    private static SkillNode typedNode(String id, SkillTier tier, SkillItemCost itemCost) {
        SkillType type = new SkillType.Builder(id, id, "a.png", tier).effects(List.of()).itemCost(itemCost).build();
        return new SkillNode(id, type, List.of(), 0f, 0f);
    }

    @Test
    void forgettingUnknownNodesTrimsHealthyShipsAndResetsShipsWhoseRootIsGoneRefundingTheirItems() {
        SkillNode root = typedNode("root", SkillTier.ROOT, null);
        SkillNode lobster = typedNode("lobster", SkillTier.SMALL, new SkillItemCost("lobster", 50f));
        SkillNode removedRoot = typedNode("removed_root", SkillTier.ROOT, null);
        ShipSkillData healthy = ShipSkillDataManager.get("healthy");
        ShipTrees.chooseStartingRoot(healthy, root);
        ShipTrees.allocate(healthy, lobster, 3);
        ItemChargeLedger.recordItemCharge(healthy, "lobster", lobster.getType().getItemCost());
        ShipTrees.allocate(healthy, node("removed"), 3);
        ShipSkillData rootless = ShipSkillDataManager.get("rootless");
        ShipTrees.chooseStartingRoot(rootless, removedRoot);
        ShipTrees.allocate(rootless, lobster, 3);
        ItemChargeLedger.recordItemCharge(rootless, "lobster", lobster.getType().getItemCost());
        rootless.incrementLevel();
        List<SkillItemCost> refunds = new ArrayList<>();
        Map<String, SkillNode> tree = Map.of("root", root, "lobster", lobster);

        ShipTreeRepairs.forgetUnknownNodes(tree, tree, Map.of(), shipId -> true, refunds::add);

        assertEquals(List.of("root", "lobster"), List.copyOf(healthy.getAllocatedNodeIds()));
        assertTrue(rootless.getAllocatedNodeIds().isEmpty());
        assertEquals(1, rootless.getLevel());
        assertEquals(List.of(new SkillItemCost("lobster", 50f)), refunds);
    }

    @Test
    void nodesDroppedWithASwitchedOffAreaRefundTheirItemCostAndBankedAllocation() {
        SkillNode root = typedNode("root", SkillTier.ROOT, null);
        SkillNode lobster = typedNode("lobster", SkillTier.SMALL, new SkillItemCost("lobster", 50f));
        SkillNode freebie = typedNode("freebie", SkillTier.SMALL, null);
        ShipSkillData ship = ShipSkillDataManager.get("ship");
        ShipTrees.chooseStartingRoot(ship, root);
        ShipTrees.allocate(ship, lobster, 3);
        ItemChargeLedger.recordItemCharge(ship, "lobster", lobster.getType().getItemCost());
        ship.addFreeAllocationCredit();
        ShipTrees.allocate(ship, freebie, 3);
        List<SkillItemCost> refunds = new ArrayList<>();

        ShipTreeRepairs.forgetUnknownNodes(Map.of("root", root), Map.of("root", root), Map.of(), shipId -> true, refunds::add);

        assertEquals(List.of("root"), List.copyOf(ship.getAllocatedNodeIds()));
        assertEquals(List.of(new SkillItemCost("lobster", 50f)), refunds);
        assertEquals(1, ship.getBankedFreeAllocations());
    }

    @Test
    void shipsThePlayerNoLongerOwnsLoseRemovedNodesWithoutARefund() {
        SkillNode root = typedNode("root", SkillTier.ROOT, null);
        SkillNode lobster = typedNode("lobster", SkillTier.SMALL, new SkillItemCost("lobster", 50f));
        SkillNode removedRoot = typedNode("removed_root", SkillTier.ROOT, null);
        ShipSkillData owned = ShipSkillDataManager.get("owned");
        ShipTrees.chooseStartingRoot(owned, root);
        ShipTrees.allocate(owned, lobster, 3);
        ItemChargeLedger.recordItemCharge(owned, "lobster", lobster.getType().getItemCost());
        ShipSkillData sold = ShipSkillDataManager.get("sold");
        ShipTrees.chooseStartingRoot(sold, root);
        ShipTrees.allocate(sold, lobster, 3);
        ItemChargeLedger.recordItemCharge(sold, "lobster", lobster.getType().getItemCost());
        ShipSkillData destroyed = ShipSkillDataManager.get("destroyed");
        ShipTrees.chooseStartingRoot(destroyed, removedRoot);
        ShipTrees.allocate(destroyed, lobster, 3);
        ItemChargeLedger.recordItemCharge(destroyed, "lobster", lobster.getType().getItemCost());
        List<SkillItemCost> refunds = new ArrayList<>();

        ShipTreeRepairs.forgetUnknownNodes(Map.of("root", root), Map.of("root", root), Map.of(), "owned"::equals, refunds::add);

        assertEquals(List.of("root"), List.copyOf(sold.getAllocatedNodeIds()));
        assertTrue(destroyed.getAllocatedNodeIds().isEmpty());
        assertEquals(List.of(new SkillItemCost("lobster", 50f)), refunds);
    }

    @Test
    void anItemCostNodeThatWasNeverChargedRefundsNothingWhenItIsRemoved() {
        SkillNode root = typedNode("root", SkillTier.ROOT, null);
        SkillNode lobster = typedNode("lobster", SkillTier.SMALL, new SkillItemCost("lobster", 50f));
        ShipSkillData adopted = ShipSkillDataManager.get("adopted");
        ShipTrees.chooseStartingRoot(adopted, root);
        ShipTrees.allocate(adopted, lobster, 3);
        List<SkillItemCost> refunds = new ArrayList<>();

        ShipTreeRepairs.forgetUnknownNodes(Map.of("root", root), Map.of("root", root), Map.of(), shipId -> true, refunds::add);

        assertEquals(List.of("root"), List.copyOf(adopted.getAllocatedNodeIds()));
        assertTrue(refunds.isEmpty());
    }
}
