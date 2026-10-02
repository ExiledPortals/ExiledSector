package exiledsector.persistence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipSkillDataManagerTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
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

        shipA.allocate(node("armor_1"), 1);

        assertNotNull(shipB);
        assertNotEquals(shipA.getSpentOp(1), shipB.getSpentOp(1));
    }

    @Test
    void survivesAcrossLookupsViaThePersistentDataMap() {
        ShipSkillData data = ShipSkillDataManager.get("ship-a");
        data.allocate(node("armor_1"), 1);

        ShipSkillData reread = ShipSkillDataManager.get("ship-a");

        assertEquals(1, reread.getSpentOp(1));
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
        ShipSkillDataManager.get("allocated").allocate(node("armor_1"), 3);
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
        ShipSkillDataManager.get("allocated").allocate(node("armor_1"), 3);

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
        healthy.chooseStartingRoot(root);
        healthy.allocate(lobster, 3);
        healthy.allocate(node("removed"), 3);
        ShipSkillData rootless = ShipSkillDataManager.get("rootless");
        rootless.chooseStartingRoot(removedRoot);
        rootless.allocate(lobster, 3);
        rootless.incrementLevel();
        List<SkillItemCost> refunds = new ArrayList<>();

        ShipSkillDataManager.forgetUnknownNodes(Map.of("root", root, "lobster", lobster), Map.of(), refunds::add);

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
        ship.chooseStartingRoot(root);
        ship.allocate(lobster, 3);
        ship.addFreeAllocationCredit();
        ship.allocate(freebie, 3);
        List<SkillItemCost> refunds = new ArrayList<>();
        Map<String, SkillNode> declared = Map.of("root", root, "lobster", lobster, "freebie", freebie);

        ShipSkillDataManager.forgetUnknownNodes(Map.of("root", root), Map.of(), declared::get, refunds::add);

        assertEquals(List.of("root"), List.copyOf(ship.getAllocatedNodeIds()));
        assertEquals(List.of(new SkillItemCost("lobster", 50f)), refunds);
        assertEquals(1, ship.getBankedFreeAllocations());
    }
}
