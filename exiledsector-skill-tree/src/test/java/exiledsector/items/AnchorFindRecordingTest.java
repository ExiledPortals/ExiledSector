package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.effects.SkillTreeLootSource;
import exiledsector.loot.BattleLoot;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.unlock.AnchorFinds;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.RelockedGates;
import exiledsector.skills.unlock.UnlockConditionType;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnchorFindRecordingTest {

    private static final String NIGHTCROSS_ANCHOR = "wormhole_anchor_nightcross";
    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String NIGHTCROSS = "nightcross";

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private final Map<String, Object> memoryValues = new HashMap<>();
    private final Map<String, Object> persistentData = new HashMap<>();
    private SectorAPI sector;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        memoryValues.put(JanusDevice.FLAG, true);
        when(sectorMemory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(memoryValues.get(call.<String>getArgument(0))));
        doAnswer(call -> memoryValues.put(call.getArgument(0), call.getArgument(1))).when(sectorMemory).set(anyString(), any());
        sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(true);
        when(settings.getModManager()).thenReturn(modManager);
        globalMock.when(Global::getSettings).thenReturn(settings);
        NodeGates.register(new JSONObject("{\"gate_found\": {\"anchors\": \"free\", \"unlockConditions\": [{\"type\": \"anchorFound\", \"volume\": \""
                + NIGHTCROSS + "\"}]}}"));

        SkillTree.clearNodes();
        SkillType exitType = new SkillType.Builder("wormhole_exit", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        SkillTree.register(new SkillNode("nightcross_wormhole_1", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(NIGHTCROSS, "gate_found")));
        SkillTree.register(new SkillNode("hegemony_2", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony")));
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(NIGHTCROSS_ANCHOR, NIGHTCROSS, "nightcross_wormhole_1").put("drop_factions", "hegemony"))
                .put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2")));
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}]"));
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

    private static JSONObject anchorRow(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "Opens a wormhole.")
                .put("drop_chance", "1");
    }

    private static ItemData anchor(String definitionId) {
        return ItemData.rolled(ItemDefinitions.get(definitionId), 5L);
    }

    private boolean nightcrossFound() {
        return AnchorFinds.isFound(sector, NIGHTCROSS);
    }

    @Test
    void aFreeAnchorDropsWhileItsGateIsClosedAndDroppingItOpensTheGate() {
        assertTrue(WormholeAnchorDefinitions.obtainable(NIGHTCROSS_ANCHOR, sector));
        assertTrue(WormholeAnchor.inertness(NIGHTCROSS_ANCHOR, sector) == WormholeAnchor.Inertness.GATED);
        BattleLoot battle = new BattleLoot(new Random(3L), new Random(4L));
        battle.setPlayerWon(true);
        Map<String, Float> defeated = battle.state(SkillTreeLootSource.INSTANCE, LinkedHashMap::new);
        defeated.put("hegemony", 100000f);

        for (int attempt = 0; attempt < 20 && !nightcrossFound(); attempt++) {
            SkillTreeLootSource.INSTANCE.addEncounterLoot(battle, mock(CargoAPI.class));
        }

        assertTrue(nightcrossFound());
        assertTrue(WormholeAnchor.inertness(NIGHTCROSS_ANCHOR, sector) == null);
    }

    @Test
    void deliveredBattleLootRecordsTheFindToo() {
        BattleLoot battle = new BattleLoot(new Random(3L), new Random(4L));
        battle.setPlayerWon(true);
        battle.<Map<String, Float>>state(SkillTreeLootSource.INSTANCE, LinkedHashMap::new).put("hegemony", 100000f);
        CargoAPI playerCargo = mock(CargoAPI.class);

        for (int attempt = 0; attempt < 20 && !nightcrossFound(); attempt++) {
            SkillTreeLootSource.INSTANCE.deliverBattleLoot(battle, playerCargo);
        }

        assertTrue(nightcrossFound());
    }

    @Test
    void buyingAnAnchorRecordsItButOtherItemsDoNot() {
        SkillTreeLootSource.INSTANCE.itemsBought(List.of(anchor(HEGEMONY_ANCHOR)));
        assertFalse(nightcrossFound());
        assertTrue(AnchorFinds.isFound(sector, "hegemony"));

        SkillTreeLootSource.INSTANCE.itemsBought(List.of(anchor(NIGHTCROSS_ANCHOR)));
        assertTrue(nightcrossFound());
    }

    @Test
    void showcaseSalvageRecordsTheAnchorsItHandsOut() {
        SkillTreeLootSource.INSTANCE.addShowcaseLoot(mock(CargoAPI.class), new Random(3L));

        assertTrue(nightcrossFound());
    }

    @Test
    void aNewFindReopensARelockedGateButTheLoadMigrationDoesNot() {
        AnchorFinds.markFound(sector, NIGHTCROSS, "test");
        RelockedGates.relockOpenAreas(sector);
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.ANCHOR_FOUND));

        AnchorFinds.markFound(sector, NIGHTCROSS, "load migration");
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.ANCHOR_FOUND));

        AnchorFinds.record(sector, "hegemony", "an anchor whose area has no anchorFound gate");
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.ANCHOR_FOUND));

        AnchorFinds.record(sector, NIGHTCROSS, "found again");
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.ANCHOR_FOUND));
    }

    @Test
    void theLoadMigrationRecordsHeldAnchorsButNotMarketListings() {
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI listed = specialStack(anchor(NIGHTCROSS_ANCHOR).toMarketListing());
        CargoStackAPI held = specialStack(anchor(HEGEMONY_ANCHOR).toSpecialItem());
        when(cargo.getStacksCopy()).thenReturn(List.of(listed, held));
        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(mock(StarSystemAPI.class));

        AnchorFindMigration.recordHeldAnchors(sector, List.of(cargo), List.of());

        assertFalse(nightcrossFound());
        assertTrue(AnchorFinds.isFound(sector, "hegemony"));

        AnchorFindMigration.recordHeldAnchors(sector, List.of(), List.of(anchor(NIGHTCROSS_ANCHOR).create("socketable_1")));

        assertTrue(nightcrossFound());
    }

    private static CargoStackAPI specialStack(SpecialItemData special) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.isSpecialStack()).thenReturn(true);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        return stack;
    }
}
