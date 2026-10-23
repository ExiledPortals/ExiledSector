package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.effects.SkillTreeLootSource;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.UnlockCondition;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnchorObtainabilityTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String ABYSS_ANCHOR = "wormhole_anchor_abyss";
    private static final String NIGHTCROSS_ANCHOR = "wormhole_anchor_nightcross";
    private static final String KESTEVEN_ANCHOR = "wormhole_anchor_kesteven";
    private static final String TRITACHYON_ANCHOR = "wormhole_anchor_tritachyon";
    private static final String BEACON = "wormhole_anchor_exotech";
    private static final String WITHHELD_GATE_FLAG = "$test_abyssOpen";
    private static final String FREE_GATE_FLAG = "$test_nightcrossOpen";
    private static final String EXIT_TYPE_FLAG = "$test_exitTypeOpen";

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private MemoryAPI sectorMemory;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.getBoolean(JanusDevice.FLAG)).thenReturn(true);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(mock(ModManagerAPI.class));
        globalMock.when(Global::getSettings).thenReturn(settings);
        NodeGates.register(new JSONObject("{\"gate_withheld\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + WITHHELD_GATE_FLAG
                + "\"}]}, \"gate_free\": {\"anchors\": \"free\", \"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FREE_GATE_FLAG + "\"}]}}"));

        SkillTree.clearNodes();
        SkillType exitType = new SkillType.Builder("wormhole_exit", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        SkillType lockedExitType = new SkillType.Builder("wormhole_exit_locked", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT)
                .unlockConditions(List.of(UnlockCondition.memoryFlag(EXIT_TYPE_FLAG))).build();
        for (SkillNode node : List.of(
                exit("hegemony_2", exitType, "hegemony"),
                exit("rat_abyss_wormhole_1", exitType, "abyss", "gate_withheld"),
                exit("nightcross_wormhole_1", exitType, "nightcross", "gate_free"),
                exit("tritachyon_2", lockedExitType, "tritachyon"),
                exit("exoship", exitType, SkillTags.EXOTECH_REGION))) {
            SkillTree.register(node);
        }
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2"))
                .put(anchorRow(ABYSS_ANCHOR, "abyss", "rat_abyss_wormhole_1").put("drop_factions", "hegemony"))
                .put(anchorRow(NIGHTCROSS_ANCHOR, "nightcross", "nightcross_wormhole_1").put("drop_factions", "hegemony"))
                .put(anchorRow(KESTEVEN_ANCHOR, "kesteven", "kesteven_wormhole1").put("drop_factions", "hegemony"))
                .put(anchorRow(TRITACHYON_ANCHOR, "tritachyon", "tritachyon_2").put("drop_factions", "hegemony"))
                .put(anchorRow(BEACON, SkillTags.EXOTECH_REGION, "exoship").put("drop_factions", "hegemony")
                        .put("unlock", ItemUnlock.RAT_PLAYER_EXOSHIP.id())));
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}, {\"faction\": \"rat_abyssals\", \"region\": \"abyss\"}]"));
    }

    @AfterEach
    void tearDown() throws Exception {
        NpcFactionVolumes.clear();
        WormholeAnchorDefinitions.clear();
        SkillTree.clearNodes();
        NodeGates.register(new JSONObject("{}"));
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static SkillNode exit(String id, SkillType type, String region, String... gateTags) {
        List<String> tags = new java.util.ArrayList<>(List.of(region));
        tags.addAll(List.of(gateTags));
        return new SkillNode(id, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, tags);
    }

    private static JSONObject anchorRow(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "Opens a wormhole.")
                .put("drop_chance", "1").put("market_chance", "1");
    }

    private static boolean obtainable(String definitionId) {
        return WormholeAnchorDefinitions.obtainable(definitionId, Global.getSector());
    }

    private static Set<String> droppedIds() {
        Random random = new Random(3L);
        Set<String> droppedIds = new HashSet<>();
        for (int battle = 0; battle < 200; battle++) {
            FactionFleetDrops.roll(Map.of("hegemony", 10000f), random, FactionFleetDrops::available)
                    .forEach(item -> droppedIds.add(item.definition().id()));
        }
        return droppedIds;
    }

    private static Set<String> stockedIds() {
        Set<String> stockedIds = new HashSet<>();
        WormholeAnchorDefinitions.rollMarketStock("hegemony", new Random(3L)).forEach(definition -> stockedIds.add(definition.id()));
        return stockedIds;
    }

    @Test
    void anAnchorIsObtainableOnlyWhenNothingButAFreeGateHoldsItsExit() {
        assertTrue(obtainable(HEGEMONY_ANCHOR));
        assertTrue(obtainable(NIGHTCROSS_ANCHOR));
        assertFalse(obtainable(ABYSS_ANCHOR));
        assertFalse(obtainable(KESTEVEN_ANCHOR));
        assertFalse(obtainable(TRITACHYON_ANCHOR));
        assertFalse(obtainable(BEACON));
        assertEquals(WormholeAnchor.Inertness.GATED, WormholeAnchor.inertness(NIGHTCROSS_ANCHOR, Global.getSector()));
    }

    @Test
    void aWithheldAnchorBecomesObtainableOnceItsGateOpensAndTheExitTypeOnceItUnlocks() {
        when(sectorMemory.getBoolean(WITHHELD_GATE_FLAG)).thenReturn(true);
        when(sectorMemory.getBoolean(EXIT_TYPE_FLAG)).thenReturn(true);

        assertTrue(obtainable(ABYSS_ANCHOR));
        assertTrue(obtainable(TRITACHYON_ANCHOR));
    }

    @Test
    void battleDropsAndMarketsSkipAWithheldAnchorAndKeepAFreeOne() {
        Set<String> expected = Set.of(HEGEMONY_ANCHOR, NIGHTCROSS_ANCHOR);

        assertEquals(expected, droppedIds());
        assertEquals(expected, stockedIds());

        when(sectorMemory.getBoolean(WITHHELD_GATE_FLAG)).thenReturn(true);

        assertTrue(droppedIds().contains(ABYSS_ANCHOR));
        assertTrue(stockedIds().contains(ABYSS_ANCHOR));
    }

    @Test
    void showcaseSalvageOffersOnlyObtainableAnchors() {
        CargoAPI loot = mock(CargoAPI.class);
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);

        SkillTreeLootSource.INSTANCE.addShowcaseLoot(loot, new Random(3L));

        verify(loot, atLeastOnce()).addSpecial(added.capture(), anyFloat());
        Set<String> anchorIds = new HashSet<>();
        added.getAllValues().stream().map(ItemData::of)
                .filter(item -> item != null && item.definition() != null && item.definition().type() == ItemType.WORMHOLE_ANCHOR)
                .forEach(item -> anchorIds.add(item.definition().id()));
        assertEquals(Set.of(HEGEMONY_ANCHOR, NIGHTCROSS_ANCHOR), anchorIds);
    }

    @Test
    void npcTreesStillTakeTheAnchorOfAGatedArea() {
        assertNotNull(NpcItems.anchorFor("abyss"));
    }
}
