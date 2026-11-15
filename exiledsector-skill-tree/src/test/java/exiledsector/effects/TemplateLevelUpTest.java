package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.persistence.InMemoryTemplateFile;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ClearedSkillTreeExtension;
import exiledsector.skills.LogisticsNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateStep;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.skills.TestTrees.node;
import static exiledsector.skills.TestTrees.registered;
import static exiledsector.skills.TestTrees.small;
import static exiledsector.skills.TestTrees.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ClearedSkillTreeExtension.class)
class TemplateLevelUpTest {

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private FleetMemberAPI member;
    private ShipSkillData shipData;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat(anyString(), anyString())).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        InMemoryTemplateFile.install();

        SkillNode root = node("root", SkillTier.ROOT);
        small("first", "root");
        node("pricey", type("pricey_type", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build(), "first");
        small("second", "first");
        small("third", "second");
        node("cargo", type("cargo_type", SkillTier.NOTABLE).tags(List.of(LogisticsNodes.TAG)).build(), "root");

        member = ShipTreeSyncTest.mockMember("planned", true);
        when(member.getHullSpec().getHullSize()).thenReturn(HullSize.FRIGATE);
        shipData = ShipSkillDataManager.get("planned");
        ShipTrees.chooseStartingRoot(shipData, root);
    }

    @AfterEach
    void tearDown() {
        SkillTreeTemplateStore.useCommonFolder();
        mockSector.close();
        lunaSettingsMock.close();
    }

    private void assignTemplate(String... nodeIds) {
        List<TemplateStep> steps = Arrays.stream(nodeIds).map(nodeId -> new TemplateStep(nodeId, null)).toList();
        SkillTreeTemplate template = SkillTreeTemplateStore.save("plan", "root", HullSize.FRIGATE, steps);
        SkillTreeTemplateStore.assign("planned", template.id());
    }

    private void levelUp(int levels) {
        ShipLevelSystem.raiseToLevel(shipData, shipData.getLevel() + levels, SkillTree.getAllNodes().values());
    }

    @Test
    void aTemplateSocketTakesItsAnchorFromStorageOnceTheLevelUpAllocatesIt() throws Exception {
        node("socket", SkillTier.SOCKET, "first");
        node("abyss_exit", SkillTier.WORMHOLE_EXIT);
        WormholeAnchorDefinitions.register(new JSONArray().put(new JSONObject().put("id", "wormhole_anchor_abyss").put("volume", "abyss")
                .put("exit_node", "abyss_exit").put("name", "Abyss Wormhole Anchor").put("description", "Opens a wormhole.")));
        try {
            Item anchor = ItemStore.get().add(ItemDefinitions.get("wormhole_anchor_abyss"), 1L);
            List<TemplateStep> steps = List.of(new TemplateStep("first", null), new TemplateStep("socket", null));
            SkillTreeTemplate template = SkillTreeTemplateStore.save("plan", "root", HullSize.FRIGATE, steps, Map.of("socket", "wormhole_anchor_abyss"));
            SkillTreeTemplateStore.assign("planned", template.id());
            shipData.setAutoAllocateTemplate(true);

            levelUp(2);
            assertEquals(2, TemplateLevelUp.spendFreeAllocations(member));

            assertEquals(anchor.id(), shipData.getSocketedItem("socket"));
        } finally {
            WormholeAnchorDefinitions.clear();
            ItemDefinitions.clear();
        }
    }

    @Test
    void eachLevelUpAllocatesTheNextTemplateNodeForFree() {
        assignTemplate("first", "second", "third");
        shipData.setAutoAllocateTemplate(true);

        levelUp(2);
        assertEquals(2, TemplateLevelUp.spendFreeAllocations(member));

        assertEquals(Set.of("root", "first", "second"), shipData.getAllocatedNodeIds());
        assertTrue(shipData.isFreeNode("first"));
        assertTrue(shipData.isFreeNode("second"));
        assertEquals(0, shipData.getBankedFreeAllocations());
        verify(member).setStatUpdateNeeded(true);
    }

    @Test
    void aTemplateFromAnotherRootIsReachedByTheShortestRouteFirst() {
        List<TemplateStep> steps = List.of(new TemplateStep("second", null), new TemplateStep("third", null));
        SkillTreeTemplateStore.assign("planned", SkillTreeTemplateStore.save("plan", "other_root", HullSize.FRIGATE, steps).id());
        shipData.setAutoAllocateTemplate(true);

        levelUp(3);
        assertEquals(3, TemplateLevelUp.spendFreeAllocations(member));

        assertEquals(Set.of("root", "first", "second", "third"), shipData.getAllocatedNodeIds());
        assertEquals(0, shipData.getBankedFreeAllocations());
    }

    @Test
    void noRouteIsBuiltTowardAStepWhoseChosenOptionThisShipCannotTake() {
        registered(type("capital_option", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build());
        registered(type("frigate_option", SkillTier.SMALL).build());
        node("choice", type("choice_type", SkillTier.SMALL).choiceOptionIds(List.of("capital_option", "frigate_option")).build(), "second");
        List<TemplateStep> steps = List.of(new TemplateStep("choice", "capital_option"));
        SkillTreeTemplateStore.assign("planned", SkillTreeTemplateStore.save("plan", "other_root", HullSize.FRIGATE, steps).id());
        shipData.setAutoAllocateTemplate(true);

        levelUp(2);
        TemplateLevelUp.spendFreeAllocations(member);

        assertEquals(Set.of("root"), shipData.getAllocatedNodeIds());
    }

    @Test
    void aTemplateFromItsOwnRootNeverAllocatesNodesOutsideIt() {
        assignTemplate("second", "third");
        shipData.setAutoAllocateTemplate(true);

        levelUp(2);
        TemplateLevelUp.spendFreeAllocations(member);

        assertEquals(Set.of("root"), shipData.getAllocatedNodeIds());
    }

    @Test
    void itemCostNodesAreLeftForThePlayerAndUnusableLevelsStayBanked() {
        assignTemplate("first", "pricey", "second");
        shipData.setAutoAllocateTemplate(true);

        levelUp(3);
        assertEquals(2, TemplateLevelUp.spendFreeAllocations(member));

        assertFalse(shipData.isAllocated("pricey"));
        assertEquals(Set.of("root", "first", "second"), shipData.getAllocatedNodeIds());
        assertEquals(1, shipData.getBankedFreeAllocations());
    }

    @Test
    void withTheToggleOffLevelUpsRefundTheMostRecentNodeAsBefore() {
        assignTemplate("first", "second");
        ShipTrees.allocate(shipData, SkillTree.get("first"), 1);

        levelUp(1);
        assertEquals(0, TemplateLevelUp.spendFreeAllocations(member));

        assertTrue(shipData.isFreeNode("first"));
        assertFalse(shipData.isAllocated("second"));
        verify(member, never()).setStatUpdateNeeded(true);
    }

    @Test
    void aLevelUpTheTemplateCannotUseStillRefundsTheLatestNodeAsBefore() {
        ShipTrees.allocate(shipData, SkillTree.get("first"), 1);
        shipData.setAutoAllocateTemplate(true);

        levelUp(1);
        assertEquals(1, shipData.getBankedFreeAllocations());
        assertEquals(0, TemplateLevelUp.spendFreeAllocations(member));

        assertTrue(shipData.isFreeNode("first"));
        assertEquals(0, shipData.getBankedFreeAllocations());
    }

    @Test
    void freeAllocationsBankedByHandAreSpentButNeverRefundedAsOp() {
        assignTemplate("first", "second");
        shipData.setAutoAllocateTemplate(true);
        shipData.addFreeAllocation();
        shipData.addFreeAllocation();
        shipData.addFreeAllocation();

        assertEquals(2, TemplateLevelUp.spendFreeAllocations(member));

        assertEquals(1, shipData.getBankedFreeAllocations());
    }

    @Test
    void levelsChangedReportsHowManyNodesEachShipAllocated() {
        assignTemplate("first", "second", "third");
        shipData.setAutoAllocateTemplate(true);
        levelUp(1);

        Map<FleetMemberAPI, Integer> counts = ShipTreeSync.levelsChanged(null, List.of(member));

        assertEquals(Map.of(member, 1), counts);
        assertTrue(shipData.isAllocated("first"));
    }

    @Test
    void aLevelUpWaitingOnlyForDockingKeepsItsFreeAllocationForTheDock() {
        ShipTrees.allocate(shipData, SkillTree.get("first"), 1);
        assignTemplate("first", "cargo");
        shipData.setAutoAllocateTemplate(true);

        levelUp(1);
        assertEquals(0, TemplateLevelUp.spendFreeAllocations(member));

        assertFalse(shipData.isAllocated("cargo"));
        assertFalse(shipData.isFreeNode("first"));
        assertEquals(1, shipData.getBankedFreeAllocations());

        assertEquals(0, TemplateLevelUp.spendBankedWhileDocked(member, false));
        assertEquals(1, TemplateLevelUp.spendBankedWhileDocked(member, true));

        assertTrue(shipData.isAllocated("cargo"));
        assertTrue(shipData.isFreeNode("cargo"));
        assertEquals(0, shipData.getBankedFreeAllocations());
    }

    @Test
    void dockingDoesNothingWithAutoAllocateOffOrNoFreeAllocations() {
        assignTemplate("first");
        shipData.addFreeAllocation();
        assertEquals(0, TemplateLevelUp.spendBankedWhileDocked(member, true));

        shipData.setAutoAllocateTemplate(true);
        assertEquals(1, TemplateLevelUp.spendBankedWhileDocked(member, true));
        assertEquals(0, TemplateLevelUp.spendBankedWhileDocked(member, true));
        assertTrue(shipData.isAllocated("first"));
    }
}
