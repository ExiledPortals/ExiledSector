package exiledsector.skills;

import exiledsector.MockSector;
import exiledsector.persistence.ShipSkillDataManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipTreeRenamesTest {

    private static final SkillItemCost LOBSTERS = new SkillItemCost("lobster", 50f);

    private MockSector mockSector;
    private final Map<String, SkillNode> declaredTree = new LinkedHashMap<>();
    private final Map<String, SkillType> types = new LinkedHashMap<>();
    private SkillType plainType;
    private SkillType socketType;
    private SkillType choiceType;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        SkillType rootType = type(new SkillType.Builder("root_type", "Root", "", SkillTier.ROOT));
        plainType = type(new SkillType.Builder("plain_type", "Plain", "", SkillTier.SMALL));
        socketType = type(new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET));
        type(new SkillType.Builder("new_option", "New Option", "", SkillTier.SMALL));
        type(new SkillType.Builder("other_option", "Other Option", "", SkillTier.SMALL));
        choiceType = type(new SkillType.Builder("choice_type", "Choice", "", SkillTier.SMALL)
                .choiceOptionIds(List.of("other_option", "new_option")));
        declare("root", rootType);
        declare("new_socket", socketType);
        declare("new_armor", plainType);
        declare("new_free", plainType);
        declare("new_paired", plainType);
        declare("new_dormant", plainType);
        declare("new_choice", choiceType);
        JSONArray renames = new JSONArray();
        for (String renamed : List.of("socket", "armor", "free", "paired", "dormant", "choice")) {
            renames.put(new JSONObject().put("old", "old_" + renamed).put("new", "new_" + renamed));
        }
        NodeReplacements.registerRenames(renames);
        NodeReplacements.register(new JSONArray().put(new JSONObject().put("old", "replaced_socket").put("new", "new_socket")));
        NodeReplacements.registerTypeRenames(new JSONArray().put(new JSONObject().put("old", "old_option").put("new", "new_option")));
    }

    @AfterEach
    void tearDown() {
        NodeReplacements.clear();
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        mockSector.close();
    }

    private SkillType type(SkillType.Builder builder) {
        SkillType type = builder.build();
        SkillTree.registerType(type);
        types.put(type.getId(), type);
        return type;
    }

    private void declare(String id, SkillType type) {
        SkillNode node = new SkillNode(id, type, List.of(), 0f, 0f);
        SkillTree.register(node);
        declaredTree.put(id, node);
    }

    private static SkillNode saved(String id, SkillType type) {
        return new SkillNode(id, type, List.of(), 0f, 0f);
    }

    private ShipSkillData savedShip() {
        ShipSkillData data = ShipSkillDataManager.get("ship");
        ShipTrees.chooseStartingRoot(data, declaredTree.get("root"));
        ShipTrees.allocate(data, saved("old_socket", socketType), 3);
        data.socketItem("old_socket", "subroutine-1");
        ShipTrees.allocate(data, saved("old_armor", plainType), 3);
        ItemCostLedger.recordPaidItemCost(data, "old_armor", LOBSTERS);
        data.addFreeAllocation();
        ShipTrees.allocate(data, saved("old_free", plainType), 3);
        ShipTrees.allocate(data, saved("old_paired", plainType), 3);
        data.pairGrantedNodeIds().add("old_paired");
        ShipTrees.selectOption(data, saved("old_choice", choiceType), new SkillType.Builder("old_option", "Old", "", SkillTier.SMALL).build(), 3);
        data.dormantNodeIds().add("old_dormant");
        return data;
    }

    private List<SkillItemCost> loadSave() {
        AffinityOptions.migrateAll(ShipSkillDataManager.all().values());
        ShipTreeRepairs.replaceRemovedNodes(declaredTree, NodeReplacements.all());
        Map<String, SkillNode> effectiveTree = new LinkedHashMap<>(declaredTree);
        effectiveTree.remove("new_dormant");
        List<SkillItemCost> refunds = new ArrayList<>();
        ShipTreeRepairs.forgetUnknownNodes(effectiveTree, declaredTree, types, shipId -> true, refunds::add);
        return refunds;
    }

    @Test
    void aSavedShipOnRenamedNodesLoadsWithItsAllocationsSocketsChargesOptionsAndFreeStateMovedOver() {
        ShipSkillData data = savedShip();
        int bankedBefore = data.getBankedFreeAllocations();

        List<SkillItemCost> refunds = loadSave();

        assertEquals(List.of(), refunds);
        assertEquals(List.of("root", "new_socket", "new_armor", "new_free", "new_paired", "new_choice"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(Set.of("new_dormant"), data.getDormantNodeIds());
        assertEquals(Map.of("new_socket", "subroutine-1"), data.getSocketedItems());
        assertEquals(LOBSTERS, ItemCostLedger.paidItemCost(data, "new_armor"));
        assertNull(ItemCostLedger.paidItemCost(data, "old_armor"));
        assertTrue(data.isFreeNode("new_free"));
        assertTrue(data.isFreeNode("new_paired"));
        assertFalse(data.isFreeNode("old_free"));
        assertEquals("new_option", data.getChoiceSelection("new_choice"));
        assertNull(data.getChoiceSelection("old_choice"));
        assertEquals(bankedBefore, data.getBankedFreeAllocations());
        assertEquals(3, ShipTrees.paidNodeCount(data));
    }

    @Test
    void aReplacedNodeStillDropsWhatWasSocketedInIt() {
        ShipSkillData data = ShipSkillDataManager.get("ship");
        ShipTrees.chooseStartingRoot(data, declaredTree.get("root"));
        ShipTrees.allocate(data, saved("replaced_socket", socketType), 3);
        data.socketItem("replaced_socket", "subroutine-1");

        loadSave();

        assertEquals(List.of("root", "new_socket"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void aRenameMovesAStartingRootAndGrantedNodesEvenWhenTheyAreNotAllocated() {
        ShipSkillData data = new ShipSkillData();
        data.startingRootId = "old_root";
        data.subroutineGrantedNodeIds().add("old_granted");
        data.socketedItems = new LinkedHashMap<>(Map.of("old_granted", "anchor-1"));

        assertTrue(ShipTrees.renameNode(data, "old_root", "root"));
        assertTrue(ShipTrees.renameNode(data, "old_granted", "granted"));
        assertFalse(ShipTrees.renameNode(data, "never_held", "held"));

        assertEquals("root", data.startingRootId);
        assertTrue(data.isGrantFree("granted"));
        assertEquals(Map.of("granted", "anchor-1"), data.getSocketedItems());
    }

    @Test
    void aRenameLeavesTheShipAloneWhenItAlreadyHoldsTheNewId() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, saved("old_armor", plainType), 3);
        ShipTrees.allocate(data, declaredTree.get("new_armor"), 3);

        assertFalse(ShipTrees.renameNode(data, "old_armor", "new_armor"));
        assertEquals(Set.of("old_armor", "new_armor"), data.getAllocatedNodeIds());
    }
}
