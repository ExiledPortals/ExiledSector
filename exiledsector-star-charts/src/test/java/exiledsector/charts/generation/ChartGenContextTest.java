package exiledsector.charts.generation;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.i18n.RealCatalogue;
import exiledsector.i18n.StyledText;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartFleetSpread;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.loot.ChartLootTotals;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.objective.ChartRewards;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemTraits;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartGenContextTest {

    private static final ChartTier BASE = ChartTier.fallback(8);
    private static final float TOLERANCE = 0.001f;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        ChartTables.loadShipped();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    private static ChartGenContext with(ChartEffect effect, float value) {
        return ChartGenContext.of(List.of(new AppliedEffect(effect, value)));
    }

    @Test
    void noModifiersLeaveTheTierAlone() {
        assertSame(BASE, ChartGenContext.none().adjust(BASE));
        assertSame(ChartGenContext.none(), ChartGenContext.from(null));
        assertEquals(1f, ChartGenContext.none().rewardMult());
        assertEquals(1f, ChartGenContext.none().sensorRangeMult());
        assertEquals(0, ChartGenContext.none().extraPois());
    }

    @Test
    void aChartsModifiersAreReadDeterministicallyWithTheirTierValues() {
        StarChart chart = StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, 12);
        ChartGenContext first = ChartGenContext.from(chart);
        ChartGenContext second = ChartGenContext.from(StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, 12));

        assertEquals(first.applied(), second.applied());
        assertTrue(first.applied().contains(new AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 700f)), first.applied().toString());
        assertTrue(first.applied().contains(new AppliedEffect(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 2f)), first.applied().toString());
        assertTrue(chart.implicits().isEmpty(), "charts no longer roll a faction implicit; the atlas node decides the faction");
        assertEquals(first.factionId(), second.factionId());
        assertEquals(700f, first.bossSizePercent(), TOLERANCE);
        assertEquals(BASE.bossSize() + 700f, first.adjust(BASE).bossSize(), TOLERANCE, "boss size modifiers add percentage points");
    }

    @Test
    void eachDifficultyEffectChangesTheFleets() {
        assertEquals(BASE.fleetSize() + 50f, with(ChartEffect.CHART_FLEET_SIZE_PERCENT, 50f).adjust(BASE).fleetSize(), TOLERANCE);
        ChartTier moreRolled = with(ChartEffect.CHART_BASE_FLEETS, 2f).adjust(BASE);
        assertEquals(BASE.fleetsMin() + 2, moreRolled.fleetsMin());
        assertEquals(BASE.fleetsMax() + 2, moreRolled.fleetsMax());
        assertEquals(BASE.fleetSize(), moreRolled.fleetSize(), TOLERANCE, "extra fleets are full size, not a thinner split");
        ChartGenContext crowded = with(ChartEffect.CHART_EXTRA_FLEETS, 2f);
        assertEquals(BASE, crowded.adjust(BASE), "crowded fleets are added after the soft limit, not to the roll");
        assertEquals(2, crowded.extraFleets());
        assertEquals(BASE.officerLevelBonus() + 1, with(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 1f).adjust(BASE).officerLevelBonus());
        assertEquals(BASE.officerCountBonus() + 2, with(ChartEffect.CHART_EXTRA_OFFICERS, 2f).adjust(BASE).officerCountBonus());
        assertEquals(BASE.bossSize() + 40f, with(ChartEffect.CHART_BOSS_SIZE_PERCENT, 40f).adjust(BASE).bossSize(), TOLERANCE);
        ChartTier noBoss = new ChartTier(BASE.tier(), BASE.fleetSize(), BASE.fleetsMin(), BASE.fleetsMax(), BASE.officerLevelBonus(),
                BASE.officerCountBonus(), BASE.fleetQuality(), BASE.levelMin(), BASE.levelMax(), BASE.cacheBudget(), BASE.domainBudget(),
                BASE.stationBudget(), BASE.derelictBudget(), false, 0f, BASE.clearWeight(), BASE.bossWeight());
        assertEquals(0f, with(ChartEffect.CHART_BOSS_SIZE_PERCENT, 40f).adjust(noBoss).bossSize(), TOLERANCE,
                "a tier switched off from having a boss stays without one");
        assertEquals(BASE.fleetQuality() + 20f, with(ChartEffect.CHART_FLEET_QUALITY_PERCENT, 20f).adjust(BASE).fleetQuality(), TOLERANCE);
        assertEquals(BASE.fleetSize(), with(ChartEffect.CHART_FLEET_QUALITY_PERCENT, 20f).adjust(BASE).fleetSize(), TOLERANCE,
                "fleet quality and fleet size are separate");
        assertEquals(BASE.poiBudget(), with(ChartEffect.CHART_FLEET_SIZE_PERCENT, 50f).adjust(BASE).poiBudget(), TOLERANCE);
    }

    @Test
    void eachRewardEffectChangesTheLootOrTheReward() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_POI_BUDGET_PERCENT, 30f),
                new AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 10f), new AppliedEffect(ChartEffect.CHART_REWARD_PERCENT, 20f),
                new AppliedEffect(ChartEffect.CHART_EXTRA_POIS, 3f), new AppliedEffect(ChartEffect.CHART_ITEM_PERCENT, 50f)));

        assertEquals(BASE.poiBudget() * 1.3f, context.adjust(BASE).poiBudget(), TOLERANCE, "quantity does not raise the site budgets");
        assertEquals(1.3f, context.rewardMult(), TOLERANCE);
        assertEquals(3, context.extraPois());
        assertEquals(1.6f, context.itemChances().general(), TOLERANCE);
        assertEquals(BASE.fleetSize(), context.adjust(BASE).fleetSize(), TOLERANCE);
        assertEquals(0.7f, with(ChartEffect.CHART_SENSOR_RANGE_PERCENT, -30f).sensorRangeMult(), TOLERANCE);
        assertEquals(ChartGenContext.MIN_SENSOR_RANGE_MULT, with(ChartEffect.CHART_SENSOR_RANGE_PERCENT, -400f).sensorRangeMult(), TOLERANCE);
        assertTrue(context.describe().contains("reward x1.30"), context.describe());
    }

    private static ChartContentPlan plan(ChartGenContext context, long seed) {
        return plan(context, seed, 8);
    }

    private static ChartContentPlan plan(ChartGenContext context, long seed, int tier) {
        return ChartContentPlanner.plan(seed, ChartTiers.forTier(tier), ChartPois.all(), ChartFactions.all(), faction -> true, context);
    }

    @Test
    void theHooksShapeTheContentPlanTheSameWayEveryTime() {
        long seed = 4242L;
        ChartContentPlan base = plan(ChartGenContext.none(), seed);
        ChartGenContext harder = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 60f),
                new AppliedEffect(ChartEffect.CHART_EXTRA_FLEETS, 2f), new AppliedEffect(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 2f)));
        ChartGenContext richer = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_EXTRA_POIS, 3f),
                new AppliedEffect(ChartEffect.CHART_POI_BUDGET_PERCENT, 50f)));

        assertEquals(plan(harder, seed), plan(harder, seed));
        assertEquals(plan(richer, seed), plan(richer, seed));
        assertEquals(base, ChartContentPlanner.plan(seed, ChartTiers.forTier(8), ChartPois.all(), ChartFactions.all(), faction -> true));
        ChartContentPlan harderPlan = plan(harder, seed);
        float basePoints = base.fleetPoints(FleetRole.GUARD) + base.fleetPoints(FleetRole.PATROL);
        float harderPoints = harderPlan.fleetPoints(FleetRole.GUARD) + harderPlan.fleetPoints(FleetRole.PATROL);
        assertTrue(harderPoints > basePoints, basePoints + " -> " + harderPoints);
        assertTrue(harderPlan.count(FleetRole.GUARD) + harderPlan.count(FleetRole.PATROL)
                > base.count(FleetRole.GUARD) + base.count(FleetRole.PATROL));
        assertTrue(harderPlan.fleets().stream().allMatch(fleet -> fleet.officerLevelBonus() >= 2));
        ChartContentPlan richerPlan = plan(richer, seed);
        assertTrue(richerPlan.pois().size() >= base.pois().size() + 3, base.pois().size() + " -> " + richerPlan.pois().size());
        assertTrue(richerPlan.lootSpent() > base.lootSpent());
        assertNotEquals(base, richerPlan);
    }

    @Test
    void dropBonusesAddWithinATypeAndStayApartAcrossTypes() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 10f),
                new AppliedEffect(ChartEffect.CHART_ITEM_PERCENT, 20f), new AppliedEffect(ChartEffect.CHART_REACTOR_ITEM_PERCENT, 40f),
                new AppliedEffect(ChartEffect.CHART_REACTOR_ITEM_PERCENT, 10f), new AppliedEffect(ChartEffect.CHART_CURRENCY_PERCENT, 30f)));

        assertEquals(1.8f, context.dropMult(DropType.REACTOR), TOLERANCE);
        assertEquals(1.3f, context.dropMult(DropType.BRIDGE), TOLERANCE);
        assertEquals(1.3f, context.dropMult(DropType.STAR_CHART), TOLERANCE);
        assertEquals(1.4f, context.dropMult(DropType.CURRENCY), TOLERANCE, "Salvage only reaches item types");
        assertEquals(1.1f, context.dropMult(DropType.WEAPON), TOLERANCE);
        assertEquals(1f, context.dropMult(DropType.COLONY_ITEM), TOLERANCE, "Quantity leaves colony items out");
        assertEquals(1.3f, context.itemChances().general(), TOLERANCE);
        assertEquals(0.5f, context.itemChances().targeted(ItemKind.REACTOR), TOLERANCE);
        assertEquals(0f, context.itemChances().targeted(ItemKind.BRIDGE), TOLERANCE);
        assertTrue(context.describe().contains("reactor +50%"), context.describe());
    }

    @Test
    void chartQualityCountsAsQuantityForEveryDropTypeButColonyItems() {
        ChartGenContext polished = ChartGenContext.of(List.of(), 12);

        assertEquals(12f, polished.quantityPercent(), TOLERANCE);
        assertEquals(1.12f, polished.dropMult(DropType.FEEDSTOCK), TOLERANCE);
        assertEquals(1f, polished.dropMult(DropType.COLONY_ITEM), TOLERANCE);
        assertSame(ItemChances.BASE, ChartGenContext.none().itemChances());
    }

    @Test
    void theLootTotalsListOnlyTheBonusesThatAreNotZero() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 23f),
                new AppliedEffect(ChartEffect.CHART_CURRENCY_PERCENT, 47f), new AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 30f)));

        List<String> lines = ChartLootTotals.lines(context).stream().map(StyledText::plain).toList();

        assertEquals(List.of("Item quantity +23%", "Currency items +47%"), lines);
        ChartGenContext sites = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_STATION_BUDGET_PERCENT, 30f)));
        assertEquals(List.of("Stations +30%"), ChartLootTotals.lines(sites).stream().map(StyledText::plain).toList());
        assertEquals(List.of("Rarity of hull items and subroutines +25%"),
                ChartLootTotals.lines(with(ChartEffect.CHART_ITEM_RARITY_PERCENT, 25f)).stream().map(StyledText::plain).toList());
        assertTrue(ChartLootTotals.lines(with(ChartEffect.CHART_FLEET_SIZE_PERCENT, 30f)).isEmpty());
    }

    @Test
    void afterGenerationWithoutRewardEffectsTouchesNothing() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        MemoryAPI cacheMemory = mock(MemoryAPI.class);
        SectorEntityToken cache = entity("cache_1", "run1", cacheMemory);
        when(system.getEntitiesWithTag(ChartIds.CONTENT_TAG)).thenReturn(List.of(cache));
        ChartGenContext context = with(ChartEffect.CHART_FLEET_SIZE_PERCENT, 30f);

        context.afterGeneration(new ChartGenerated(system, "run1", ChartContent.none(3), context));

        verify(cacheMemory, never()).set(anyString(), anyFloat());
        verify(cache, never()).addDropValue(anyString(), anyInt());
    }

    private static SectorEntityToken entity(String id, String runId, MemoryAPI memory) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getId()).thenReturn(id);
        when(entity.hasTag(ChartIds.runTag(runId))).thenReturn(true);
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        return entity;
    }

    @Test
    void everyEffectDescribesItselfInEnglishAndChinese() throws Exception {
        JSONObject english = new JSONObject(RealCatalogue.entries("en"));
        JSONObject chinese = new JSONObject(RealCatalogue.entries("zh_CN"));
        for (ChartEffect effect : ChartEffect.values()) {
            String key = effect.key();
            assertTrue(english.has(key) || english.has(key + ".other") && english.has(key + ".one"), key);
            assertTrue(chinese.has(key) || chinese.has(key + ".other"), key);
            String description = effect.description(3f).plain();
            assertTrue(description.contains("3") && !description.contains(key), description);
            assertFalse(effect.description(3f).spans().isEmpty(), key);
        }
        assertTrue(ChartEffect.CHART_SENSOR_RANGE_PERCENT.description(-30f).plain().contains("30%"));
        assertFalse(ChartEffect.CHART_SENSOR_RANGE_PERCENT.description(-30f).plain().contains("-30"));
    }

    @Test
    void theCompletionRewardScalesWithTheChart() {
        assertEquals(ChartRewards.creditsFor(6), ChartRewards.creditsFor(6, 1f));
        assertTrue(ChartRewards.creditsFor(6, 1.5f) > ChartRewards.creditsFor(6));
        assertEquals(0, ChartRewards.creditsFor(6, 1.5f) % 1000);
        assertEquals(0, ChartRewards.creditsFor(6, -1f));
    }

    @Test
    void bossSizeModifiersAddUpAndOverflowPastTheCapIntoFleetStrength() {
        boolean sawOverflow = false;
        boolean sawUnderCap = false;
        for (int tier = 10; tier <= 15; tier++) {
            ChartTier base = ChartTiers.forTier(tier);
            ChartGenContext gatedChart = ChartGenContext.from(StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, tier));
            ChartGenContext gatedAndWarded = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 700f),
                    new AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 540f)));
            assertEquals(700f, gatedChart.bossSizePercent(), TOLERANCE, "tier " + tier);
            for (ChartGenContext context : List.of(ChartGenContext.none(), gatedChart, gatedAndWarded)) {
                float bossSize = base.bossSize() + context.bossSizePercent();
                PlannedFleet boss = ChartContentPlanner.plannedBoss(context.adjust(base), 50f, 0);
                assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, bossSize)), boss.combatPoints(), TOLERANCE,
                        "tier " + tier);
                assertEquals(ChartContentPlanner.overflowStrength(bossSize, ChartContentPlanner.BOSS_SIZE_CAP), boss.strengthBonus(), "tier " + tier);
                assertEquals(0.65f, boss.quality(), TOLERANCE, "tier " + tier);
                if (bossSize > ChartContentPlanner.BOSS_SIZE_CAP) {
                    sawOverflow = true;
                    assertTrue(boss.strengthBonus() > 0, "tier " + tier);
                } else {
                    sawUnderCap = true;
                    assertEquals(0, boss.strengthBonus(), "tier " + tier);
                }
            }
        }
        assertTrue(sawOverflow);
        assertTrue(sawUnderCap);
    }

    @Test
    void aPlannedBossFleetCarriesTheChartsBossSize() {
        ChartGenContext gatedChart = ChartGenContext.from(StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, 12));
        ChartTier base = ChartTiers.forTier(12);
        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plan = plan(gatedChart, seed, 12);
            float bossSize = base.bossSize() + gatedChart.bossSizePercent();
            assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, bossSize)), plan.fleetPoints(FleetRole.BOSS), TOLERANCE,
                    "seed " + seed);
        }
        ChartContentPlan plainPlan = plan(ChartGenContext.none(), 3L, 12);
        assertEquals(ChartTier.fleetPoints(base.bossSize()), plainPlan.fleetPoints(FleetRole.BOSS), TOLERANCE);
    }

    @Test
    void rolledFleetsPastTheFleetLimitMergeIntoTheRestAsSizeAndStrength() {
        int limit = StarChartSettings.DEFAULT_FLEET_LIMIT;
        assertEquals(new ChartFleetSpread(limit, 200f, 0), ChartFleetSpread.of(limit, 200f, 0, limit));
        ChartFleetSpread doubled = ChartFleetSpread.of(limit * 2, 200f, 0, limit);
        assertEquals(limit, doubled.fleets());
        assertEquals(350f, doubled.fleetSize(), TOLERANCE, "75% of the cut fleets' size goes to the kept fleets");
        assertEquals(25, doubled.strengthBonus(), "25% of it goes to their strength");
        assertEquals(limit + 2, ChartFleetSpread.of(limit * 2, 200f, 2, limit).fleets(), "crowded fleets still spawn");
        assertEquals(ChartContentPlanner.MAX_FLEETS, ChartFleetSpread.of(limit, 200f, 30, limit).fleets());
        assertEquals(1, ChartFleetSpread.of(2, 200f, -5, limit).fleets());
        ChartFleetSpread squeezed = ChartFleetSpread.of(7, 1100f, 0, 4);
        assertEquals(ChartContentPlanner.FLEET_SIZE_CAP, squeezed.cappedFleetSize(), TOLERANCE);
        assertEquals(ChartContentPlanner.MAX_OVERFLOW_STRENGTH, squeezed.totalStrengthBonus(), "size past the cap adds strength too");

        ChartTier overRolled = new ChartTier(15, 200f, limit * 2, limit * 2, 0, 0, 50f, 100, 100, 0f, 0f, 0f, 0f, false, 0f, 1f, 0f);
        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plain = ChartContentPlanner.plan(seed, overRolled, List.of(), List.of(), faction -> true);
            assertEquals(limit, plain.count(FleetRole.GUARD) + plain.count(FleetRole.PATROL), "seed " + seed);
            assertEquals(limit * ChartTier.fleetPoints(350f), plain.fleetPoints(FleetRole.GUARD) + plain.fleetPoints(FleetRole.PATROL), 1f, "seed " + seed);
            assertTrue(plain.fleets().stream().allMatch(fleet -> fleet.strengthBonus() == 25), plain.fleets().toString());
            ChartContentPlan crowdedPlan = ChartContentPlanner.plan(seed, overRolled, List.of(), List.of(), faction -> true,
                    with(ChartEffect.CHART_EXTRA_FLEETS, 3f));
            assertEquals(limit + 3, crowdedPlan.count(FleetRole.GUARD) + crowdedPlan.count(FleetRole.PATROL), "seed " + seed);
            ChartContentPlan fewer = ChartContentPlanner.plan(seed, overRolled, List.of(), List.of(), faction -> true, ChartGenContext.none(), 4);
            assertEquals(4, fewer.count(FleetRole.GUARD) + fewer.count(FleetRole.PATROL), "seed " + seed);
            assertTrue(fewer.fleets().stream().allMatch(fleet -> fleet.strengthBonus() > 25), fewer.fleets().toString());
        }
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            for (long seed = 0; seed < 20; seed++) {
                ChartContentPlan binary = plan(with(ChartEffect.CHART_BASE_FLEETS, 1f), seed, tier);
                assertTrue(binary.count(FleetRole.GUARD) + binary.count(FleetRole.PATROL) <= limit, "tier " + tier + " seed " + seed);
            }
        }
    }

    @Test
    void chartQualityAddsOnePercentLootAndRewardPerPoint() {
        ChartGenContext polished = ChartGenContext.of(List.of(), 12);
        assertFalse(polished.isEmpty());
        assertEquals(1f, polished.siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertEquals(1.12f, polished.rewardMult(), TOLERANCE);
        assertEquals(BASE.poiBudget(), polished.adjust(BASE).poiBudget(), TOLERANCE);
        assertEquals(BASE.fleetSize(), polished.adjust(BASE).fleetSize(), TOLERANCE);
        assertSame(ChartGenContext.none(), ChartGenContext.of(List.of(), 0));

        ItemData plainData = ItemData.rolledAtTier(StarChartFixtures.definition("star_chart"), 3L, 5);
        StarChart plainChart = (StarChart) plainData.preview();
        StarChart polishedChart = (StarChart) plainData.withTraits(new ItemTraits(5, 12, null, null)).preview();
        ChartGenContext plainContext = ChartGenContext.from(plainChart);
        ChartGenContext polishedContext = ChartGenContext.from(polishedChart);

        assertEquals(0, plainContext.quality());
        assertEquals(12, polishedContext.quality());
        assertEquals(plainContext.rewardMult() + 0.12f, polishedContext.rewardMult(), TOLERANCE);
        assertEquals(plainContext.siteBudgetMult(ChartPoi.Kind.STATION), polishedContext.siteBudgetMult(ChartPoi.Kind.STATION), TOLERANCE);
        assertTrue(ChartRewards.creditsFor(5, polishedContext.rewardMult()) >= ChartRewards.creditsFor(5, plainContext.rewardMult()));
        assertTrue(polishedChart.headerLines().stream().anyMatch(line -> line.plain().contains("12% increased quantity of items found in the system and completion reward")),
                polishedChart.headerLines().toString());
        assertTrue(plainChart.headerLines().stream().noneMatch(line -> line.plain().contains("Quality")));
    }

    @Test
    void eachSiteBudgetEffectRaisesOnlyItsOwnCategory() {
        assertEquals(1.25f, with(ChartEffect.CHART_CACHE_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertEquals(1.25f, with(ChartEffect.CHART_DOMAIN_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.DOMAIN), TOLERANCE);
        assertEquals(1.25f, with(ChartEffect.CHART_STATION_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.STATION), TOLERANCE);
        assertEquals(1.25f, with(ChartEffect.CHART_DERELICT_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.DERELICT), TOLERANCE);

        ChartTier moreStations = with(ChartEffect.CHART_STATION_BUDGET_PERCENT, 50f).adjust(BASE);
        assertEquals(BASE.stationBudget() * 1.5f, moreStations.stationBudget(), TOLERANCE);
        assertEquals(BASE.cacheBudget(), moreStations.cacheBudget(), TOLERANCE);
        assertEquals(BASE.domainBudget(), moreStations.domainBudget(), TOLERANCE);
        assertEquals(BASE.derelictBudget(), moreStations.derelictBudget(), TOLERANCE);
    }

    @Test
    void siteBudgetPercentsAddWithinACategoryAndThePoiBudgetReachesEveryCategory() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_POI_BUDGET_PERCENT, 20f),
                new AppliedEffect(ChartEffect.CHART_DOMAIN_BUDGET_PERCENT, 30f), new AppliedEffect(ChartEffect.CHART_DOMAIN_BUDGET_PERCENT, 10f)));

        assertEquals(1.6f, context.siteBudgetMult(ChartPoi.Kind.DOMAIN), TOLERANCE, "percents add before they multiply");
        assertEquals(1.2f, context.siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertEquals(1.2f, context.siteBudgetMult(ChartPoi.Kind.STATION), TOLERANCE);
        assertEquals(1.2f, context.siteBudgetMult(ChartPoi.Kind.DERELICT), TOLERANCE);
        assertEquals(0f, with(ChartEffect.CHART_CACHE_BUDGET_PERCENT, -150f).siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertTrue(context.describe().contains("site budgets cache x1.20 domain x1.60 station x1.20 derelict x1.20"), context.describe());
    }

    @Test
    void quantityDoesNotRaiseTheSiteBudgets() {
        ChartGenContext context = with(ChartEffect.CHART_QUANTITY_PERCENT, 40f);
        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            assertEquals(1f, context.siteBudgetMult(kind), TOLERANCE, kind.name());
        }
        assertEquals(BASE.poiBudget(), context.adjust(BASE).poiBudget(), TOLERANCE);
    }
    @Test
    void rarityAddsUpAndOnlyShiftsTheRarityOfItemChances() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_ITEM_RARITY_PERCENT, 30f),
                new AppliedEffect(ChartEffect.CHART_ITEM_RARITY_PERCENT, 20f)));

        assertEquals(1.5f, context.itemChances().rarity(), TOLERANCE);
        assertEquals(1f, context.itemChances().general(), TOLERANCE);
        assertEquals(1f, ChartGenContext.none().itemChances().rarity(), TOLERANCE);
        assertEquals(1.5f, context.itemChances().scaled(2f).rarity(), TOLERANCE);
    }
}
