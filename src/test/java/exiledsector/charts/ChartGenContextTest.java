package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartGenContext.AppliedEffect;
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
        assertEquals(1.8f, first.adjust(BASE).bossPower() / BASE.bossPower(), TOLERANCE);
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
        assertEquals(BASE.bossPower() * 1.4f, with(ChartEffect.CHART_BOSS_POWER_PERCENT, 40f).adjust(BASE).bossPower(), TOLERANCE);
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
        return ChartContentPlanner.plan(seed, context.adjust(ChartTiers.forTier(8)), ChartPois.all(), ChartFactions.all(), faction -> true,
                context.extraPois());
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
}
