package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.SectorManager;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import exiledlib.ProjectFiles;
import exiledsector.compat.NexerelinCompat;
import exiledsector.effects.SkillTreeLootSource;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.unlock.NodeGates;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FactionlessAnchorsTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String KESTEVEN_ANCHOR = "wormhole_anchor_kesteven";
    private static final String ENIGMA_ANCHOR = "wormhole_anchor_enigma";
    private static final String SITE = "station_research";

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private final Set<String> existingFactionIds = new HashSet<>(Set.of("hegemony", "luddic_church"));
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);
    private SectorAPI sector;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.getBoolean(JanusDevice.FLAG)).thenReturn(true);
        sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getFaction(anyString())).thenAnswer(call -> existingFactionIds.contains(call.<String>getArgument(0)) ? mock(FactionAPI.class) : null);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getScriptClassLoader()).thenReturn(FactionlessAnchorsTest.class.getClassLoader());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        globalMock.when(Global::getSettings).thenReturn(settings);
        NodeGates.register(new JSONObject("{}"));

        SkillTree.clearNodes();
        SkillType exitType = new SkillType.Builder("wormhole_exit", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        for (String exit : List.of("hegemony_2", "kesteven_wormhole1", "enigma_wormhole1")) {
            SkillTree.register(new SkillNode(exit, exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(exit.split("_")[0])));
        }
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2"))
                .put(anchorRow(KESTEVEN_ANCHOR, "kesteven", "kesteven_wormhole1").put("drop_factions", "kesteven"))
                .put(anchorRow(ENIGMA_ANCHOR, "enigma", "enigma_wormhole1").put("drop_factions", "enigma;luddic_church")));
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}, {\"faction\": \"kesteven\", \"region\": \"kesteven\"}]"));
        FactionlessAnchors.register(new JSONArray().put(new JSONObject().put("site", SITE).put("chance", "1"))
                .put(new JSONObject().put("site", "derelict_probe").put("chance", "0")));
    }

    @AfterEach
    void tearDown() throws Exception {
        FactionlessAnchors.clear();
        NpcFactionVolumes.clear();
        WormholeAnchorDefinitions.clear();
        SkillTree.clearNodes();
        SectorManager.LIVE_FACTION_IDS.clear();
        NexConfig.CONFIGS_BY_FACTION.clear();
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static JSONObject anchorRow(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "Opens a wormhole.")
                .put("drop_chance", "0.1").put("market_chance", "0.3");
    }

    private Set<String> salvagedIds(String siteId) {
        Random random = new Random(3L);
        Set<String> salvagedIds = new HashSet<>();
        for (int site = 0; site < 100; site++) {
            ItemData anchor = FactionlessAnchors.roll(siteId, random, sector);
            if (anchor != null) {
                salvagedIds.add(anchor.definition().id());
            }
        }
        return salvagedIds;
    }

    @Test
    void anAnchorIsFactionlessOnlyWhenEveryFactionThatDropsItIsGone() {
        assertFalse(FactionlessAnchors.factionless(HEGEMONY_ANCHOR, sector));
        assertTrue(FactionlessAnchors.factionless(KESTEVEN_ANCHOR, sector));
        assertFalse(FactionlessAnchors.factionless(ENIGMA_ANCHOR, sector));

        existingFactionIds.remove("luddic_church");

        assertTrue(FactionlessAnchors.factionless(ENIGMA_ANCHOR, sector));
    }

    @Test
    void aPlayableFactionNexerelinHasEliminatedCountsAsGoneButANonPlayableOneNeverDoes() {
        when(modManager.isModEnabled(NexerelinCompat.MOD_ID)).thenReturn(true);
        NexFactionConfig playable = new NexFactionConfig();
        NexFactionConfig unplayable = new NexFactionConfig();
        unplayable.playableFaction = false;
        NexConfig.CONFIGS_BY_FACTION.put("hegemony", playable);
        NexConfig.CONFIGS_BY_FACTION.put("luddic_church", unplayable);

        assertTrue(FactionlessAnchors.factionless(HEGEMONY_ANCHOR, sector));
        assertFalse(FactionlessAnchors.factionless(ENIGMA_ANCHOR, sector));

        SectorManager.LIVE_FACTION_IDS.add("hegemony");

        assertFalse(FactionlessAnchors.factionless(HEGEMONY_ANCHOR, sector));
    }

    @Test
    void anUnplayableAuxiliaryDoesNotKeepAnEliminatedFactionsAnchorAlive() throws Exception {
        when(modManager.isModEnabled(NexerelinCompat.MOD_ID)).thenReturn(true);
        existingFactionIds.add("lions_guard");
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}, {\"faction\": \"lions_guard\", \"region\": \"hegemony\"}]"));
        NexFactionConfig auxiliary = new NexFactionConfig();
        auxiliary.playableFaction = false;
        NexConfig.CONFIGS_BY_FACTION.put("hegemony", new NexFactionConfig());
        NexConfig.CONFIGS_BY_FACTION.put("lions_guard", auxiliary);

        assertTrue(FactionlessAnchors.factionless(HEGEMONY_ANCHOR, sector));
    }

    @Test
    void techMiningDecayLowersTheAnchorChance() {
        assertNull(FactionlessAnchors.roll(SITE, new Random(3L), sector, 0f));
    }

    @Test
    void salvageDropsOnlyFactionlessAnchorsAndOnlyAtListedSites() {
        assertEquals(Set.of(KESTEVEN_ANCHOR), salvagedIds(SITE));
        assertEquals(Set.of(), salvagedIds("derelict_probe"));
        assertEquals(Set.of(), salvagedIds("wreck"));

        existingFactionIds.add("kesteven");

        assertEquals(Set.of(), salvagedIds(SITE));
    }

    @Test
    void aSiteDropsAtMostOneAnchor() {
        existingFactionIds.remove("luddic_church");
        CargoAPI loot = mock(CargoAPI.class);
        SectorEntityToken site = mock(SectorEntityToken.class);
        when(site.getCustomEntityType()).thenReturn(SITE);
        when(site.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);

        try (MockedStatic<Misc> miscMock = Mockito.mockStatic(Misc.class)) {
            miscMock.when(() -> Misc.getSalvageSeed(any())).thenReturn(42L);
            SkillTreeLootSource.INSTANCE.addSalvageLoot(sector, site, loot);
        }

        verify(loot).addSpecial(added.capture(), anyFloat());
        assertTrue(Set.of(KESTEVEN_ANCHOR, ENIGMA_ANCHOR).contains(ItemData.of(added.getValue()).definition().id()));
    }

    @Test
    void techMiningAddsAFactionlessAnchorToItsFinds() throws Exception {
        TechMiningItemListener.Find find = new TechMiningItemListener.Find(sector, null, 0, "techmining_first_find", 1f, new Random(3L),
                new ArrayList<>(), new ArrayList<>());
        FactionlessAnchors.register(new JSONArray().put(new JSONObject().put("site", "techmining_first_find").put("chance", "1")));

        SkillTreeLootSource.INSTANCE.addTechMiningFinds(find);

        assertEquals(List.of(KESTEVEN_ANCHOR), find.items().stream().map(item -> item.definition().id()).toList());
    }

    @Test
    void noAnchorWhenTheSiteRollMisses() throws Exception {
        FactionlessAnchors.register(new JSONArray().put(new JSONObject().put("site", SITE).put("chance", "0.0001")));

        assertNull(FactionlessAnchors.roll(SITE, new Random(3L), sector));
    }

    @Test
    void everyAnchorSalvageSiteIsARealSalvageSite() throws Exception {
        Set<String> salvageSites = sitesIn("data/config/exiledSector/item_salvage.csv");
        Set<String> anchorSites = sitesIn(FactionlessAnchors.DATA_PATH);

        assertFalse(anchorSites.isEmpty());
        assertTrue(salvageSites.containsAll(anchorSites), "Unknown sites: " + anchorSites.stream().filter(site -> !salvageSites.contains(site)).toList());
    }

    private static Set<String> sitesIn(String path) throws Exception {
        return Files.readAllLines(ProjectFiles.projectRoot().resolve(path)).stream().skip(1).map(String::trim).filter(line -> !line.isEmpty())
                .map(line -> line.split(",")[0]).collect(Collectors.toSet());
    }
}
