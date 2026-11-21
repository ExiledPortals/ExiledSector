package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.MockSector;
import exiledsector.ProjectFiles;
import exiledsector.charts.loot.StarChartLootSource;
import exiledsector.items.drop.SalvageSiteDrops;
import exiledsector.loot.LootMultipliers;
import exiledsector.charts.ChartIds;
import exiledsector.items.anchor.AnchorJanusGate;
import exiledsector.items.drop.CraftingDrops;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemData;
import exiledsector.items.ItemUnlock;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.anchor.WormholeAnchorDefinitions;
import exiledsector.items.crafting.material.StarChartCipher;
import exiledsector.items.starchart.StarChartFixtures;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.loot.ChartDropRules;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.mockito.ArgumentCaptor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import lunalib.lunaSettings.LunaSettings;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemSalvageListenerTest {

    private MockedStatic<Misc> miscMock;
    private final ItemSalvageListener listener = new ItemSalvageListener();

    @BeforeEach
    void setUp() throws Exception {
        miscMock = Mockito.mockStatic(Misc.class);
        miscMock.when(() -> Misc.getSalvageSeed(any())).thenReturn(42L);
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "volume_subroutine").put("kind", "MODSPEC_CACHE")
                .put("rarity", "10")));
        SalvageSiteDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station_research").put("chances", "1").put("hullItemUniqueChance", "0")));
        CraftingDrops.register(ProjectFiles.csvRows(CraftingDrops.DATA_PATH));
    }

    @AfterEach
    void tearDown() {
        SalvageSiteDrops.clear();
        ItemDefinitions.clear();
        CraftingDrops.clear();
        miscMock.close();
    }

    private static SectorEntityToken site(String customEntityType, String override) {
        Map<String, Object> values = new HashMap<>();
        if (override != null) {
            values.put(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, override);
        }
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.contains(anyString())).thenAnswer(call -> values.containsKey(call.<String>getArgument(0)));
        when(memory.getString(anyString())).thenAnswer(call -> (String) values.get(call.<String>getArgument(0)));
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(values.get(call.<String>getArgument(0))));
        Mockito.doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getCustomEntityType()).thenReturn(customEntityType);
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        return entity;
    }

    private static InteractionDialogAPI dialogFor(SectorEntityToken target) {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(target);
        return dialog;
    }

    @Test
    void aListedSiteAddsItsHullItemsToTheLootOnce() {
        SectorEntityToken station = site("station_research", null);
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));
        listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));

        verify(loot, times(1)).addSpecial(any(), Mockito.eq(1f));
    }

    @Test
    void aSalvageSpecOverrideDecidesTheSite() {
        assertEquals("station_research", ItemSalvageListener.siteId(site("custom_station", "station_research")));
        assertEquals("custom_station", ItemSalvageListener.siteId(site("custom_station", null)));
    }

    private static SectorEntityToken scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource source) {
        SectorEntityToken debris = site(Entities.DEBRIS_FIELD_SHARED, null);
        DebrisFieldTerrainPlugin.DebrisFieldParams params = new DebrisFieldTerrainPlugin.DebrisFieldParams(200f, 1f, 10f, 5f);
        params.source = source;
        DebrisFieldTerrainPlugin field = mock(DebrisFieldTerrainPlugin.class);
        when(field.getParams()).thenReturn(params);
        when(debris.getMemoryWithoutUpdate().get(MemFlags.SALVAGE_DEBRIS_FIELD)).thenReturn(field);
        return debris;
    }

    @Test
    void everyDebrisFieldButBattleAndSalvagedWreckDebrisIsASalvageSite() {
        for (DebrisFieldTerrainPlugin.DebrisFieldSource source : List.of(DebrisFieldTerrainPlugin.DebrisFieldSource.GEN,
                DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED)) {
            assertEquals(Entities.DEBRIS_FIELD_SHARED, ItemSalvageListener.siteId(scavengedDebris(source)), source.name());
        }
        for (DebrisFieldTerrainPlugin.DebrisFieldSource source : List.of(DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE,
                DebrisFieldTerrainPlugin.DebrisFieldSource.SALVAGE, DebrisFieldTerrainPlugin.DebrisFieldSource.PLAYER_SALVAGE)) {
            assertEquals(null, ItemSalvageListener.siteId(scavengedDebris(source)), source.name());
        }
        assertEquals(null, ItemSalvageListener.siteId(scavengedDebris(null)));
        assertEquals(null, ItemSalvageListener.siteId(site(Entities.DEBRIS_FIELD_SHARED, null)));
    }

    @Test
    void aChartDebrisRingRollsItsSalvageOnceOnTheFieldsOwnMemory() throws Exception {
        SalvageSiteDrops.register(new JSONArray().put(new JSONObject().put("site", Entities.DEBRIS_FIELD_SHARED).put("chances", "1")
                .put("hullItemUniqueChance", "0").put("feedstockMin", "5").put("feedstockMax", "5")));
        SectorEntityToken ring = scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED);
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportAboutToShowLootToPlayer(loot, dialogFor(ring));
        listener.reportAboutToShowLootToPlayer(loot, dialogFor(ring));

        verify(ring.getMemoryWithoutUpdate(), times(1)).set(ItemSalvageListener.ROLLED_KEY, true);
        assertFalse(ItemSalvageListener.canShowcase(ring));
    }

    @Test
    void theShippedTableListsDebrisFieldsLikeAWreck() throws Exception {
        SalvageSiteDrops.register(ProjectFiles.csvRows("data/config/exiledSector/item_salvage.csv"));
        CargoAPI battleDebrisLoot = mock(CargoAPI.class);

        assertTrue(SalvageSiteDrops.hasRule(Entities.DEBRIS_FIELD_SHARED));
        assertTrue(ItemSalvageListener.canShowcase(scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource.GEN)));
        assertTrue(ItemSalvageListener.canShowcase(scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED)));
        assertFalse(ItemSalvageListener.canShowcase(scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource.PLAYER_SALVAGE)));
        assertFalse(ItemSalvageListener.canShowcase(scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE)));
        listener.reportAboutToShowLootToPlayer(battleDebrisLoot, dialogFor(scavengedDebris(DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE)));
        Mockito.verifyNoInteractions(battleDebrisLoot);
    }

    @Test
    void salvageOutsideALiveChartSystemGetsNoChartBonuses() {
        SectorEntityToken outside = mock(SectorEntityToken.class);
        when(outside.getContainingLocation()).thenReturn(mock(LocationAPI.class));
        SectorEntityToken nowhere = mock(SectorEntityToken.class);

        LootMultipliers none = new LootMultipliers(ChartGenContext.none().itemChances(), CraftingDrops.BASE_RARITY);
        assertEquals(none, StarChartLootSource.INSTANCE.lootMultipliersAt(outside.getContainingLocation()));
        assertEquals(none, StarChartLootSource.INSTANCE.lootMultipliersAt(nowhere.getContainingLocation()));
    }

    @Test
    void unlistedSitesAndFleetsAddNothing() {
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportAboutToShowLootToPlayer(loot, dialogFor(site("debris_field_shared", null)));
        listener.reportAboutToShowLootToPlayer(loot, dialogFor(mock(CampaignFleetAPI.class)));
        listener.reportAboutToShowLootToPlayer(loot, null);

        verify(loot, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aSalvagedSystemsiteCanAlsoHoldAStarChartAndAChartCraftingItem() throws Exception {
        StarChartFixtures.registerShipped();
        StarChartCipher.registerAll();
        ChartDropRules.register(new JSONArray().put(new JSONObject().put("source", "system_salvage").put("chance", "1").put("cipher_chance", "200")));
        try (MockSector mockSector = new MockSector()) {
            ChartRun run = new ChartRun("salvageRun", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(6, "hegemony", "boss", 4, 6));
            run.recordArrival();
            ChartRuns.store(run);
            StarSystemAPI system = mock(StarSystemAPI.class);
            when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
            when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag("salvageRun")));
            when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId("salvageRun"));
            SectorEntityToken station = site("station_research", null);
            when(station.getContainingLocation()).thenReturn(system);
            CargoAPI loot = mock(CargoAPI.class);

            listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));

            ArgumentCaptor<SpecialItemData> specials = ArgumentCaptor.forClass(SpecialItemData.class);
            verify(loot, Mockito.atLeastOnce()).addSpecial(specials.capture(), anyFloat());
            List<SpecialItemData> charts = specials.getAllValues().stream().filter(special -> ItemData.STAR_CHART_ITEM_ID.equals(special.getId())).toList();
            assertEquals(1, charts.size());
            assertEquals(6, ItemData.ofStarChart(charts.get(0)).preview().tier());
            assertEquals(StarChartCipher.values().length, specials.getAllValues().stream().filter(special -> StarChartCipher.byItemId(special.getId()) != null).count());
        } finally {
            ChartDropRules.clear();
            StarChartFixtures.unregisterCraftingItems();
            StarChartFixtures.clear();
        }
    }

    private static List<SpecialItemData> lootFrom(ItemSalvageListener listener, SectorEntityToken station) {
        CargoAPI loot = mock(CargoAPI.class);
        listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));
        ArgumentCaptor<SpecialItemData> specials = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(loot, Mockito.atLeast(0)).addSpecial(specials.capture(), anyFloat());
        return specials.getAllValues();
    }

    private static JSONObject subroutine(String id, String kind, boolean unique) throws Exception {
        return new JSONObject().put("id", id).put("name", id).put("rarity", "10").put("kind", kind).put("unique", unique);
    }

    private static JSONObject anchor(String id, String volume) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", volume + "_exit").put("name", id);
    }

    @Test
    void aShowcaseSiteAlsoDropsThreeDoctrinesThreeAnchorsTheCacheAndEveryCraftingMaterialOnce() throws Exception {
        ItemDefinitions.register(new JSONArray().put(subroutine("volume_subroutine", "MODSPEC_CACHE", false))
                .put(subroutine("doctrine_archive_hegemony", "DOCTRINE", true)).put(subroutine("doctrine_archive_luddic", "DOCTRINE", true))
                .put(subroutine("doctrine_archive_pirate", "DOCTRINE", true)).put(subroutine("doctrine_archive_remnant", "DOCTRINE", true))
                .put(subroutine("unique_phoenix_ash_archive", "UNIQUE", true)));
        WormholeAnchorDefinitions.register(new JSONArray().put(anchor("wormhole_anchor_hegemony", "hegemony"))
                .put(anchor("wormhole_anchor_luddic", "luddic")).put(anchor("wormhole_anchor_pirate", "pirate"))
                .put(anchor("wormhole_anchor_abyss", "abyss")));
        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class)) {
            lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), eq(AnchorJanusGate.ALLOW_BEFORE_JANUS_FIELD_ID))).thenReturn(true);
            SectorEntityToken plainStation = site("station_research", null);
            SectorEntityToken showcaseStation = site("station_research", null);
            assertTrue(ItemSalvageListener.canShowcase(showcaseStation));
            ItemSalvageListener.markShowcase(showcaseStation);

            List<SpecialItemData> plainLoot = lootFrom(listener, plainStation);
            List<SpecialItemData> showcaseLoot = lootFrom(listener, showcaseStation);

            List<String> materialIds = MaterialItems.ids();
            long plainItems = plainLoot.stream().filter(special -> !materialIds.contains(special.getId())).count();
            long showcaseItems = showcaseLoot.stream().filter(special -> !materialIds.contains(special.getId())).count();
            assertEquals(plainItems + 1 + 2L * SkillTreeLootSource.SHOWCASE_PICKS, showcaseItems);
            assertTrue(showcaseLoot.stream().map(SpecialItemData::getId).toList().containsAll(materialIds.stream()
                    .filter(itemId -> ItemUnlock.craftingItemUsable(itemId, Global.getSector())).toList()));
            assertFalse(ItemSalvageListener.canShowcase(showcaseStation));
            assertTrue(lootFrom(listener, showcaseStation).isEmpty());
        } finally {
            WormholeAnchorDefinitions.clear();
        }
    }

    @Test
    void outsideSystemsChartSalvageNeedsASectorAndItsOwnRule() {
        assertTrue(StarChartLootSource.salvageLoot(null, site("station_research", null)).isEmpty());
    }
}
