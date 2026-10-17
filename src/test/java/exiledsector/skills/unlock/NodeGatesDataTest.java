package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.compat.NightcrossCompat;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.skills.tags.SkillTags;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeGatesDataTest {

    private static final String ABYSS_GATE = "gate_singularity_defeated";
    private static final String PASCAL_GATE = "gate_pascal_visited";
    private static final String LUNAR_FLEET_GATE = "gate_lunar_fleet_defeated";
    private static final String PROTOTYPE_GATE = "gate_ancient_prototype_found";
    private static final String CANDLES_GRACE_NODE_ID = "gravitational_drag_1";

    @BeforeAll
    static void loadRealData() throws Exception {
        RealSkillData.load();
        NodeGates.register(RealSkillData.readJson(RealSkillData.projectRoot().resolve(NodeGates.DATA_PATH)));
    }

    @AfterAll
    static void clear() throws Exception {
        RealSkillData.clear();
        NodeGates.register(new org.json.JSONObject("{}"));
    }

    @Test
    void everyGateTagInTheVocabularyHasConditionsAndNoConfiguredGateIsUnknown() {
        assertEquals(Set.copyOf(SkillTags.GATE), NodeGates.all().keySet());
        NodeGates.all().forEach((gateTag, conditions) -> assertTrue(!conditions.isEmpty(), gateTag));
    }

    @Test
    void theAbyssGateWaitsForTheSingularityToBeDefeated() {
        assertEquals(List.of(UnlockConditionType.RAT_SINGULARITY_DEFEATED),
                NodeGates.all().get(ABYSS_GATE).stream().map(UnlockCondition::getType).toList());
    }

    @Test
    void everyAbyssNodeAndTheWormholeIntoItAreGated() {
        List<String> ungated = SkillTree.getAllNodes().values().stream()
                .filter(node -> SkillTags.ABYSS_REGION.equals(node.getRegion()) || leadsIntoTheAbyss(node))
                .filter(node -> !node.getTags().contains(ABYSS_GATE))
                .map(SkillNode::getId).toList();

        assertTrue(ungated.isEmpty(), "Abyss nodes without the gate: " + ungated);
    }

    @Test
    void theNightcrossGatesWaitForPascalTheLunarFleetAndAnAncientPrototype() {
        assertEquals(List.of(UnlockConditionType.NIGHTCROSS_PASCAL_VISITED),
                NodeGates.all().get(PASCAL_GATE).stream().map(UnlockCondition::getType).toList());
        assertEquals(List.of(UnlockConditionType.NIGHTCROSS_LUNAR_FLEET_DEFEATED),
                NodeGates.all().get(LUNAR_FLEET_GATE).stream().map(UnlockCondition::getType).toList());
        assertEquals(List.of(UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND),
                NodeGates.all().get(PROTOTYPE_GATE).stream().map(UnlockCondition::getType).toList());
    }

    @Test
    void everyNightcrossNodeAndTheWormholeIntoItWaitForPascal() {
        assertTrue(ungatedIn("nightcross", PASCAL_GATE).isEmpty(), "Nightcross nodes without the gate: " + ungatedIn("nightcross", PASCAL_GATE));
    }

    @Test
    void everyStargazerNodeAndTheWormholeIntoItWaitForTheLunarFleetExceptCandlesGrace() {
        assertTrue(ungatedIn("stargazer", LUNAR_FLEET_GATE).isEmpty(), "Stargazer nodes without the gate: " + ungatedIn("stargazer", LUNAR_FLEET_GATE));
    }

    @Test
    void candlesGraceWaitsOnlyForAnAncientPrototype() {
        List<String> gates = SkillTree.get(CANDLES_GRACE_NODE_ID).getTags().stream().filter(SkillTags.GATE::contains).toList();
        assertEquals(List.of(PROTOTYPE_GATE), gates);
    }

    @Test
    void withoutNightcrossInstalledNoNightcrossOrStargazerNodeIsLocked() {
        assertEquals(List.of(), lockedNightcrossNodes(false));
    }

    @Test
    void withNightcrossInstalledAFreshSaveLocksEveryNightcrossAndStargazerNode() {
        List<String> areaNodes = SkillTree.getAllNodes().values().stream()
                .filter(node -> AreaToggles.NIGHTCROSS_REGIONS.contains(node.getRegion())).map(SkillNode::getId).sorted().toList();
        assertEquals(areaNodes, lockedNightcrossNodes(true));
    }

    private static List<String> lockedNightcrossNodes(boolean nightcrossInstalled) {
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(nightcrossInstalled);
        MemoryAPI emptyMemory = mock(MemoryAPI.class);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(emptyMemory);
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(emptyMemory);
        when(sector.getStarSystem(anyString())).thenReturn(mock(StarSystemAPI.class));
        try (MockedStatic<LunaSettings> lunaSettings = Mockito.mockStatic(LunaSettings.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            lunaSettings.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
            global.when(Global::getSettings).thenReturn(settings);
            global.when(Global::getSector).thenReturn(sector);
            return SkillTree.getAllNodes().values().stream()
                    .filter(node -> AreaToggles.NIGHTCROSS_REGIONS.contains(node.getRegion()))
                    .filter(node -> NodeGates.isLocked(node, null))
                    .map(SkillNode::getId).sorted().toList();
        }
    }

    private static List<String> ungatedIn(String region, String gateTag) {
        return SkillTree.getAllNodes().values().stream()
                .filter(node -> !node.getId().equals(CANDLES_GRACE_NODE_ID))
                .filter(node -> region.equals(node.getRegion()) || leadsInto(node, region))
                .filter(node -> !node.getTags().contains(gateTag))
                .map(SkillNode::getId).toList();
    }

    private static boolean leadsInto(SkillNode node, String region) {
        SkillNode paired = node.getPairedNodeId() == null ? null : SkillTree.get(node.getPairedNodeId());
        return paired != null && region.equals(paired.getRegion());
    }

    @Test
    void everyAbyssNotableAndKeystoneCostsTwentyFiveAbyssalMatter() {
        SkillTree.getAllNodes().values().stream()
                .filter(node -> SkillTags.ABYSS_REGION.equals(node.getRegion()))
                .filter(node -> node.getType().getTier() == SkillTier.NOTABLE || node.getType().getTier() == SkillTier.KEYSTONE)
                .filter(node -> !node.getType().getId().equals("chronos_adaptability"))
                .forEach(node -> {
                    assertEquals("rat_abyssal_matter", node.getType().getItemCost().itemId(), node.getId());
                    assertEquals(25f, node.getType().getItemCost().quantity(), node.getId());
                });
    }

    @Test
    void chronosRhythmsCostsAChronosCore() {
        SkillItemCost itemCost = SkillTree.getAllNodes().get("chronos_adaptability_1").getType().getItemCost();
        assertEquals("rat_chronos_core", itemCost.itemId());
        assertEquals(1f, itemCost.quantity());
    }

    private static boolean leadsIntoTheAbyss(SkillNode node) {
        SkillNode paired = node.getPairedNodeId() == null ? null : SkillTree.get(node.getPairedNodeId());
        return paired != null && SkillTags.ABYSS_REGION.equals(paired.getRegion());
    }
}
