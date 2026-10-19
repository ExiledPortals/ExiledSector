package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.util.MutableValue;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemUnlock;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartDropsTest {

    private static final String RUN_ID = "dropRun";
    private static final String WARDENS_VIGIL = "unique_chart_wardens_vigil";
    private static final String SILENT_REACH = "unique_chart_silent_reach";
    private static final Set<String> CHART_CURRENCY_IDS = Set.of(StarChartCurrency.REFORGE.itemId(), StarChartCurrency.ANNULMENT.itemId(),
            StarChartCurrency.CARTOGRAPHY.itemId());

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private final Map<String, Object> sectorMemoryValues = new HashMap<>();
    private int playerLevel = 10;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        StarChartCurrency.registerAll();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.contains(anyString())).thenAnswer(call -> sectorMemoryValues.containsKey(call.<String>getArgument(0)));
        when(sectorMemory.getInt(anyString())).thenAnswer(call -> ((Number) sectorMemoryValues.get(call.<String>getArgument(0))).intValue());
        when(sectorMemory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(sectorMemoryValues.get(call.<String>getArgument(0))));
        Mockito.doAnswer(call -> sectorMemoryValues.put(call.getArgument(0), call.getArgument(1))).when(sectorMemory).set(anyString(), any());
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenAnswer(call -> playerLevel);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        ChartDrops.clear();
        StarChartCurrency.unregisterAll();
        StarChartFixtures.clear();
    }

    private static JSONObject rule(String source, float chance, float extraChance, float tierUpChance, float currencyChance) throws Exception {
        return new JSONObject().put("source", source).put("chance", String.valueOf(chance)).put("extra_chance", String.valueOf(extraChance))
                .put("tier_up_chance", String.valueOf(tierUpChance)).put("currency_chance", String.valueOf(currencyChance));
    }

    private static int tierOf(ItemData chart) {
        return chart.preview().tier();
    }

    private static Predicate<exiledsector.items.ItemDefinition> anyUnique() {
        return definition -> true;
    }

    @Test
    void theShippedTableCoversEverySourceAndKeepsChartCurrenciesInsideSystems() throws Exception {
        ChartDrops.register(ChartContentDataTest.csvRows(ChartDrops.PATH));

        for (ChartDrops.Source source : ChartDrops.Source.values()) {
            ChartDrops.Rule rule = ChartDrops.rule(source);
            assertNotNull(rule, source.id());
            assertTrue(rule.chance() > 0f, source.id());
            assertEquals(source.insideSystem(), rule.currencyChance() > 0f, source.id());
            if (!source.insideSystem()) {
                assertTrue(rule.chance() <= 0.1f, source.id() + " must stay rare");
                assertTrue(rule.maxTier() <= 6, source.id() + " must leave the high tiers to the sustain loop");
                assertEquals(0f, rule.tierUpChance(), source.id());
            }
        }
        assertEquals(1f, ChartDrops.rule(ChartDrops.Source.RUN_REWARD).chance());
    }

    @Test
    void aCurrencyChanceOutsideASystemIsIgnored() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("battle", 1f, 0f, 0f, 1f)).put(rule("nowhere", 1f, 0f, 0f, 0f)));

        assertEquals(0f, ChartDrops.rule(ChartDrops.Source.BATTLE).currencyChance());
        assertFalse(ChartDrops.hasRule(ChartDrops.Source.SALVAGE));
        for (int roll = 0; roll < 200; roll++) {
            assertTrue(ChartDrops.roll(ChartDrops.Source.BATTLE, 3, 1f, new Random(roll), anyUnique()).currencies().isEmpty());
        }
    }

    @Test
    void runRewardsDropChartsAtTheRunTierOrOneAbove() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0f, 0.4f, 0f)));
        Random random = new Random(11L);
        int trials = 3000;
        int tierUps = 0;
        for (int roll = 0; roll < trials; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 5, 1f, random, anyUnique());
            assertEquals(1, loot.charts().size());
            int tier = tierOf(loot.charts().get(0));
            assertTrue(tier == 5 || tier == 6, "tier " + tier);
            tierUps += tier == 6 ? 1 : 0;
        }
        assertEquals(0.4, tierUps / (double) trials, 0.03);
        for (int roll = 0; roll < 50; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 15, 1f, random, anyUnique());
            assertEquals(15, tierOf(loot.charts().get(0)));
        }
    }

    @Test
    void theRewardMultiplierScalesHowManyChartsDropUpToTheCap() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0.25f, 0f, 0f)));

        assertEquals(1.25, averageCharts(1f), 0.05);
        assertEquals(2.5, averageCharts(2f), 0.05);
        assertEquals(ChartDrops.MAX_CHARTS_PER_DROP, averageCharts(10f), 0.0);
        assertEquals(0.0, averageCharts(0f), 0.0);
    }

    private static double averageCharts(float mult) {
        Random random = new Random(5L);
        int charts = 0;
        int trials = 2000;
        for (int roll = 0; roll < trials; roll++) {
            charts += ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 4, mult, random, definition -> true).charts().size();
        }
        return charts / (double) trials;
    }

    @Test
    void uniqueChartsNeedTheirUnlockAndTheirMinimumTier() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0f, 0f, 0f)));
        assertEquals(ItemUnlock.CLEARED_CHART_TIER_PREFIX + "3", ItemDefinitions.get(WARDENS_VIGIL).unlock());

        Set<String> lockedDrops = dropIds(6);
        assertFalse(lockedDrops.contains(WARDENS_VIGIL));
        assertTrue(lockedDrops.contains(SILENT_REACH));

        assertTrue(ItemUnlock.recordChartCleared(sector, 3));
        assertFalse(ItemUnlock.recordChartCleared(sector, 2));
        assertEquals(3, ItemUnlock.highestClearedChartTier(sector));
        assertTrue(dropIds(6).contains(WARDENS_VIGIL));
        assertFalse(dropIds(3).contains(WARDENS_VIGIL), "the vigil never rolls below its minimum tier");
    }

    private Set<String> dropIds(int tier) {
        Set<String> ids = new HashSet<>();
        Random random = new Random(tier);
        for (int roll = 0; roll < 4000; roll++) {
            ChartDrops.roll(ChartDrops.Source.RUN_REWARD, tier, 1f, random, ChartDrops.uniquesFor(sector)).charts()
                    .forEach(chart -> ids.add(chart.definitionId()));
        }
        return ids;
    }

    @Test
    void onlySystemsourcesDropChartCurrencies() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("system_salvage", 0f, 0f, 0f, 1f)).put(rule("salvage", 1f, 0f, 0f, 0f)));

        ChartDrops.Loot systemLoot = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, 4, 1f, new Random(2L), anyUnique());
        assertTrue(systemLoot.charts().isEmpty());
        assertEquals(1, systemLoot.currencies().size());
        assertTrue(CHART_CURRENCY_IDS.containsAll(systemLoot.currencies().keySet()));
        assertTrue(ChartDrops.roll(ChartDrops.Source.SALVAGE, 4, 1f, new Random(2L), anyUnique()).currencies().isEmpty());
    }

    @Test
    void outsideSystemsTheTierComesFromTheEncounterStrength() throws Exception {
        ChartDrops.register(ChartContentDataTest.csvRows(ChartDrops.PATH));

        assertEquals(ChartDrops.NO_TIER, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 100f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 150f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 269f));
        assertEquals(2, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 270f));
        assertEquals(6, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 100000f));
        assertEquals(ChartDrops.NO_TIER, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 4f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 5f));
        assertEquals(3, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 11f));
        assertEquals(5, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 60f));
        assertTrue(ChartDrops.roll(ChartDrops.Source.BATTLE, ChartDrops.NO_TIER, 1f, new Random(1L), anyUnique()).isEmpty());
    }

    private StarSystemAPI system(String runId) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag(runId)));
        return system;
    }

    private ChartRun storeRun(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "hegemony", ChartObjective.CLEAR, List.of("g1"), "boss", 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    @Test
    void systemsalvageAndBattlesDropAtTheRunTierAndOnlyWhileTheRunIsLive() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("system_salvage", 1f, 0f, 0f, 1f)).put(rule("system_battle", 1f, 0f, 0f, 0f))
                .put(rule("salvage", 1f, 0f, 0f, 0f)).put(rule("battle", 1f, 0f, 0f, 0f)));
        StarSystemAPI system = system(RUN_ID);

        assertTrue(ChartDrops.forSalvage(sector, system, new Random(1L)).isEmpty(), "a system without a live run drops nothing");

        storeRun(9);
        ChartDrops.Loot salvage = ChartDrops.forSalvage(sector, system, new Random(1L));
        assertEquals(9, tierOf(salvage.charts().get(0)));
        assertEquals(1, salvage.currencies().size());
        ChartDrops.Loot battle = ChartDrops.forBattle(sector, system, 40f, new Random(1L));
        assertEquals(9, tierOf(battle.charts().get(0)));
        assertTrue(ChartDrops.forSalvage(sector, system("otherRun"), new Random(1L)).isEmpty());

        LocationAPI outside = mock(LocationAPI.class);
        assertEquals(1, tierOf(ChartDrops.forSalvage(sector, outside, new Random(1L)).charts().get(0)));
        assertTrue(ChartDrops.forSalvage(sector, outside, new Random(1L)).currencies().isEmpty());
    }

    @Test
    void techMiningUsesTheFirstFindOrTheMonthlyRule() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("techmining_first_find", 1f, 0f, 0f, 0f)));

        assertEquals(1, ChartDrops.forTechMining(sector, true, 1f, new Random(1L)).charts().size());
        assertTrue(ChartDrops.forTechMining(sector, false, 1f, new Random(1L)).isEmpty());
    }

    @Test
    void completingAnObjectiveRecordsTheClearedTierAndDropsChartsIntoCargo() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0f, 0f, 1f)));
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getCredits()).thenReturn(mock(MutableValue.class));
        when(playerFleet.getCargo()).thenReturn(cargo);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        ChartRun run = storeRun(4);
        run.recordTargetGone("g1");
        assertTrue(run.markObjectiveComplete());

        ChartRewards.Reward reward = ChartRewards.grant(sector, run);

        assertEquals(1, reward.starCharts());
        assertEquals(4, ItemUnlock.highestClearedChartTier(sector));
        ArgumentCaptor<SpecialItemData> specials = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, atLeastOnce()).addSpecial(specials.capture(), Mockito.eq(1f));
        List<SpecialItemData> charts = specials.getAllValues().stream().filter(special -> ItemData.STAR_CHART_ITEM_ID.equals(special.getId())).toList();
        assertEquals(1, charts.size());
        assertEquals(4, ItemData.ofStarChart(charts.get(0)).preview().tier());
        assertTrue(specials.getAllValues().stream().anyMatch(special -> CHART_CURRENCY_IDS.contains(special.getId())));
        assertNull(ChartRewards.grant(sector, run));
    }
}
