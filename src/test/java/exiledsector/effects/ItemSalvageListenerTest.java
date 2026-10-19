package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartIds;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemData;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import exiledsector.charts.ChartContent;
import exiledsector.charts.ChartDrops;
import exiledsector.charts.ChartObjective;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.mockito.ArgumentCaptor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
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
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "domain_subroutine_military")
                .put("rarity", "10").put("prefixes", "HULL_MULT:4:6").put("suffixes", "ARMOR_PERCENT:6:9")));
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station_research").put("chances", "1").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        ItemDrops.clear();
        ItemDefinitions.clear();
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

    @Test
    void aChartSystemsiteCanRaiseTheHullItemChance() {
        MemoryAPI plain = mock(MemoryAPI.class);
        MemoryAPI boosted = mock(MemoryAPI.class);
        MemoryAPI broken = mock(MemoryAPI.class);
        when(boosted.contains(ChartIds.ITEM_CHANCE_MEMORY_KEY)).thenReturn(true);
        when(boosted.getFloat(ChartIds.ITEM_CHANCE_MEMORY_KEY)).thenReturn(1.5f);
        when(broken.contains(ChartIds.ITEM_CHANCE_MEMORY_KEY)).thenReturn(true);
        when(broken.getFloat(ChartIds.ITEM_CHANCE_MEMORY_KEY)).thenReturn(-2f);

        assertEquals(1f, ItemSalvageListener.itemChanceMult(plain));
        assertEquals(1.5f, ItemSalvageListener.itemChanceMult(boosted));
        assertEquals(1f, ItemSalvageListener.itemChanceMult(broken));
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
    void aSalvagedSystemsiteCanAlsoHoldAStarChartAndAChartCurrency() throws Exception {
        StarChartFixtures.registerShipped();
        StarChartCurrency.registerAll();
        ChartDrops.register(new JSONArray().put(new JSONObject().put("source", "system_salvage").put("chance", "1").put("currency_chance", "1")));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            SectorAPI sector = mock(SectorAPI.class);
            when(sector.getPersistentData()).thenReturn(new HashMap<>());
            global.when(Global::getSector).thenReturn(sector);
            ChartRun run = new ChartRun("salvageRun", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(6, "hegemony", ChartObjective.CLEAR, List.of("g1"), "boss", 4, 6));
            run.recordArrival();
            ChartRuns.store(run);
            StarSystemAPI system = mock(StarSystemAPI.class);
            when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
            when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag("salvageRun")));
            SectorEntityToken station = site("station_research", null);
            when(station.getContainingLocation()).thenReturn(system);
            CargoAPI loot = mock(CargoAPI.class);

            listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));

            ArgumentCaptor<SpecialItemData> specials = ArgumentCaptor.forClass(SpecialItemData.class);
            verify(loot, Mockito.atLeastOnce()).addSpecial(specials.capture(), anyFloat());
            List<SpecialItemData> charts = specials.getAllValues().stream().filter(special -> ItemData.STAR_CHART_ITEM_ID.equals(special.getId())).toList();
            assertEquals(1, charts.size());
            assertEquals(6, ItemData.ofStarChart(charts.get(0)).preview().tier());
            assertEquals(1, specials.getAllValues().stream().filter(special -> StarChartCurrency.byItemId(special.getId()) != null).count());
        } finally {
            ChartDrops.clear();
            StarChartCurrency.unregisterAll();
            StarChartFixtures.clear();
        }
    }

    @Test
    void outsideSystemsChartSalvageNeedsASectorAndItsOwnRule() {
        assertTrue(ItemSalvageListener.chartLoot(null, site("station_research", null)).isEmpty());
    }
}
