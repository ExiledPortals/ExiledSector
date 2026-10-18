package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.items.ItemData;
import exiledsector.items.ItemTraits;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChartGenContextTest {

    private static final ChartTier BASE = ChartTier.fallback(8);
    private static final float TOLERANCE = 0.001f;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        ChartContentDataTest.loadShippedTables();
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
        StarChart chart = StarChartFixtures.chart("unique_chart_wardens_vigil", 5L, 12);
        ChartGenContext first = ChartGenContext.from(chart);
        ChartGenContext second = ChartGenContext.from(StarChartFixtures.chart("unique_chart_wardens_vigil", 5L, 12));

        assertEquals(first.applied(), second.applied());
        assertTrue(first.applied().contains(new AppliedEffect(ChartEffect.CHART_BOSS_POWER_PERCENT, 80f)), first.applied().toString());
        assertTrue(first.applied().contains(new AppliedEffect(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 150f)), first.applied().toString());
        assertTrue(first.applied().contains(new AppliedEffect(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 2f)), first.applied().toString());
        assertTrue(first.applied().stream().anyMatch(applied -> applied.effect() == ChartEffect.CHART_LOOT_BUDGET_PERCENT),
                "the trinary implicit is read too");
        assertEquals(1.8f, first.bossPowerMult(), TOLERANCE);
        assertEquals(BASE.bossPower(), first.adjust(BASE).bossPower(), TOLERANCE, "the planner applies the boss percentage after the cap");
    }

    @Test
    void eachDifficultyEffectChangesTheFleetBudget() {
        float averageFleets = (BASE.fleetsMin() + BASE.fleetsMax()) / 2f;

        assertEquals(BASE.fleetPower() * 1.5f, with(ChartEffect.CHART_FLEET_POWER_PERCENT, 50f).adjust(BASE).fleetPower(), TOLERANCE);
        ChartTier moreFleets = with(ChartEffect.CHART_EXTRA_FLEETS, 2f).adjust(BASE);
        assertEquals(BASE.fleetsMin() + 2, moreFleets.fleetsMin());
        assertEquals(BASE.fleetsMax() + 2, moreFleets.fleetsMax());
        assertEquals(BASE.fleetPower() * (1f + 2f / averageFleets), moreFleets.fleetPower(), TOLERANCE);
        assertEquals(BASE.officerLevelBonus() + 1, with(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 1f).adjust(BASE).officerLevelBonus());
        assertEquals(BASE.officerCountBonus() + 2, with(ChartEffect.CHART_EXTRA_OFFICERS, 2f).adjust(BASE).officerCountBonus());
        assertEquals(BASE.averageSMods() + 1, with(ChartEffect.CHART_AVERAGE_SMODS_BONUS, 1f).adjust(BASE).averageSMods());
        assertEquals(1.4f, with(ChartEffect.CHART_BOSS_POWER_PERCENT, 40f).bossPowerMult(), TOLERANCE);
        assertEquals(BASE.quality() + 0.2f, with(ChartEffect.CHART_FLEET_QUALITY_PERCENT, 20f).adjust(BASE).quality(), TOLERANCE);
        assertEquals(BASE.lootBudget(), with(ChartEffect.CHART_FLEET_POWER_PERCENT, 50f).adjust(BASE).lootBudget(), TOLERANCE);
    }

    @Test
    void eachRewardEffectChangesTheLootOrTheReward() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_LOOT_BUDGET_PERCENT, 30f),
                new AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 10f), new AppliedEffect(ChartEffect.CHART_REWARD_PERCENT, 20f),
                new AppliedEffect(ChartEffect.CHART_EXTRA_POIS, 3f), new AppliedEffect(ChartEffect.CHART_ITEM_DROP_CHANCE_PERCENT, 50f),
                new AppliedEffect(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 100f)));

        assertEquals(BASE.lootBudget() * 1.4f, context.adjust(BASE).lootBudget(), TOLERANCE);
        assertEquals(1.3f, context.rewardMult(), TOLERANCE);
        assertEquals(3, context.extraPois());
        assertEquals(1.5f, context.itemChanceMult(), TOLERANCE);
        assertEquals(3f, context.vaultItemChanceMult(), TOLERANCE);
        assertEquals(BASE.fleetPower(), context.adjust(BASE).fleetPower(), TOLERANCE);
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
        ChartGenContext harder = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_FLEET_POWER_PERCENT, 60f),
                new AppliedEffect(ChartEffect.CHART_EXTRA_FLEETS, 2f), new AppliedEffect(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 2f)));
        ChartGenContext richer = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_EXTRA_POIS, 3f),
                new AppliedEffect(ChartEffect.CHART_LOOT_BUDGET_PERCENT, 50f)));

        assertEquals(plan(harder, seed), plan(harder, seed));
        assertEquals(plan(richer, seed), plan(richer, seed));
        assertEquals(base, ChartContentPlanner.plan(seed, ChartTiers.forTier(8), ChartPois.all(), ChartFactions.all(), faction -> true));
        ChartContentPlan harderPlan = plan(harder, seed);
        float basePower = base.fleetPower(FleetRole.GUARD) + base.fleetPower(FleetRole.PATROL);
        float harderPower = harderPlan.fleetPower(FleetRole.GUARD) + harderPlan.fleetPower(FleetRole.PATROL);
        assertTrue(harderPower > basePower, basePower + " -> " + harderPower);
        assertTrue(harderPlan.count(FleetRole.GUARD) + harderPlan.count(FleetRole.PATROL)
                > base.count(FleetRole.GUARD) + base.count(FleetRole.PATROL));
        assertTrue(harderPlan.fleets().stream().allMatch(fleet -> fleet.officerLevelBonus() >= 2));
        ChartContentPlan richerPlan = plan(richer, seed);
        assertTrue(richerPlan.pois().size() >= base.pois().size() + 3, base.pois().size() + " -> " + richerPlan.pois().size());
        assertTrue(richerPlan.lootSpent() > base.lootSpent());
        assertNotEquals(base, richerPlan);
    }

    @Test
    void afterGenerationMarksOnlyThisRunsSalvageAndEnrichesTheVault() {
        StarSystemAPI pocket = mock(StarSystemAPI.class);
        MemoryAPI cacheMemory = mock(MemoryAPI.class);
        MemoryAPI vaultMemory = mock(MemoryAPI.class);
        SectorEntityToken cache = entity("cache_1", "run1", cacheMemory);
        SectorEntityToken vault = entity("vault_1", "run1", vaultMemory);
        SectorEntityToken otherRun = entity("cache_2", "run2", mock(MemoryAPI.class));
        CampaignFleetAPI guard = mock(CampaignFleetAPI.class);
        when(pocket.getEntitiesWithTag(ChartIds.CONTENT_TAG)).thenReturn(List.of(cache, vault, otherRun, guard));
        when(pocket.getEntityById("vault_1")).thenReturn(vault);
        ChartContent content = new ChartContent(5, "pirates", ChartObjective.VAULT, List.of("vault_1"), null, "vault_1", 1, 2);
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_ITEM_DROP_CHANCE_PERCENT, 50f),
                new AppliedEffect(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 25f)));

        context.afterGeneration(new ChartGenerated(pocket, "run1", content, context));

        verify(cacheMemory, atLeastOnce()).set(ChartIds.ITEM_CHANCE_MEMORY_KEY, 1.5f);
        verify(vaultMemory, atLeastOnce()).set(eq(ChartIds.ITEM_CHANCE_MEMORY_KEY), eq(1.875f));
        verify(vault).addDropValue(ChartGenContext.VAULT_DROP_GROUP, 5000);
        verify(otherRun, never()).getMemoryWithoutUpdate();
        verifyNoInteractions(guard);
    }

    @Test
    void afterGenerationWithoutRewardEffectsTouchesNothing() {
        StarSystemAPI pocket = mock(StarSystemAPI.class);
        MemoryAPI cacheMemory = mock(MemoryAPI.class);
        SectorEntityToken cache = entity("cache_1", "run1", cacheMemory);
        when(pocket.getEntitiesWithTag(ChartIds.CONTENT_TAG)).thenReturn(List.of(cache));
        ChartGenContext context = with(ChartEffect.CHART_FLEET_POWER_PERCENT, 30f);

        context.afterGeneration(new ChartGenerated(pocket, "run1", ChartContent.none(3), context));

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
        JSONObject english = new JSONObject(Files.readString(Path.of("data/strings/exiledSector/en.json"), StandardCharsets.UTF_8));
        JSONObject chinese = new JSONObject(Files.readString(Path.of("data/strings/exiledSector/zh_CN.json"), StandardCharsets.UTF_8));
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
    void theWardenGetsTheWholeBossPercentageAfterTheCapAndOverflowsPastTheCeiling() {
        ChartFaction plainFaction = new ChartFaction("pirates", 1f, ChartTiers.LOWEST_TIER, ChartTiers.HIGHEST_TIER, 1f, 0f);
        boolean sawOverflow = false;
        boolean sawFullPercentage = false;
        for (int tier = 10; tier <= 15; tier++) {
            ChartTier base = ChartTiers.forTier(tier);
            float cappedBasePower = Math.min(ChartContentPlanner.BOSS_POWER_CAP, base.bossPower());
            ChartGenContext vigil = ChartGenContext.from(StarChartFixtures.chart("unique_chart_wardens_vigil", 5L, tier));
            ChartGenContext vigilAndWarded = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_BOSS_POWER_PERCENT, 80f),
                    new AppliedEffect(ChartEffect.CHART_BOSS_POWER_PERCENT, 60f)));
            assertEquals(1.8f, vigil.bossPowerMult(), TOLERANCE, "tier " + tier);
            for (ChartGenContext context : List.of(vigil, vigilAndWarded)) {
                ChartTier adjusted = context.adjust(base);
                float scaledPower = cappedBasePower * context.bossPowerMult();
                PlannedFleet boss = ChartContentPlanner.plannedBoss(adjusted, plainFaction, 0.5f, context.bossPowerMult(), 0);
                PlannedFleet atCeiling = ChartContentPlanner.plannedBoss(adjusted, plainFaction, 0.5f,
                        Math.min(context.bossPowerMult(), ChartContentPlanner.BOSS_POWER_CEILING / cappedBasePower), 0);
                assertEquals(Math.min(ChartContentPlanner.BOSS_POWER_CEILING, scaledPower), boss.combatPoints(), TOLERANCE, "tier " + tier);
                if (scaledPower > ChartContentPlanner.BOSS_POWER_CEILING) {
                    sawOverflow = true;
                    assertTrue(boss.quality() > atCeiling.quality(), "tier " + tier);
                    assertEquals(atCeiling.officerLevelBonus() + ChartContentPlanner.BOSS_OVERFLOW_BONUS, boss.officerLevelBonus(), "tier " + tier);
                    assertEquals(atCeiling.averageSMods() + ChartContentPlanner.BOSS_OVERFLOW_BONUS, boss.averageSMods(), "tier " + tier);
                } else {
                    sawFullPercentage |= scaledPower > ChartContentPlanner.BOSS_POWER_CAP;
                    assertEquals(atCeiling, boss, "tier " + tier);
                }
            }
        }
        assertTrue(sawOverflow);
        assertTrue(sawFullPercentage, "a warden above the old cap keeps its whole percentage");
    }

    @Test
    void aPlannedWardenFleetCarriesTheChartsBossPercentage() {
        ChartGenContext vigil = ChartGenContext.from(StarChartFixtures.chart("unique_chart_wardens_vigil", 5L, 12));
        ChartTier base = ChartTiers.forTier(12);
        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plan = plan(vigil, seed, 12);
            float cappedBasePower = Math.min(ChartContentPlanner.BOSS_POWER_CAP, base.bossPower() * plan.faction().powerMult());
            float expected = Math.min(ChartContentPlanner.BOSS_POWER_CEILING, cappedBasePower * 1.8f);
            assertEquals(expected, plan.fleetPower(FleetRole.BOSS), TOLERANCE, "seed " + seed);
            assertTrue(plan.fleetPower(FleetRole.BOSS) > ChartContentPlanner.BOSS_POWER_CAP, "seed " + seed);
        }
        ChartContentPlan plainPlan = plan(ChartGenContext.none(), 3L, 12);
        assertEquals(Math.min(ChartContentPlanner.BOSS_POWER_CAP, base.bossPower() * plainPlan.faction().powerMult()), plainPlan.fleetPower(FleetRole.BOSS),
                TOLERANCE);
    }

    @Test
    void extraFleetsNeverPushTheFleetCountPastTheLimit() {
        ChartTier top = ChartTiers.forTier(15);
        ChartTier crowded = with(ChartEffect.CHART_EXTRA_FLEETS, 4f).adjust(top);
        float averageFleets = (top.fleetsMin() + top.fleetsMax()) / 2f;

        assertTrue(top.fleetsMax() + 4 > ChartContentPlanner.MAX_FLEETS);
        assertEquals(ChartContentPlanner.MAX_FLEETS, crowded.fleetsMax());
        assertEquals(Math.min(ChartContentPlanner.MAX_FLEETS, top.fleetsMin() + 4), crowded.fleetsMin());
        assertEquals(top.fleetPower() * (1f + 4f / averageFleets), crowded.fleetPower(), TOLERANCE, "clamped fleets keep their power");
        ChartTier packed = with(ChartEffect.CHART_EXTRA_FLEETS, 30f).adjust(top);
        assertEquals(ChartContentPlanner.MAX_FLEETS, packed.fleetsMin());
        assertEquals(ChartContentPlanner.MAX_FLEETS, packed.fleetsMax());
        ChartTier oversizedData = new ChartTier(15, 900f, 14, 16, 0, 0, 0.5f, 0, 100, 100, 0f, false, 0f, 1f, 0f, 0f);
        for (long seed = 0; seed < 60; seed++) {
            ChartContentPlan crowdedPlan = plan(with(ChartEffect.CHART_EXTRA_FLEETS, 4f), seed, 15);
            assertTrue(crowdedPlan.count(FleetRole.GUARD) + crowdedPlan.count(FleetRole.PATROL) <= ChartContentPlanner.MAX_FLEETS, "seed " + seed);
            ChartContentPlan oversizedPlan = ChartContentPlanner.plan(seed, oversizedData, List.of(), List.of(), faction -> true);
            assertTrue(oversizedPlan.count(FleetRole.GUARD) + oversizedPlan.count(FleetRole.PATROL) <= ChartContentPlanner.MAX_FLEETS, "seed " + seed);
        }
    }

    @Test
    void vaultRichnessBecomesLootWhenThePocketHasNoVault() {
        ChartGenContext hoard = with(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 100f);
        assertEquals(BASE.lootBudget(), hoard.adjust(BASE).lootBudget(), TOLERANCE);
        assertEquals(BASE.lootBudget() * 2f, hoard.lootBudgetWithoutVault(BASE), TOLERANCE);
        assertEquals(BASE.lootBudget(), ChartGenContext.none().lootBudgetWithoutVault(BASE), TOLERANCE);
        boolean sawVault = false;
        boolean sawNoVault = false;
        for (long seed = 0; seed < 40; seed++) {
            ChartContentPlan plain = plan(ChartGenContext.none(), seed);
            ChartContentPlan hoarded = plan(hoard, seed);
            assertEquals(plain.objective(), hoarded.objective(), "seed " + seed);
            if (plain.objective() == ChartObjective.VAULT) {
                sawVault = true;
                assertEquals(plain, hoarded, "with a vault, the richness stays in the vault: seed " + seed);
            } else {
                sawNoVault = true;
                assertTrue(hoarded.lootSpent() > plain.lootSpent(), "seed " + seed + ": " + plain.lootSpent() + " -> " + hoarded.lootSpent());
            }
        }
        assertTrue(sawVault);
        assertTrue(sawNoVault);
        assertTrue(ChartEffect.CHART_VAULT_RICHNESS_PERCENT.description(25f).plain().contains("without a vault"));
    }

    @Test
    void chartQualityAddsOnePercentLootAndRewardPerPoint() {
        ChartGenContext polished = ChartGenContext.of(List.of(), 12);
        assertFalse(polished.isEmpty());
        assertEquals(1.12f, polished.lootMult(), TOLERANCE);
        assertEquals(1.12f, polished.rewardMult(), TOLERANCE);
        assertEquals(BASE.lootBudget() * 1.12f, polished.adjust(BASE).lootBudget(), TOLERANCE);
        assertEquals(BASE.fleetPower(), polished.adjust(BASE).fleetPower(), TOLERANCE);
        assertSame(ChartGenContext.none(), ChartGenContext.of(List.of(), 0));

        ItemData plainData = ItemData.rolledAtTier(StarChartFixtures.definition("star_chart_single"), 3L, 5);
        StarChart plainChart = (StarChart) plainData.preview();
        StarChart polishedChart = (StarChart) plainData.withTraits(new ItemTraits(5, 12, null, null)).preview();
        ChartGenContext plainContext = ChartGenContext.from(plainChart);
        ChartGenContext polishedContext = ChartGenContext.from(polishedChart);

        assertEquals(0, plainContext.quality());
        assertEquals(12, polishedContext.quality());
        assertEquals(plainContext.rewardMult() + 0.12f, polishedContext.rewardMult(), TOLERANCE);
        assertEquals(plainContext.lootMult() + 0.12f, polishedContext.lootMult(), TOLERANCE);
        assertTrue(ChartRewards.creditsFor(5, polishedContext.rewardMult()) >= ChartRewards.creditsFor(5, plainContext.rewardMult()));
        assertTrue(polishedChart.headerLines().stream().anyMatch(line -> line.plain().contains("12% more loot and completion reward")),
                polishedChart.headerLines().toString());
        assertTrue(plainChart.headerLines().stream().noneMatch(line -> line.plain().contains("Quality")));
    }
}
