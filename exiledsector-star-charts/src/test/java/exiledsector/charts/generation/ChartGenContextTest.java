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
import exiledsector.charts.effect.ChartBattleRules;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.effect.ChartFleetStat;
import exiledsector.charts.effect.ChartPrefixRewards;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.loot.ChartLootTotals;
import exiledsector.items.drop.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.modifier.ItemRoll;
import exiledsector.items.ItemTraits;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.items.starchart.StarChart;
import exiledsector.items.starchart.StarChartFixtures;
import exiledsector.loot.DropType;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
        ChartPrefixRewards.resetForTests();
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    private static ChartGenContext with(ChartModifier effect, float value) {
        return ChartGenContext.of(List.of(new AppliedEffect(effect, value)));
    }

    @Test
    void noModifiersLeaveTheTierAlone() {
        assertSame(BASE, ChartGenContext.none().adjust(BASE));
        assertSame(ChartGenContext.none(), ChartGenContext.from(null));
        assertEquals(1f, ChartGenContext.none().sensorRangeMult());
        assertEquals(0, ChartGenContext.none().extraPois());
    }

    @Test
    void aChartsModifiersAreReadDeterministicallyWithTheirTierValues() {
        StarChart chart = StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, 12);
        ChartGenContext first = ChartGenContext.from(chart);
        ChartGenContext second = ChartGenContext.from(StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, 12));

        assertEquals(first.applied(), second.applied());
        assertTrue(first.applied().contains(new AppliedEffect(ChartModifier.CHART_BOSS_SIZE_PERCENT, 700f)), first.applied().toString());
        assertTrue(first.applied().contains(new AppliedEffect(ChartModifier.CHART_OFFICER_LEVEL_BONUS, 2f)), first.applied().toString());
        assertTrue(chart.implicits().isEmpty(), "charts no longer roll a faction implicit; the atlas node decides the faction");
        assertEquals(first.factionId(), second.factionId());
        assertEquals(700f, first.bossSizePercent(), TOLERANCE);
        assertEquals(BASE.bossSize() + 700f, first.adjust(BASE).bossSize(), TOLERANCE, "boss size modifiers add percentage points");
    }

    @Test
    void eachDifficultyEffectChangesTheFleets() {
        assertEquals(BASE.fleetSize() + 50f, with(ChartModifier.CHART_FLEET_SIZE_PERCENT, 50f).adjust(BASE).fleetSize(), TOLERANCE);
        ChartTier moreRolled = with(ChartModifier.CHART_BASE_FLEETS, 2f).adjust(BASE);
        assertEquals(BASE.fleetsMin() + 2, moreRolled.fleetsMin());
        assertEquals(BASE.fleetsMax() + 2, moreRolled.fleetsMax());
        assertEquals(BASE.fleetSize(), moreRolled.fleetSize(), TOLERANCE, "extra fleets are full size, not a thinner split");
        ChartGenContext crowded = with(ChartModifier.CHART_EXTRA_FLEETS, 2f);
        assertEquals(BASE, crowded.adjust(BASE), "crowded fleets are added after the soft limit, not to the roll");
        assertEquals(2, crowded.extraFleets());
        assertEquals(BASE.officerLevelBonus() + 1, with(ChartModifier.CHART_OFFICER_LEVEL_BONUS, 1f).adjust(BASE).officerLevelBonus());
        assertEquals(BASE.officerCountBonus() + 2, with(ChartModifier.CHART_EXTRA_OFFICERS, 2f).adjust(BASE).officerCountBonus());
        assertEquals(BASE.bossSize() + 40f, with(ChartModifier.CHART_BOSS_SIZE_PERCENT, 40f).adjust(BASE).bossSize(), TOLERANCE);
        ChartTier noBoss = new ChartTier(BASE.tier(), BASE.fleetSize(), BASE.fleetsMin(), BASE.fleetsMax(), BASE.officerLevelBonus(),
                BASE.officerCountBonus(), BASE.fleetQuality(), BASE.levelMin(), BASE.levelMax(), BASE.cacheBudget(), BASE.domainBudget(),
                BASE.stationBudget(), BASE.derelictBudget(), false, 0f);
        assertEquals(0f, with(ChartModifier.CHART_BOSS_SIZE_PERCENT, 40f).adjust(noBoss).bossSize(), TOLERANCE,
                "a tier switched off from having a boss stays without one");
        assertEquals(BASE.fleetQuality() + 20f, with(ChartModifier.CHART_FLEET_QUALITY_PERCENT, 20f).adjust(BASE).fleetQuality(), TOLERANCE);
        assertEquals(BASE.fleetSize(), with(ChartModifier.CHART_FLEET_QUALITY_PERCENT, 20f).adjust(BASE).fleetSize(), TOLERANCE,
                "fleet quality and fleet size are separate");
        assertEquals(BASE.poiBudget(), with(ChartModifier.CHART_FLEET_SIZE_PERCENT, 50f).adjust(BASE).poiBudget(), TOLERANCE);
    }

    @Test
    void theBossStarChartLineAddsToTheOtherStarChartLinesForBossesOnly() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_STAR_CHART_PERCENT, 20f),
                new AppliedEffect(ChartModifier.CHART_STAR_CHART_PERCENT, 30f)));

        assertEquals(1.5f, context.bossStarChartMult(0f), TOLERANCE);
        assertEquals(1.3f, context.dropMult(DropType.STAR_CHART), TOLERANCE);
        assertEquals(1f, ChartGenContext.none().bossStarChartMult(0f), TOLERANCE);
    }

    @Test
    void eachLootEffectChangesTheLoot() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_POI_BUDGET_PERCENT, 30f),
                new AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 10f),
                new AppliedEffect(ChartModifier.CHART_EXTRA_POIS, 3f), new AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 50f)));

        assertEquals(BASE.poiBudget() * 1.3f, context.adjust(BASE).poiBudget(), TOLERANCE, "quantity does not raise the site budgets");
        assertEquals(1.1f, context.dropMult(DropType.FEEDSTOCK), TOLERANCE);
        assertEquals(3, context.extraPois());
        assertEquals(1.6f, context.dropMult(DropType.BRIDGE), TOLERANCE);
        assertEquals(1.1f, context.dropMult(DropType.SUBROUTINE), TOLERANCE, "the hull item line leaves subroutines out");
        assertEquals(1.1f, context.dropMult(DropType.STAR_CHART), TOLERANCE, "the hull item line leaves Star Charts out");
        assertEquals(BASE.fleetSize(), context.adjust(BASE).fleetSize(), TOLERANCE);
        assertEquals(0.7f, with(ChartModifier.CHART_SENSOR_RANGE_PERCENT, -30f).sensorRangeMult(), TOLERANCE);
        assertEquals(ChartGenContext.MIN_SENSOR_RANGE_MULT, with(ChartModifier.CHART_SENSOR_RANGE_PERCENT, -400f).sensorRangeMult(), TOLERANCE);
        assertTrue(context.describe().contains("site budgets cache x1.30"), context.describe());
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
        ChartGenContext harder = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_FLEET_SIZE_PERCENT, 60f),
                new AppliedEffect(ChartModifier.CHART_EXTRA_FLEETS, 2f), new AppliedEffect(ChartModifier.CHART_OFFICER_LEVEL_BONUS, 2f)));
        ChartGenContext richer = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_EXTRA_POIS, 3f),
                new AppliedEffect(ChartModifier.CHART_POI_BUDGET_PERCENT, 50f)));

        assertEquals(plan(harder, seed), plan(harder, seed));
        assertEquals(plan(richer, seed), plan(richer, seed));
        assertEquals(base, ChartContentPlanner.plan(seed, ChartTiers.forTier(8), ChartPois.all(), ChartFactions.all(), faction -> true));
        ChartContentPlan harderPlan = plan(harder, seed);
        float basePoints = base.fleetPoints(FleetRole.GUARD) + base.fleetPoints(FleetRole.PATROL);
        float harderPoints = harderPlan.fleetPoints(FleetRole.GUARD) + harderPlan.fleetPoints(FleetRole.PATROL);
        assertTrue(harderPoints > basePoints, basePoints + " -> " + harderPoints);
        assertTrue(harderPlan.count(FleetRole.GUARD) + harderPlan.count(FleetRole.PATROL)
                > base.count(FleetRole.GUARD) + base.count(FleetRole.PATROL));
        assertTrue(harderPlan.fleets().stream().allMatch(fleet -> fleet.strength().officerLevelBonus() >= 2));
        ChartContentPlan richerPlan = plan(richer, seed);
        assertTrue(richerPlan.pois().size() >= base.pois().size() + 3, base.pois().size() + " -> " + richerPlan.pois().size());
        assertTrue(richerPlan.lootSpent() > base.lootSpent());
        assertNotEquals(base, richerPlan);
    }

    @Test
    void dropBonusesAddWithinATypeAndStayApartAcrossTypes() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 10f),
                new AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 20f), new AppliedEffect(ChartModifier.CHART_REACTOR_ITEM_PERCENT, 40f),
                new AppliedEffect(ChartModifier.CHART_REACTOR_ITEM_PERCENT, 10f), new AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 30f)));

        assertEquals(1.8f, context.dropMult(DropType.REACTOR), TOLERANCE);
        assertEquals(1.3f, context.dropMult(DropType.BRIDGE), TOLERANCE);
        assertEquals(1.3f, context.dropMult(DropType.HULL_ITEM), TOLERANCE);
        assertEquals(1.1f, context.dropMult(DropType.STAR_CHART), TOLERANCE);
        assertEquals(1.1f, context.dropMult(DropType.SUBROUTINE), TOLERANCE);
        assertEquals(1.4f, context.dropMult(DropType.CRAFTING_ITEM), TOLERANCE, "the hull item line only reaches hull items");
        assertEquals(1.1f, context.dropMult(DropType.WEAPON), TOLERANCE);
        assertEquals(1.1f, context.dropMult(DropType.COLONY_ITEM), TOLERANCE, "colony items read the item quantity too");
        assertEquals(0.5f, context.itemChances().extraOverParent(DropType.REACTOR), TOLERANCE);
        assertEquals(0f, context.itemChances().extraOverParent(DropType.BRIDGE), TOLERANCE);
        assertTrue(context.describe().contains("reactor +50%"), context.describe());
    }

    @Test
    void chartQualityCountsAsQuantityForEveryDropType() {
        ChartGenContext polished = ChartGenContext.of(List.of(), 12);

        assertEquals(12f, polished.quantityPercent(), TOLERANCE);
        for (DropType type : DropType.values()) {
            assertEquals(1.12f, polished.dropMult(type), TOLERANCE, type.name());
        }
        assertEquals(ItemChances.BASE, ChartGenContext.none().itemChances());
    }

    @Test
    void theLootTotalsListOnlyTheBonusesThatAreNotZero() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 23f),
                new AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 47f), new AppliedEffect(ChartModifier.CHART_FLEET_SIZE_PERCENT, 30f)));

        List<String> lines = ChartLootTotals.lines(context).stream().map(StyledText::plain).toList();

        assertEquals(List.of("Item quantity +23%", "Enemy fleet size +30%", "Crafting items +47%"), lines);
        ChartGenContext sites = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_STATION_BUDGET_PERCENT, 30f)));
        assertEquals(List.of("Stations +30%"), ChartLootTotals.lines(sites).stream().map(StyledText::plain).toList());
        assertEquals(List.of("Rarity of hull items, unique items and crafting items +25%"),
                ChartLootTotals.lines(with(ChartModifier.CHART_ITEM_RARITY_PERCENT, 25f)).stream().map(StyledText::plain).toList());
        assertTrue(ChartLootTotals.lines(with(ChartModifier.CHART_BOSS_SIZE_PERCENT, 30f)).isEmpty());
    }

    @Test
    void afterGenerationWithoutRewardEffectsTouchesNothing() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        MemoryAPI cacheMemory = mock(MemoryAPI.class);
        SectorEntityToken cache = entity("cache_1", "run1", cacheMemory);
        when(system.getEntitiesWithTag(ChartIds.CONTENT_TAG)).thenReturn(List.of(cache));
        ChartGenContext context = with(ChartModifier.CHART_FLEET_SIZE_PERCENT, 30f);

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
        for (ChartModifier effect : ChartModifier.values()) {
            String key = effect.key();
            assertTrue(english.has(key) || english.has(key + ".other") && english.has(key + ".one"), key);
            assertTrue(chinese.has(key) || chinese.has(key + ".other"), key);
            String description = effect.description(3f).plain();
            assertTrue(description.contains("3") != effect.isFlag() && !description.contains(key), description);
            assertFalse(effect.description(3f).spans().isEmpty(), key);
        }
        assertTrue(ChartModifier.CHART_SENSOR_RANGE_PERCENT.description(-30f).plain().contains("30%"));
        assertFalse(ChartModifier.CHART_SENSOR_RANGE_PERCENT.description(-30f).plain().contains("-30"));
    }

    @Test
    void modifierEffectScalesTheChartsOwnLinesAndPrefixRewardsButNotAddedLines() throws Exception {
        ChartPrefixRewards.register(new JSONArray().put(new JSONObject().put("id", "TEST_GATED_BOSS_SIZE").put("item_quantity", "low=5/mid=15/high=25")));
        ChartGenContext plain = ChartGenContext.from(StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, 12))
                .withEffects(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_SIZE_PERCENT, 10f)));
        ChartGenContext scaled = plain.withEffects(List.of(new AppliedEffect(ChartModifier.CHART_MODIFIER_EFFECT_PERCENT, 30f),
                new AppliedEffect(ChartModifier.CHART_MODIFIER_EFFECT_PERCENT, 20f)));
        float prefixQuantity = (float) plain.applied().stream().filter(line -> line.effect() == ChartModifier.CHART_QUANTITY_PERCENT)
                .mapToDouble(AppliedEffect::value).sum();

        assertEquals(50f, scaled.modifierEffectPercent(), TOLERANCE);
        assertEquals(700f * 1.5f + 10f, scaled.bossSizePercent(), TOLERANCE);
        assertEquals(3, scaled.officerLevelBonus());
        assertEquals(25f, prefixQuantity, TOLERANCE);
        assertEquals(plain.quantityPercent() + prefixQuantity * 0.5f, scaled.quantityPercent(), TOLERANCE);
        assertEquals(10f, with(ChartModifier.CHART_BOSS_SIZE_PERCENT, 10f).withEffects(List.of(
                new AppliedEffect(ChartModifier.CHART_MODIFIER_EFFECT_PERCENT, 50f))).bossSizePercent(), TOLERANCE);
    }

    @Test
    void modifierEffectMakesMisfiresMoreFrequentLikeEveryOtherDrawbackGrows() {
        assertEquals(15f, ChartModifier.CHART_SHIELD_DAMAGE_TAKEN_PERCENT.withModifierEffect(10f, 1.5f), TOLERANCE);
        assertEquals(20f, ChartModifier.CHART_WEAPON_MISFIRE_SECONDS.withModifierEffect(30f, 1.5f), TOLERANCE);
    }

    @Test
    void modifierEffectRoundsWholeNumberLinesDownUntilTheyReachTheNextWholeNumber() {
        assertEquals(1f, ChartModifier.CHART_EXTRA_FLEETS.withModifierEffect(1f, 1.99f), TOLERANCE);
        assertEquals(2f, ChartModifier.CHART_EXTRA_FLEETS.withModifierEffect(1f, 2f), TOLERANCE);
        assertEquals(4f, ChartModifier.CHART_EXTRA_POIS.withModifierEffect(3f, 1.5f), TOLERANCE);
        assertEquals(3f, ChartModifier.CHART_OFFICER_LEVEL_BONUS.withModifierEffect(2f, 1.5f), TOLERANCE);
        assertEquals(-1f, ChartModifier.CHART_EXTRA_FLEETS.withModifierEffect(-1f, 1.5f), TOLERANCE);
    }

    @Test
    void bossSizeModifiersAddUpAndOverflowPastTheCapIntoFleetStrength() {
        boolean sawOverflow = false;
        boolean sawUnderCap = false;
        for (int tier = 10; tier <= 15; tier++) {
            ChartTier base = ChartTiers.forTier(tier);
            ChartGenContext gatedChart = ChartGenContext.from(StarChartFixtures.fullChart(StarChartFixtures.TEST_GATED, 5L, tier));
            ChartGenContext gatedAndWarded = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_SIZE_PERCENT, 700f),
                    new AppliedEffect(ChartModifier.CHART_BOSS_SIZE_PERCENT, 540f)));
            assertEquals(700f, gatedChart.bossSizePercent(), TOLERANCE, "tier " + tier);
            for (ChartGenContext context : List.of(ChartGenContext.none(), gatedChart, gatedAndWarded)) {
                float bossSize = base.bossSize() + context.bossSizePercent();
                ChartTier adjusted = context.adjust(base);
                PlannedFleet boss = ChartContentPlanner.plannedBoss(adjusted, 0);
                assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, bossSize)), boss.strength().combatPoints(), TOLERANCE,
                        "tier " + tier);
                assertEquals(ChartContentPlanner.overflowStrength(bossSize, ChartContentPlanner.BOSS_SIZE_CAP), boss.strengthBonus(), "tier " + tier);
                assertEquals(ChartContentPlanner.bossQuality(ChartContentPlanner.fleetQuality(adjusted)) / ChartTier.PERCENT, boss.strength().quality(),
                        TOLERANCE, "tier " + tier);
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
        assertEquals(new ChartFleetSpread(limit, 50f, 0), ChartFleetSpread.of(limit, 50f, 0, limit));
        ChartFleetSpread doubled = ChartFleetSpread.of(limit * 2, 50f, 0, limit);
        assertEquals(limit, doubled.fleets());
        assertEquals(87.5f, doubled.fleetSize(), TOLERANCE, "75% of the cut fleets' size goes to the kept fleets");
        assertEquals(25, doubled.strengthBonus(), "25% of it goes to their strength");
        assertEquals(limit + 2, ChartFleetSpread.of(limit * 2, 50f, 2, limit).fleets(), "crowded fleets still spawn");
        assertEquals(ChartContentPlanner.MAX_FLEETS, ChartFleetSpread.of(limit, 50f, 30, limit).fleets());
        assertEquals(1, ChartFleetSpread.of(2, 50f, -5, limit).fleets());
        ChartFleetSpread squeezed = ChartFleetSpread.of(7, 413f, 0, 4);
        assertEquals(ChartContentPlanner.FLEET_SIZE_CAP, squeezed.cappedFleetSize(), TOLERANCE);
        assertEquals(ChartContentPlanner.MAX_OVERFLOW_STRENGTH, squeezed.totalStrengthBonus(), "size past the cap adds strength too");

        ChartTier overRolled = new ChartTier(15, 50f, limit * 2, limit * 2, 0, 0, 50f, 100, 100, 0f, 0f, 0f, 0f, false, 0f);
        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plain = ChartContentPlanner.plan(seed, overRolled, List.of(), List.of(), faction -> true);
            assertEquals(limit, plain.count(FleetRole.GUARD) + plain.count(FleetRole.PATROL), "seed " + seed);
            assertEquals(limit * ChartTier.fleetPoints(87.5f), plain.fleetPoints(FleetRole.GUARD) + plain.fleetPoints(FleetRole.PATROL), 1f, "seed " + seed);
            assertTrue(plain.fleets().stream().allMatch(fleet -> fleet.strengthBonus() == 25), plain.fleets().toString());
            ChartContentPlan crowdedPlan = ChartContentPlanner.plan(seed, overRolled, List.of(), List.of(), faction -> true,
                    with(ChartModifier.CHART_EXTRA_FLEETS, 3f));
            assertEquals(limit + 3, crowdedPlan.count(FleetRole.GUARD) + crowdedPlan.count(FleetRole.PATROL), "seed " + seed);
            ChartContentPlan fewer = ChartContentPlanner.plan(seed, overRolled, List.of(), List.of(), faction -> true, ChartGenContext.none(), 4);
            assertEquals(4, fewer.count(FleetRole.GUARD) + fewer.count(FleetRole.PATROL), "seed " + seed);
            assertTrue(fewer.fleets().stream().allMatch(fleet -> fleet.strengthBonus() > 25), fewer.fleets().toString());
        }
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_DROP_TIER; tier++) {
            for (long seed = 0; seed < 20; seed++) {
                ChartContentPlan binary = plan(with(ChartModifier.CHART_BASE_FLEETS, 1f), seed, tier);
                assertTrue(binary.count(FleetRole.GUARD) + binary.count(FleetRole.PATROL) <= limit, "tier " + tier + " seed " + seed);
            }
        }
    }

    @Test
    void chartQualityAddsOnePercentItemQuantityPerPoint() {
        ChartGenContext polished = ChartGenContext.of(List.of(), 12);
        assertFalse(polished.isEmpty());
        assertEquals(1f, polished.siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertEquals(1.12f, polished.dropMult(DropType.FEEDSTOCK), TOLERANCE);
        assertEquals(BASE.poiBudget(), polished.adjust(BASE).poiBudget(), TOLERANCE);
        assertEquals(BASE.fleetSize(), polished.adjust(BASE).fleetSize(), TOLERANCE);
        assertSame(ChartGenContext.none(), ChartGenContext.of(List.of(), 0));

        ItemData plainData = ItemData.rolled(StarChartFixtures.definition("star_chart"), 3L, ItemRoll.atTier(5));
        StarChart plainChart = (StarChart) plainData.preview();
        StarChart polishedChart = (StarChart) plainData.withTraits(new ItemTraits(5, 12, null, null)).preview();
        ChartGenContext plainContext = ChartGenContext.from(plainChart);
        ChartGenContext polishedContext = ChartGenContext.from(polishedChart);

        assertEquals(0, plainContext.quantity());
        assertEquals(12, polishedContext.quantity());
        assertEquals(plainContext.dropMult(DropType.FEEDSTOCK) + 0.12f, polishedContext.dropMult(DropType.FEEDSTOCK), TOLERANCE);
        assertEquals(plainContext.siteBudgetMult(ChartPoi.Kind.STATION), polishedContext.siteBudgetMult(ChartPoi.Kind.STATION), TOLERANCE);
        assertTrue(polishedChart.headerLines().stream().anyMatch(line -> line.plain().contains("12% increased quantity of items found in the system")),
                polishedChart.headerLines().toString());
        assertTrue(plainChart.headerLines().stream().noneMatch(line -> line.plain().contains("Quality")));
    }

    @Test
    void aChartThatRolledTheRetiredBountyModifierStillLoadsAndItsLineDoesNothing() {
        ItemData rolled = StarChartFixtures.withEveryModifier(StarChartFixtures.TEST_GATED, 5L, 12);
        List<RolledModifier> explicits = new ArrayList<>(rolled.explicits());
        explicits.add(new RolledModifier("CHART_REWARD_PERCENT", ModifierSlot.SUFFIX, List.of(new RolledLine("CHART_REWARD_PERCENT", 30f))));
        ItemData withBounty = ItemData.parse(new ItemData(rolled.definitionId(), rolled.seed(), explicits, rolled.frozenName(), rolled.traits()).encode());

        StarChart plain = (StarChart) rolled.preview();
        StarChart bounty = (StarChart) withBounty.preview();

        assertEquals(explicits.size(), withBounty.explicits().size());
        assertEquals(ChartGenContext.from(plain).describe(), ChartGenContext.from(bounty).describe());
        assertEquals(plain.slottedEffectLines(false, null).stream().map(line -> line.text().plain()).toList(),
                bounty.slottedEffectLines(false, null).stream().map(line -> line.text().plain()).toList());
    }

    @Test
    void eachSiteBudgetEffectRaisesOnlyItsOwnCategory() {
        assertEquals(1.25f, with(ChartModifier.CHART_CACHE_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertEquals(1.25f, with(ChartModifier.CHART_DOMAIN_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.DOMAIN), TOLERANCE);
        assertEquals(1.25f, with(ChartModifier.CHART_STATION_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.STATION), TOLERANCE);
        assertEquals(1.25f, with(ChartModifier.CHART_DERELICT_BUDGET_PERCENT, 25f).siteBudgetMult(ChartPoi.Kind.DERELICT), TOLERANCE);

        ChartTier moreStations = with(ChartModifier.CHART_STATION_BUDGET_PERCENT, 50f).adjust(BASE);
        assertEquals(BASE.stationBudget() * 1.5f, moreStations.stationBudget(), TOLERANCE);
        assertEquals(BASE.cacheBudget(), moreStations.cacheBudget(), TOLERANCE);
        assertEquals(BASE.domainBudget(), moreStations.domainBudget(), TOLERANCE);
        assertEquals(BASE.derelictBudget(), moreStations.derelictBudget(), TOLERANCE);
    }

    @Test
    void siteBudgetPercentsAddWithinACategoryAndThePoiBudgetReachesEveryCategory() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_POI_BUDGET_PERCENT, 20f),
                new AppliedEffect(ChartModifier.CHART_DOMAIN_BUDGET_PERCENT, 30f), new AppliedEffect(ChartModifier.CHART_DOMAIN_BUDGET_PERCENT, 10f)));

        assertEquals(1.6f, context.siteBudgetMult(ChartPoi.Kind.DOMAIN), TOLERANCE, "percents add before they multiply");
        assertEquals(1.2f, context.siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertEquals(1.2f, context.siteBudgetMult(ChartPoi.Kind.STATION), TOLERANCE);
        assertEquals(1.2f, context.siteBudgetMult(ChartPoi.Kind.DERELICT), TOLERANCE);
        assertEquals(0f, with(ChartModifier.CHART_CACHE_BUDGET_PERCENT, -150f).siteBudgetMult(ChartPoi.Kind.CACHE), TOLERANCE);
        assertTrue(context.describe().contains("site budgets cache x1.20 domain x1.60 station x1.20 derelict x1.20"), context.describe());
    }

    @Test
    void quantityDoesNotRaiseTheSiteBudgets() {
        ChartGenContext context = with(ChartModifier.CHART_QUANTITY_PERCENT, 40f);
        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            assertEquals(1f, context.siteBudgetMult(kind), TOLERANCE, kind.name());
        }
        assertEquals(BASE.poiBudget(), context.adjust(BASE).poiBudget(), TOLERANCE);
    }
    @Test
    void itemRarityAddsUpAtTheRootAndOnlyShiftsTheRarityOfItemChances() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 30f),
                new AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 20f)));

        assertEquals(50f, context.rarityPercent(DropType.ITEM), TOLERANCE);
        assertEquals(50f, context.itemRarityPercent(), TOLERANCE);
        assertEquals(0f, context.rarityPercent(DropType.HULL_ITEM), TOLERANCE);
        for (DropType type : DropType.values()) {
            assertEquals(1.5f, context.itemChances().rarity(type), TOLERANCE, type.name());
            assertEquals(1f, context.itemChances().quantity(type), TOLERANCE, type.name());
            assertEquals(1f, ChartGenContext.none().itemChances().rarity(type), TOLERANCE, type.name());
        }
        assertEquals(1.5f, context.itemChances().scaled(2f).rarity(DropType.BRIDGE), TOLERANCE);
    }

    @Test
    void aNodesRarityPercentReachesOnlyTheDropTypesBelowIt() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_INHIBITOR_PERCENT, 0f)));
        context.addRarityPercent(DropType.INHIBITOR, 40f);

        assertEquals(1.4f, context.itemChances().rarity(DropType.INHIBITOR), TOLERANCE);
        assertEquals(1f, context.itemChances().rarity(DropType.ENGRAM), TOLERANCE);
        assertEquals(1f, context.itemChances().rarity(DropType.CRAFTING_ITEM), TOLERANCE);
        assertEquals(0f, context.itemRarityPercent(), TOLERANCE);
    }

    @Test
    void theSchematicAndCraftingSubcategoryLinesAddAtTheirOwnNodes() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_SCHEMATIC_PERCENT, 10f),
                new AppliedEffect(ChartModifier.CHART_BASIC_CRAFTING_ITEM_PERCENT, 20f), new AppliedEffect(ChartModifier.CHART_ENGRAM_PERCENT, 30f),
                new AppliedEffect(ChartModifier.CHART_INHIBITOR_PERCENT, 40f), new AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 50f)));

        assertEquals(1.1f, context.itemChances().quantity(DropType.SCHEMATIC), TOLERANCE);
        assertEquals(1f, context.itemChances().quantity(DropType.HULL_ITEM), TOLERANCE);
        assertEquals(1.5f, context.itemChances().quantity(DropType.CRAFTING_ITEM), TOLERANCE);
        assertEquals(1.7f, context.itemChances().quantity(DropType.BASIC_CRAFTING_ITEM), TOLERANCE);
        assertEquals(1.8f, context.itemChances().quantity(DropType.ENGRAM), TOLERANCE);
        assertEquals(1.9f, context.itemChances().quantity(DropType.INHIBITOR), TOLERANCE);
        for (DropType node : List.of(DropType.SCHEMATIC, DropType.BASIC_CRAFTING_ITEM, DropType.ENGRAM, DropType.INHIBITOR)) {
            assertEquals(node, ChartModifier.forDropType(node).dropType());
        }
    }

    @Test
    void systemRuleLinesSetTheirFlagsAndEnemyNodeGrantsCollapse() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_TWINNED_BOSS, 1f),
                new AppliedEffect(ChartModifier.CHART_NO_COMBAT_CRUISERS, 1f), new AppliedEffect(ChartModifier.CHART_NO_COMBAT_FRIGATES, 1f),
                new AppliedEffect(ChartModifier.CHART_SHIELD_DAMAGE_TAKEN_PERCENT, 20f), new AppliedEffect(ChartModifier.CHART_UNSHIELDED_SHIELD_DAMAGE_TAKEN_PERCENT, 100f),
                new AppliedEffect(ChartModifier.CHART_ARMOR_PERCENT, -90f), new AppliedEffect(ChartModifier.CHART_NO_SHIP_SYSTEMS, 1f),
                new AppliedEffect(ChartModifier.CHART_COMMAND_POINT_RATE_PERCENT, -40f),
                new AppliedEffect(ChartModifier.CHART_SEVERED_COMMAND_POINT_RATE_PERCENT, -90f), new AppliedEffect(ChartModifier.CHART_FORCED_AUTOPILOT, 1f),
                new AppliedEffect(ChartModifier.CHART_ARC_STRIKE_EMP_FLAT, 800f), new AppliedEffect(ChartModifier.CHART_WEAPON_MISFIRE_SECONDS, 30f),
                new AppliedEffect(ChartModifier.CHART_WEAPON_AND_ENGINE_REPAIR_SPEED_PERCENT, -40f),
                new AppliedEffect(ChartModifier.CHART_ENEMY_REFRACTING_PROJECTILES, 1f),
                new AppliedEffect(ChartModifier.CHART_ENEMY_REFRACTING_PROJECTILES, 1f)));

        assertTrue(context.twinnedBoss());
        assertEquals(Set.of(HullSize.CRUISER, HullSize.FRIGATE), context.bannedCombatHullSizes());
        assertTrue(context.battleRules().shipSystemsDisabled());
        assertTrue(context.battleRules().autopilotForced());
        assertEquals(800f, context.battleRules().arcStrikeEmp(), TOLERANCE);
        assertEquals(1f / 30f, context.battleRules().weaponMisfireRate(), TOLERANCE);
        assertEquals(0.6f, context.fleetStatMults().get(ChartFleetStat.WEAPON_AND_ENGINE_REPAIR_SPEED), TOLERANCE);
        assertEquals(2.2f, context.fleetStatMults().get(ChartFleetStat.SHIELD_DAMAGE_TAKEN), TOLERANCE);
        assertEquals(0.1f, context.fleetStatMults().get(ChartFleetStat.ARMOR), TOLERANCE);
        assertEquals(0.1f, context.fleetStatMults().get(ChartFleetStat.COMMAND_POINT_RATE), TOLERANCE);
        assertEquals(Set.of("refracting_projectiles_1"), context.enemyGrantedNodeIds());
        assertFalse(ChartGenContext.none().twinnedBoss());
        assertTrue(ChartGenContext.none().bannedCombatHullSizes().isEmpty());
        assertEquals(ChartBattleRules.NONE, ChartGenContext.none().battleRules());
    }
}
