package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateStep;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateLevelUpTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private FleetMemberAPI member;
    private ShipSkillData shipData;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat(anyString(), anyString())).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);

        SkillTree.clearNodes();
        SkillNode root = node("root", type("root_type", SkillTier.ROOT).build(), List.of());
        node("first", type("first_type", SkillTier.SMALL).build(), List.of("root"));
        node("pricey", type("pricey_type", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build(), List.of("first"));
        node("second", type("second_type", SkillTier.SMALL).build(), List.of("first"));
        node("third", type("third_type", SkillTier.SMALL).build(), List.of("second"));

        member = ShipTreeSyncTest.mockMember("planned", true);
        when(member.getHullSpec().getHullSize()).thenReturn(HullSize.FRIGATE);
        shipData = ShipSkillDataManager.get("planned");
        ShipTrees.chooseStartingRoot(shipData, root);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
    }

    private static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).effects(List.of());
    }

    private static SkillNode node(String id, SkillType type, List<String> connectedIds) {
        SkillNode node = new SkillNode(id, type, connectedIds, 0f, 0f);
        SkillTree.register(node);
        return node;
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
    void creditsBankedByHandAreSpentButNeverRefundedAsOp() {
        assignTemplate("first", "second");
        shipData.setAutoAllocateTemplate(true);
        shipData.addFreeAllocationCredit();
        shipData.addFreeAllocationCredit();
        shipData.addFreeAllocationCredit();

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
}
