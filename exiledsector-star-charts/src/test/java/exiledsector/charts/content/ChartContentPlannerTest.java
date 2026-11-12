package exiledsector.charts.content;

import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlan.PlannedPoi;
import exiledsector.Seeds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartContentPlannerTest {

    private static final Predicate<String> EVERY_FACTION = factionId -> true;
    private static final String BOSS_SIZE_ANOMALY = "boss_size_test";

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadShipped();
    }

    @AfterEach
    void tearDown() {
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    private static ChartContentPlan plan(long seed, int tier) {
        return ChartContentPlanner.plan(seed, ChartTiers.forTier(tier), ChartPois.all(), ChartFactions.all(), EVERY_FACTION);
    }

    @Test
    void aStationBossPlansItsStationAndAGuardFromItsShareOfTheBossSize() {
        ChartBosses.useForTests(List.of(new ChartBosses.Definition(ChartBossType.STATION, 1f, 1, 15, 0.5f)),
                List.of(new ChartBosses.Station("station2_Standard", Set.of(), 1, 15, 1f)));
        try {
            ChartTier tier = ChartTiers.forTier(10);
            ChartContentPlan plan = plan(77L, 10);

            assertEquals(ChartBossType.STATION, plan.bossType());
            assertEquals("station2_Standard", plan.bossStation().variantId());
            assertEquals(ChartContentPlanner.STATION_OFFICER_LEVEL + tier.officerLevelBonus() + ChartContentPlanner.BOSS_OFFICER_LEVEL_BONUS,
                    plan.bossStation().officerLevel());
            assertEquals(0, plan.count(FleetRole.BOSS));
            assertEquals(1, plan.count(FleetRole.STATION_GUARD));
            assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, tier.bossSize() * 0.5f)),
                    plan.fleetPoints(FleetRole.STATION_GUARD), 0.001f);
            PlannedFleet fallback = plan.bossStation().fallbackBoss();
            assertEquals(FleetRole.BOSS, fallback.role());
            assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, tier.bossSize())), fallback.combatPoints(), 0.001f);
        } finally {
            ChartBosses.resetForTests();
        }
    }

    @Test
    void aBarredStationBossIsAlwaysAFleet() {
        ChartBosses.useForTests(List.of(new ChartBosses.Definition(ChartBossType.STATION, 1f, 1, 15, 0.5f)),
                List.of(new ChartBosses.Station("station2_Standard", Set.of(), 1, 15, 1f)));
        try {
            ChartGenContext barred = ChartGenContext.none().withStationBossBarred(true);
            for (long seed = 0; seed < 10; seed++) {
                ChartContentPlan plan = ChartContentPlanner.plan(seed, ChartTiers.forTier(10), ChartPois.all(), ChartFactions.all(), EVERY_FACTION, barred);
                assertEquals(ChartBossType.FLEET, plan.bossType());
                assertEquals(1, plan.count(FleetRole.BOSS));
                assertEquals(0, plan.count(FleetRole.STATION_GUARD));
            }
        } finally {
            ChartBosses.resetForTests();
        }
    }

    @Test
    void aBiomeWithItsOwnSitesPlacesOnlyThemAndOtherBiomesNeverDo() {
        ChartPoi ordinary = site("ordinary", ChartPoi.Kind.CACHE, 1f);
        ChartPoi own = new ChartPoi("own", ChartPoi.Kind.STATION, "supply_cache", "", "", 2f, 1f, 1, 15, false, "", List.of(), List.of("rift"));
        List<ChartPoi> pois = List.of(ordinary, own);
        for (long seed = 0; seed < 10; seed++) {
            ChartContentPlan rift = ChartContentPlanner.plan(seed, ChartTiers.forTier(8), pois, ChartFactions.all(), EVERY_FACTION,
                    ChartGenContext.none().withBiome("rift"));
            ChartContentPlan normal = ChartContentPlanner.plan(seed, ChartTiers.forTier(8), pois, ChartFactions.all(), EVERY_FACTION,
                    ChartGenContext.none().withBiome("normal"));

            assertFalse(rift.pois().isEmpty());
            assertTrue(rift.pois().stream().allMatch(planned -> planned.poi() == own));
            assertTrue(normal.pois().stream().noneMatch(planned -> planned.poi() == own));
        }
    }

    @Test
    void aBiomesOwnSitesSpendTheBudgetsOfEveryKind() {
        ChartPoi own = new ChartPoi("own", ChartPoi.Kind.STATION, "supply_cache", "", "", 2f, 1f, 1, 15, false, "", List.of(), List.of("rift"));

        List<PlannedPoi> planned = ChartContentPlanner.spendPooledLoot(withSiteBudgets(5, 3f, 0f, 0f, 3f), List.of(own), new Random(4L));

        assertEquals(3, planned.size());
    }

    @Test
    void withoutBossTablesEveryBossIsAFleet() {
        for (long seed = 0; seed < 10; seed++) {
            ChartContentPlan plan = plan(seed, 6);
            assertEquals(ChartBossType.FLEET, plan.bossType());
            assertEquals(1, plan.count(FleetRole.BOSS));
            assertEquals(0, plan.count(FleetRole.STATION_GUARD));
        }
    }

    @Test
    void theSameSeedAndTierAlwaysPlanTheSameContent() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            assertEquals(plan(1234L, tier), plan(1234L, tier), "tier " + tier);
        }
    }

    @Test
    void differentSeedsPlanDifferentContent() {
        Set<ChartContentPlan> plans = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            plans.add(plan(seed, 8));
        }
        assertTrue(plans.size() > 15, "only " + plans.size() + " distinct plans");
    }

    private static float spentOn(List<PlannedPoi> pois, ChartPoi.Kind kind) {
        float spent = 0f;
        for (PlannedPoi planned : pois) {
            if (planned.poi().kind() == kind) {
                spent += planned.poi().cost();
            }
        }
        return spent;
    }

    private static float cheapestAt(ChartPoi.Kind kind, int tier) {
        float cheapest = Float.MAX_VALUE;
        for (ChartPoi poi : ChartPois.ofKind(ChartPois.lootAt(ChartPois.all(), tier), kind)) {
            cheapest = Math.min(cheapest, poi.cost());
        }
        return cheapest;
    }

    private static ChartTier withSiteBudgets(int tier, float cacheBudget, float domainBudget, float stationBudget, float derelictBudget) {
        return new ChartTier(tier, 100f, 2, 2, 0, 0, 50f, 100, 100, cacheBudget, domainBudget, stationBudget, derelictBudget, false, 0f);
    }

    private static ChartPoi site(String id, ChartPoi.Kind kind, float cost) {
        return new ChartPoi(id, kind, "supply_cache", "", kind == ChartPoi.Kind.DERELICT ? "SMALL" : "", cost, 1f, 1, 15, false, "");
    }

    @Test
    void eachCategoryOverspendsByLessThanOneCheapestSiteAndOnlyUsesPointsOfInterestAllowedAtTheTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier tierData = ChartTiers.forTier(tier);
            for (long seed = 0; seed < 30; seed++) {
                ChartContentPlan plan = plan(seed, tier);
                for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
                    float spent = spentOn(plan.pois(), kind);
                    assertTrue(spent == 0f || spent < tierData.siteBudget(kind) + cheapestAt(kind, tier),
                            "tier " + tier + " seed " + seed + " spent " + spent + " on " + kind);
                }
                assertTrue(plan.pois().size() <= ChartContentPlanner.MAX_POIS);
                for (PlannedPoi planned : plan.pois()) {
                    assertTrue(planned.poi().allowedAt(tier), planned.poi().id() + " at tier " + tier);
                }
            }
        }
    }

    @Test
    void eachSiteBudgetIsSpentDownToLessThanItsCheapestSite() {
        for (long seed = 0; seed < 30; seed++) {
            ChartContentPlan plan = plan(seed, 10);
            for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
                float left = ChartTiers.forTier(10).siteBudget(kind) - spentOn(plan.pois(), kind);
                assertTrue(left < cheapestAt(kind, 10), "left " + left + " of the " + kind + " budget unspent");
            }
        }
    }

    @Test
    void eachCategorySpendsOnlyItsOwnBudget() {
        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            ChartTier onlyThisKind = withSiteBudgets(15, kind == ChartPoi.Kind.CACHE ? 12f : 0f, kind == ChartPoi.Kind.DOMAIN ? 12f : 0f,
                    kind == ChartPoi.Kind.STATION ? 12f : 0f, kind == ChartPoi.Kind.DERELICT ? 12f : 0f);
            for (long seed = 0; seed < 30; seed++) {
                List<PlannedPoi> planned = ChartContentPlanner.spendLoot(onlyThisKind, ChartPois.all(), new Random(seed));
                assertFalse(planned.isEmpty(), kind + " seed " + seed);
                assertTrue(planned.stream().allMatch(poi -> poi.poi().kind() == kind), kind + " seed " + seed + ": " + planned);
            }
        }
        assertTrue(ChartContentPlanner.spendLoot(withSiteBudgets(15, 0f, 0f, 0f, 0f), ChartPois.all(), new Random(1)).isEmpty());
    }

    @Test
    void aFractionalLeftoverBuysOneMoreCheapestSiteWithTheMatchingChance() {
        List<ChartPoi> pois = List.of(site("probe", ChartPoi.Kind.DOMAIN, 3f), site("survey", ChartPoi.Kind.DOMAIN, 6f));
        int seeds = 4000;
        for (float budget : new float[]{1.5f, 7.5f}) {
            float spentTotal = 0f;
            for (long seed = 0; seed < seeds; seed++) {
                List<PlannedPoi> planned = ChartContentPlanner.spendLoot(withSiteBudgets(15, 0f, budget, 0f, 0f), pois,
                        Seeds.derived(seed, ChartContentPlanner.LOOT_SALT));
                float spent = spentOn(planned, ChartPoi.Kind.DOMAIN);
                assertTrue(budget - spent < 3f && spent < budget + 3f, "budget " + budget + " spent " + spent);
                spentTotal += spent;
            }
            assertEquals(budget, spentTotal / seeds, 0.1f, "budget " + budget + " buys its worth in sites on average");
        }
        List<PlannedPoi> leftoverOnly = ChartContentPlanner.spendLoot(withSiteBudgets(15, 0f, 2.9f, 0f, 0f), pois, new Random(5));
        assertTrue(leftoverOnly.stream().allMatch(planned -> planned.poi().id().equals("probe")), "the leftover only ever buys the cheapest site");
    }

    @Test
    void theSiteCapCutsCachesFirst() {
        List<ChartPoi> pois = List.of(site("cache", ChartPoi.Kind.CACHE, 1f), site("domain", ChartPoi.Kind.DOMAIN, 1f),
                site("station", ChartPoi.Kind.STATION, 1f), site("derelict", ChartPoi.Kind.DERELICT, 1f));

        List<PlannedPoi> planned = ChartContentPlanner.spendLoot(withSiteBudgets(15, 10f, 10f, 10f, 10f), pois, new Random(7));

        assertEquals(ChartContentPlanner.MAX_POIS, planned.size());
        assertEquals(10f, spentOn(planned, ChartPoi.Kind.DOMAIN));
        assertEquals(10f, spentOn(planned, ChartPoi.Kind.STATION));
        assertEquals(ChartContentPlanner.MAX_POIS - 20f, spentOn(planned, ChartPoi.Kind.DERELICT));
        assertEquals(0f, spentOn(planned, ChartPoi.Kind.CACHE));
    }

    @Test
    void picksLeanTowardsTheAverageCostOfWhatIsLeft() {
        ChartPoi cheap = new ChartPoi("cheap", ChartPoi.Kind.CACHE, "supply_cache_small", "", "", 1f, 10f, 1, 15, false, "");
        ChartPoi mid = new ChartPoi("mid", ChartPoi.Kind.CACHE, "supply_cache", "", "", 5f, 10f, 1, 15, false, "");

        assertEquals(10f / 3f, ChartContentPlanner.targetCost(List.of(cheap, mid), 10f), 0.001f);
        assertEquals(5f, ChartContentPlanner.targetCost(List.of(mid), 10f), 0.001f);
        assertEquals(1f, ChartContentPlanner.targetCost(List.of(cheap), 3f), 0.001f);
    }

    @Test
    void fleetsStayWithinTheTierRangeAndTheSizeCapsAndAverageTheTierFleetSize() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier tierData = ChartTiers.forTier(tier);
            for (long seed = 0; seed < 20; seed++) {
                ChartContentPlan plan = plan(seed, tier);
                long ordinary = plan.count(FleetRole.GUARD) + plan.count(FleetRole.PATROL);
                assertTrue(ordinary >= tierData.fleetsMin() && ordinary <= tierData.fleetsMax(), "tier " + tier + ": " + ordinary + " fleets");
                assertEquals(1, plan.count(FleetRole.BOSS), "tier " + tier + " has one boss");
                float fleetSize = tierData.fleetSize();
                float expectedPoints = ordinary * ChartTier.fleetPoints(fleetSize);
                float spent = plan.fleetPoints(FleetRole.GUARD) + plan.fleetPoints(FleetRole.PATROL);
                float minimumPoints = ChartTier.fleetPoints(ChartContentPlanner.MIN_FLEET_SIZE);
                if (fleetSize * (1f + ChartContentPlanner.SHARE_SPREAD) / (1f - ChartContentPlanner.SHARE_SPREAD) <= ChartContentPlanner.FLEET_SIZE_CAP) {
                    assertEquals(expectedPoints, spent, Math.max(1f, ordinary * minimumPoints), "tier " + tier);
                } else {
                    assertTrue(spent <= expectedPoints + 1f, "tier " + tier);
                }
                for (PlannedFleet fleet : plan.fleets()) {
                    float cap = ChartTier.fleetPoints(fleet.role() == FleetRole.BOSS ? ChartContentPlanner.BOSS_SIZE_CAP : ChartContentPlanner.FLEET_SIZE_CAP);
                    assertTrue(fleet.combatPoints() <= cap, fleet.toString());
                    assertTrue(fleet.combatPoints() >= minimumPoints, fleet.toString());
                    assertTrue(fleet.quality() <= ChartContentPlanner.MAX_QUALITY / ChartTier.PERCENT);
                    if (fleet.strengthBonus() > 0) {
                        assertEquals(cap, fleet.combatPoints(), 0.01f, fleet.toString());
                    }
                }
            }
        }
    }

    @Test
    void fleetsOverTheSizeCapAreCappedAndTurnTheRestIntoFleetStrength() {
        ChartTier huge = new ChartTier(15, 9600f, 2, 2, 1, 1, 50f, 100, 100, 0f, 0f, 0f, 0f, true, 8000f);

        ChartContentPlan plan = ChartContentPlanner.plan(5L, huge, List.of(), List.of(), EVERY_FACTION);

        assertEquals(2, plan.count(FleetRole.GUARD) + plan.count(FleetRole.PATROL));
        for (PlannedFleet fleet : plan.fleets()) {
            boolean boss = fleet.role() == FleetRole.BOSS;
            assertEquals(ChartTier.fleetPoints(boss ? ChartContentPlanner.BOSS_SIZE_CAP : ChartContentPlanner.FLEET_SIZE_CAP), fleet.combatPoints(), 0.01f);
            assertTrue(fleet.strengthBonus() > 0, fleet.toString());
            assertTrue(fleet.strengthBonus() <= ChartContentPlanner.MAX_OVERFLOW_STRENGTH, fleet.toString());
            assertEquals(boss ? 0.65f : 0.5f, fleet.quality(), 0.001f);
            assertEquals(boss ? 1 + ChartContentPlanner.BOSS_OFFICER_LEVEL_BONUS : 1, fleet.officerLevelBonus());
        }
        assertEquals(ChartContentPlanner.MAX_OVERFLOW_STRENGTH, ChartContentPlanner.overflowStrength(2f * ChartContentPlanner.BOSS_SIZE_CAP, ChartContentPlanner.BOSS_SIZE_CAP));
        assertEquals(25, ChartContentPlanner.overflowStrength(1.25f * ChartContentPlanner.FLEET_SIZE_CAP, ChartContentPlanner.FLEET_SIZE_CAP));
        assertEquals(0, ChartContentPlanner.overflowStrength(ChartContentPlanner.FLEET_SIZE_CAP, ChartContentPlanner.FLEET_SIZE_CAP));
    }

    @Test
    void fleetQualityPastOneHundredPercentAddsAnSModPerFiftyPercent() {
        assertEquals(0, ChartContentPlanner.averageSMods(100f));
        assertEquals(0, ChartContentPlanner.averageSMods(60f));
        assertEquals(1, ChartContentPlanner.averageSMods(150f));
        assertEquals(2, ChartContentPlanner.averageSMods(215f));
    }

    @Test
    void theChartsFactionInhabitsTheSystemWhenItIsLoaded() {
        ChartTier tier = new ChartTier(8, 200f, 3, 3, 0, 0, 60f, 100, 100, 0f, 0f, 0f, 0f, true, 400f);
        List<ChartFaction> factions = List.of(new ChartFaction("pirates", 10f), new ChartFaction("remnant", 1f));
        ChartGenContext remnantChart = ChartGenContext.of(List.of(), 0, "remnant");

        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plan = ChartContentPlanner.plan(seed, tier, List.of(), factions, EVERY_FACTION, remnantChart);
            assertEquals(factions.get(1), plan.faction());
            assertEquals("pirates", ChartContentPlanner.plan(seed, tier, List.of(), factions, id -> !id.equals("remnant"), remnantChart)
                    .faction().factionId());
        }
    }

    @Test
    void guardFleetsOnlyHoldGuardedLootAndNoLootMeansNoGuards() {
        ChartTier bossless = new ChartTier(2, 100f, 2, 3, 0, 0, 40f, 60, 90, 0f, 0f, 0f, 0f, false, 0f);

        ChartContentPlan noLoot = ChartContentPlanner.plan(9L, bossless, List.of(), List.of(), EVERY_FACTION);

        assertEquals(0, noLoot.count(FleetRole.GUARD));
        for (long seed = 0; seed < 40; seed++) {
            ChartContentPlan plan = plan(seed, 1);
            for (PlannedFleet fleet : plan.fleets()) {
                if (fleet.role() == FleetRole.GUARD) {
                    assertTrue(fleet.guardsPoi());
                    assertTrue(plan.pois().get(fleet.poiIndex()).guarded());
                }
            }
        }
    }

    @Test
    void theBossGuardsTheMostValuableSite() {
        int checked = 0;
        for (long seed = 0; seed < 50; seed++) {
            ChartContentPlan plan = plan(seed, 12);
            PlannedFleet boss = plan.fleets().stream().filter(fleet -> fleet.role() == FleetRole.BOSS).findFirst().orElseThrow();
            if (plan.pois().isEmpty()) {
                continue;
            }
            float highestCost = (float) plan.pois().stream().mapToDouble(planned -> planned.poi().cost()).max().orElseThrow();
            assertEquals(highestCost, plan.pois().get(boss.poiIndex()).poi().cost(), 0.001f, "seed " + seed);
            checked++;
        }
        assertTrue(checked > 0);
    }

    @Test
    void thePlannedAnomaliesEffectsSizeTheBoss() throws Exception {
        float bossSizePercent = 50f;
        ChartTables.useAnomalies(List.of(new ChartAnomalies.Definition(BOSS_SIZE_ANOMALY, 1f, 1, 16, "", "", true, Set.of(), false,
                List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_BOSS_SIZE_PERCENT, bossSizePercent)), Set.of(), 1f, 1f)));
        try {
            ChartTier tier = ChartTiers.forTier(10);
            ChartContentPlan ordinary = ChartContentPlanner.plan(5L, tier, ChartPois.all(), ChartFactions.all(), EVERY_FACTION, ChartGenContext.none(),
                    StarChartSettings.DEFAULT_FLEET_LIMIT, List.of());
            ChartContentPlan larger = ChartContentPlanner.plan(5L, tier, ChartPois.all(), ChartFactions.all(), EVERY_FACTION, ChartGenContext.none(),
                    StarChartSettings.DEFAULT_FLEET_LIMIT, List.of(BOSS_SIZE_ANOMALY));

            assertTrue(tier.boss());
            assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, tier.bossSize())), ordinary.fleetPoints(FleetRole.BOSS), 0.01f);
            assertEquals(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, tier.bossSize() + bossSizePercent)),
                    larger.fleetPoints(FleetRole.BOSS), 0.01f);
            assertEquals(List.of(BOSS_SIZE_ANOMALY), larger.anomalies());
        } finally {
            ChartTables.resetAnomalies();
        }
    }

    @Test
    void unloadedFactionsAreNeverPicked() {
        for (long seed = 0; seed < 50; seed++) {
            ChartContentPlan plan = ChartContentPlanner.plan(seed, ChartTiers.forTier(10), ChartPois.all(), ChartFactions.all(),
                    "hegemony"::equals);
            assertEquals("hegemony", plan.faction().factionId());
        }
        assertNotEquals("hegemony", ChartContentPlanner.plan(1L, ChartTiers.forTier(10), ChartPois.all(), ChartFactions.all(),
                factionId -> false).faction().factionId());
    }
}
