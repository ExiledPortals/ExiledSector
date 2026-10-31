package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.util.MutableValue;
import exiledsector.MockSector;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.objective.ChartRewards;
import exiledsector.items.ItemData;
import exiledsector.items.ItemUnlock;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import exiledsector.skills.SkillTreeSetting;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartDropsTest {

    private static final String RUN_ID = "dropRun";
    private static final String GATED = StarChartFixtures.TEST_GATED;
    private static final String UNGATED = StarChartFixtures.TEST_FIXED;
    private static final Set<String> CHART_CURRENCY_IDS = Set.of(StarChartCurrency.REFORGE.itemId(), StarChartCurrency.ANNULMENT.itemId(),
            StarChartCurrency.CARTOGRAPHY.itemId());

    private MockSector mockSector;
    private SectorAPI sector;
    private final Map<String, Object> sectorMemoryValues = new HashMap<>();
    private int playerLevel = 10;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        StarChartCurrency.registerAll();
        mockSector = new MockSector();
        sector = mockSector.sector;
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
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartDrops.clear();
        AtlasCatalysts.resetForTests();
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

    @Test
    void theShippedTableCoversEverySourceAndKeepsChartCurrenciesInsideSystems() throws Exception {
        ChartDrops.register(ChartTables.csvRows(ChartDrops.PATH));

        for (ChartDrops.Source source : ChartDrops.Source.values()) {
            ChartDrops.Rule rule = ChartDrops.rule(source);
            assertNotNull(rule, source.id());
            assertTrue(rule.chance() > 0f, source.id());
            assertEquals(source.insideSystem(), rule.currencyChance() > 0f, source.id());
            assertEquals(source.insideSystem(), rule.catalystChance() > 0f, source.id());
            if (!source.insideSystem()) {
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
            assertTrue(ChartDrops.roll(ChartDrops.Source.BATTLE, 3, 1f, new Random(roll)).currencies().isEmpty());
        }
    }

    @Test
    void nothingDropsWhileStarChartsAreSwitchedOff() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 1f, 0f, 1f)).put(rule("chart_boss", 1f, 1f, 0f, 1f)));
        StarChartSettings.setEnabledForTests(false);
        try {
            for (int roll = 0; roll < 50; roll++) {
                assertSame(ChartDrops.Loot.NONE, ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 5, 1f, new Random(roll)));
                assertSame(ChartDrops.Loot.NONE, ChartDrops.roll(ChartDrops.Source.CHART_BOSS, 5, 2f, new Random(roll)));
            }
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
        assertEquals(2, ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 5, 1f, new Random(1L)).charts().size());
    }

    @Test
    void chartsDroppedInsideARunNeverFallBelowTheRunTierButOutsideDropsKeepTheirCap() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f, 0f, 0f, 0f).put("max_tier", "3"))
                .put(rule("battle", 1f, 0f, 0f, 0f).put("max_tier", "3")));

        assertEquals(9, tierOf(ChartDrops.roll(ChartDrops.Source.CHART_FLEET, 9, 1f, new Random(4L)).charts().get(0)));
        assertEquals(3, tierOf(ChartDrops.roll(ChartDrops.Source.BATTLE, 9, 1f, new Random(4L)).charts().get(0)));
    }

    @Test
    void aPowerfulBossAlwaysDropsAChartOneTierUp() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_boss", 0f, 0f, 0f, 0f)));
        for (long roll = 0; roll < 30; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.CHART_BOSS, 5, ChartDrops.Mults.uniform(1f), new Random(roll), true);
            assertEquals(6, tierOf(loot.charts().get(0)));
        }
        assertSame(ChartDrops.Loot.NONE, ChartDrops.roll(ChartDrops.Source.CHART_BOSS, 5, ChartDrops.Mults.uniform(1f), new Random(3L), false));
    }

    @Test
    void runRewardsDropChartsAtTheRunTierOrOneAbove() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0f, 0.4f, 0f)));
        Random random = new Random(11L);
        int trials = 3000;
        int tierUps = 0;
        for (int roll = 0; roll < trials; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 5, 1f, random);
            assertEquals(1, loot.charts().size());
            int tier = tierOf(loot.charts().get(0));
            assertTrue(tier == 5 || tier == 6, "tier " + tier);
            tierUps += tier == 6 ? 1 : 0;
        }
        assertEquals(0.4, tierUps / (double) trials, 0.03);
        for (int roll = 0; roll < 50; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 15, 1f, random);
            assertEquals(15, tierOf(loot.charts().get(0)));
        }
    }

    @Test
    void theRewardMultiplierScalesHowManyChartsDropWithoutACap() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0.25f, 0f, 0f)));

        assertEquals(1.25, averageCharts(1f), 0.05);
        assertEquals(2.5, averageCharts(2f), 0.05);
        assertEquals(11.0, averageCharts(10f), 0.0);
        assertEquals(0.0, averageCharts(0f), 0.0);
    }

    private static double averageCharts(float mult) {
        Random random = new Random(5L);
        int charts = 0;
        int trials = 2000;
        for (int roll = 0; roll < trials; roll++) {
            charts += ChartDrops.roll(ChartDrops.Source.RUN_REWARD, 4, mult, random).charts().size();
        }
        return charts / (double) trials;
    }

    @Test
    void aChartOnlyDropsFromItsMinimumTier() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("run_reward", 1f, 0f, 0f, 0f)));

        assertTrue(dropIds(6).contains(GATED));
        assertTrue(dropIds(3).contains(UNGATED));
        assertFalse(dropIds(3).contains(GATED), "the gated chart never rolls below its minimum tier");
    }

    private Set<String> dropIds(int tier) {
        Set<String> ids = new HashSet<>();
        Random random = new Random(tier);
        for (int roll = 0; roll < 4000; roll++) {
            ChartDrops.roll(ChartDrops.Source.RUN_REWARD, tier, 1f, random).charts()
                    .forEach(chart -> ids.add(chart.definitionId()));
        }
        return ids;
    }

    @Test
    void onlySystemsourcesDropChartCurrencies() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("system_salvage", 0f, 0f, 0f, 1f)).put(rule("salvage", 1f, 0f, 0f, 0f)));

        ChartDrops.Loot systemLoot = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, 4, 1f, new Random(2L));
        assertTrue(systemLoot.charts().isEmpty());
        assertEquals(1, systemLoot.currencies().size());
        assertTrue(CHART_CURRENCY_IDS.containsAll(systemLoot.currencies().keySet()));
        assertTrue(ChartDrops.roll(ChartDrops.Source.SALVAGE, 4, 1f, new Random(2L)).currencies().isEmpty());
    }

    @Test
    void catalystsDropOnlyInsideSystemsAmongThoseTheTierReachesScaledByTheirMultiplier() throws Exception {
        AtlasCatalysts.useForTests(List.of(new AtlasCatalysts.Catalyst("test_low", "spatial_breach", AtlasCatalysts.Family.TERMINUS, 1f, 1),
                new AtlasCatalysts.Catalyst("test_high", "asteroid_storm", AtlasCatalysts.Family.TERMINUS, 1f, 10)));
        ChartDrops.register(new JSONArray().put(rule("system_salvage", 0f, 0f, 0f, 0f).put("catalyst_chance", "1"))
                .put(rule("salvage", 0f, 0f, 0f, 0f).put("catalyst_chance", "1")));

        assertEquals(0f, ChartDrops.rule(ChartDrops.Source.SALVAGE).catalystChance());
        for (long roll = 0; roll < 30; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, 4, new ChartDrops.Mults(0f, 0f, 1f), new Random(roll));
            assertEquals(Map.of("test_low", 1), loot.catalysts());
            assertEquals(Map.of("test_low", 1), loot.materials());
            assertTrue(ChartDrops.roll(ChartDrops.Source.SALVAGE, 4, 1f, new Random(roll)).isEmpty());
        }
        Set<String> highTierDrops = new HashSet<>();
        int dropped = 0;
        Random random = new Random(7L);
        int trials = 4000;
        for (int roll = 0; roll < trials; roll++) {
            ChartDrops.Loot loot = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, 12, new ChartDrops.Mults(0f, 0f, 0.5f), random);
            highTierDrops.addAll(loot.catalysts().keySet());
            dropped += loot.catalysts().size();
        }
        assertEquals(Set.of("test_low", "test_high"), highTierDrops);
        assertEquals(0.5, dropped / (double) trials, 0.03);
    }

    @Test
    void theCatalystModifierScalesOnlyCatalystsAndTheQuantityLinesScaleThemToo() {
        ChartGenContext catalysts = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_CATALYST_PERCENT, 50f)));
        ChartGenContext quantity = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 20f)));

        assertEquals(new ChartDrops.Mults(1f, 1f, 1.5f), ChartDrops.Mults.of(catalysts, 1f));
        assertEquals(2.4f, ChartDrops.Mults.of(quantity, 2f).catalyst(), 0.0001f);
    }

    @Test
    void outsideSystemsTheStrengthOnlyGatesTheDropAndSalvageAndTechMiningStayAtTierOne() throws Exception {
        ChartDrops.register(ChartTables.csvRows(ChartDrops.PATH));

        assertEquals(ChartDrops.NO_TIER, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 0f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.BATTLE, 1f));
        assertEquals(ChartDrops.NO_TIER, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 4f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 5f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.SALVAGE, 60f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.TECHMINING_FIRST_FIND, 60f));
        assertEquals(1, ChartDrops.tierForStrength(ChartDrops.Source.TECHMINING_MONTHLY, 60f));
        assertTrue(ChartDrops.roll(ChartDrops.Source.BATTLE, ChartDrops.NO_TIER, 1f, new Random(1L)).isEmpty());
    }

    @Test
    void aWonBattleDropsTheTierItsStrongestDefeatedNpcLevelReached() throws Exception {
        ChartDrops.register(ChartTables.csvRows(ChartDrops.PATH));
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        try {
            assertEquals(ChartDrops.NO_TIER, ChartDrops.battleTier(0f, 40));
            assertEquals(1, ChartDrops.battleTier(400f, 0));
            assertEquals(1, ChartDrops.battleTier(400f, 15));
            assertEquals(1, ChartDrops.battleTier(400f, 25));
            assertEquals(2, ChartDrops.battleTier(400f, 28));
            assertEquals(7, ChartDrops.battleTier(400f, 35));
            assertEquals(6, tierOf(ChartDrops.roll(ChartDrops.Source.BATTLE, ChartDrops.battleTier(400f, 35), 100f, new Random(1L)).charts()
                    .get(0)));
        } finally {
            ChartTiers.resetForTests();
        }
    }

    @Test
    void withTheSkillTreeOffABattleDropsATierPerStrengthPerTierOfDeploymentPointsDefeated() throws Exception {
        ChartDrops.register(ChartTables.csvRows(ChartDrops.PATH));
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        SkillTreeSetting.setEnabledForTests(false);
        try {
            assertEquals(ChartDrops.NO_TIER, ChartDrops.battleTier(0f, 40));
            assertEquals(1, ChartDrops.battleTier(150f, 35));
            assertEquals(3, ChartDrops.battleTier(400f, 35));
            assertEquals(6, ChartDrops.battleTier(5000f, 0));
        } finally {
            SkillTreeSetting.setEnabledForTests(SkillTreeSetting.DEFAULT_ENABLED);
            ChartTiers.resetForTests();
        }
    }

    @Test
    void battleChartsScaleWithTheDifficultyScaledDeploymentPointsDefeated() throws Exception {
        ChartDrops.register(ChartTables.csvRows(ChartDrops.PATH));
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        try {
            LocationAPI outside = mock(LocationAPI.class);
            for (int roll = 0; roll < 50; roll++) {
                assertEquals(1, ChartDrops.forBattle(outside, 500f, 0, new Random(roll)).charts().size());
                assertEquals(2, ChartDrops.forBattle(outside, 1000f, 0, new Random(roll)).charts().size());
                assertEquals(10, ChartDrops.forBattle(outside, 5000f, 0, new Random(roll)).charts().size());
            }
            Random random = new Random(3L);
            int trials = 4000;
            int drops = 0;
            for (int roll = 0; roll < trials; roll++) {
                drops += ChartDrops.forBattle(outside, 100f, 0, random).charts().size();
            }
            assertEquals(0.2f, drops / (float) trials, 0.03f);
        } finally {
            ChartTiers.resetForTests();
        }
    }

    private StarSystemAPI system(String runId) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag(runId)));
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(runId));
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
    void systemSalvageDropsAtTheRunTierOnlyWhileTheRunIsLiveAndBattlesInsideLeaveItToTheFleetRolls() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("system_salvage", 1f, 0f, 0f, 1f)).put(rule("chart_fleet", 1f, 0f, 0f, 0f))
                .put(rule("salvage", 1f, 0f, 0f, 0f)).put(rule("battle", 1f, 0f, 0f, 0f)));
        StarSystemAPI system = system(RUN_ID);

        assertTrue(ChartDrops.forSalvage(sector, system, new Random(1L)).isEmpty(), "a system without a live run drops nothing");

        storeRun(9);
        ChartDrops.Loot salvage = ChartDrops.forSalvage(sector, system, new Random(1L));
        assertEquals(9, tierOf(salvage.charts().get(0)));
        assertEquals(1, salvage.currencies().size());
        assertTrue(ChartDrops.forBattle(system, 400f, 30, new Random(1L)).isEmpty());
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
        CargoAPI cargo = mockSector.cargo();
        when(cargo.getCredits()).thenReturn(mock(MutableValue.class));
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
