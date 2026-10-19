package exiledsector.charts;

import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import exiledsector.charts.ChartContentPlan.PlannedPoi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
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

    @BeforeEach
    void setUp() throws Exception {
        ChartContentDataTest.loadShippedTables();
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

    @Test
    void lootNeverOverspendsAndOnlyUsesPointsOfInterestAllowedAtTheTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier tierData = ChartTiers.forTier(tier);
            for (long seed = 0; seed < 30; seed++) {
                ChartContentPlan plan = plan(seed, tier);
                assertTrue(plan.lootSpent() <= tierData.lootBudget() + 0.001f, "tier " + tier + " seed " + seed + " spent " + plan.lootSpent());
                assertTrue(plan.pois().size() <= ChartContentPlanner.MAX_POIS);
                for (PlannedPoi planned : plan.pois()) {
                    assertTrue(planned.poi().allowedAt(tier), planned.poi().id() + " at tier " + tier);
                }
                boolean hasVault = plan.pois().stream().anyMatch(planned -> planned.poi().isVault());
                assertEquals(plan.objective() == ChartObjective.VAULT, hasVault, "tier " + tier + " seed " + seed);
            }
        }
    }

    @Test
    void theLootBudgetIsMostlySpent() {
        for (long seed = 0; seed < 30; seed++) {
            ChartContentPlan plan = plan(seed, 10);
            float budget = ChartTiers.forTier(10).lootBudget();
            assertTrue(budget - plan.lootSpent() < 2f, "left " + (budget - plan.lootSpent()) + " unspent");
        }
    }

    @Test
    void picksLeanTowardsTheAverageCostOfWhatIsLeft() {
        ChartPoi cheap = new ChartPoi("cheap", ChartPoi.Kind.CACHE, "supply_cache_small", "", "", 1f, 10f, 1, 15, false);
        ChartPoi mid = new ChartPoi("mid", ChartPoi.Kind.CACHE, "supply_cache", "", "", 5f, 10f, 1, 15, false);

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
                assertEquals(tierData.boss() ? 1 : 0, plan.count(FleetRole.BOSS), "tier " + tier);
                float fleetSize = tierData.fleetSize() + plan.faction().fleetSizeBonus();
                float expectedPoints = ordinary * ChartTier.fleetPoints(fleetSize);
                float spent = plan.fleetPoints(FleetRole.GUARD) + plan.fleetPoints(FleetRole.PATROL);
                float minimumPoints = ChartTier.fleetPoints(ChartContentPlanner.MIN_FLEET_SIZE);
                if (fleetSize * (1f + ChartContentPlanner.SHARE_SPREAD) / (1f - ChartContentPlanner.SHARE_SPREAD) <= ChartContentPlanner.FLEET_SIZE_CAP) {
                    assertEquals(expectedPoints, spent, Math.max(1f, ordinary * minimumPoints), "tier " + tier);
                } else {
                    assertTrue(spent <= expectedPoints + 1f, "tier " + tier);
                }
                for (PlannedFleet fleet : plan.fleets()) {
                    float cap = ChartTier.fleetPoints(fleet.role() == FleetRole.BOSS ? ChartContentPlanner.WARDEN_SIZE_CAP : ChartContentPlanner.FLEET_SIZE_CAP);
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
        ChartTier huge = new ChartTier(15, 1200f, 2, 2, 1, 1, 50f, 100, 100, 0f, true, 2000f, 1f, 0f, 0f);

        ChartContentPlan plan = ChartContentPlanner.plan(5L, huge, List.of(), List.of(), EVERY_FACTION);

        assertEquals(2, plan.count(FleetRole.GUARD) + plan.count(FleetRole.PATROL));
        for (PlannedFleet fleet : plan.fleets()) {
            boolean warden = fleet.role() == FleetRole.BOSS;
            assertEquals(ChartTier.fleetPoints(warden ? ChartContentPlanner.WARDEN_SIZE_CAP : ChartContentPlanner.FLEET_SIZE_CAP), fleet.combatPoints(), 0.01f);
            assertTrue(fleet.strengthBonus() > 0, fleet.toString());
            assertTrue(fleet.strengthBonus() <= ChartContentPlanner.MAX_OVERFLOW_STRENGTH, fleet.toString());
            assertEquals(warden ? 0.65f : 0.5f, fleet.quality(), 0.001f);
            assertEquals(warden ? 1 + ChartContentPlanner.BOSS_OFFICER_LEVEL_BONUS : 1, fleet.officerLevelBonus());
        }
        assertEquals(ChartContentPlanner.MAX_OVERFLOW_STRENGTH, ChartContentPlanner.overflowStrength(2000f, ChartContentPlanner.WARDEN_SIZE_CAP));
        assertEquals(25, ChartContentPlanner.overflowStrength(750f, ChartContentPlanner.FLEET_SIZE_CAP));
        assertEquals(0, ChartContentPlanner.overflowStrength(600f, ChartContentPlanner.FLEET_SIZE_CAP));
    }

    @Test
    void theChartsFactionInhabitsTheSystemWhenItIsLoaded() {
        ChartTier tier = new ChartTier(8, 200f, 3, 3, 0, 0, 60f, 100, 100, 0f, true, 400f, 1f, 0f, 0f);
        List<ChartFaction> factions = List.of(new ChartFaction("pirates", 10f, 0f, 0f), new ChartFaction("remnant", 1f, -10f, 15f));
        ChartGenContext remnantChart = ChartGenContext.of(List.of(), 0, "remnant");

        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plan = ChartContentPlanner.plan(seed, tier, List.of(), factions, EVERY_FACTION, remnantChart);
            assertEquals(factions.get(1), plan.faction());
            assertEquals("pirates", ChartContentPlanner.plan(seed, tier, List.of(), factions, id -> !id.equals("remnant"), remnantChart)
                    .faction().factionId());
        }
    }

    @Test
    void aFactionsFleetSizeBonusAddsToEveryFleetAndTheWarden() {
        ChartTier tier = new ChartTier(8, 200f, 3, 3, 0, 0, 60f, 100, 100, 0f, true, 400f, 1f, 0f, 0f);
        ChartFaction plain = new ChartFaction("hegemony", 1f, 0f, 0f);
        ChartFaction bigger = new ChartFaction("derelict", 1f, 20f, -10f);

        ChartContentPlan plainPlan = ChartContentPlanner.plan(3L, tier, List.of(), List.of(plain), EVERY_FACTION);
        ChartContentPlan biggerPlan = ChartContentPlanner.plan(3L, tier, List.of(), List.of(bigger), EVERY_FACTION);

        assertEquals(3 * ChartTier.fleetPoints(200f), plainPlan.fleetPoints(FleetRole.GUARD) + plainPlan.fleetPoints(FleetRole.PATROL), 0.5f);
        assertEquals(3 * ChartTier.fleetPoints(220f), biggerPlan.fleetPoints(FleetRole.GUARD) + biggerPlan.fleetPoints(FleetRole.PATROL), 0.5f);
        assertEquals(ChartTier.fleetPoints(400f), plainPlan.fleetPoints(FleetRole.BOSS), 0.01f);
        assertEquals(ChartTier.fleetPoints(420f), biggerPlan.fleetPoints(FleetRole.BOSS), 0.01f);
        assertEquals(0.5f, biggerPlan.fleets().get(0).quality(), 0.001f);
    }

    @Test
    void theClearObjectiveAlwaysHasAGuardEvenWithoutGuardedLoot() {
        ChartTier clearOnly = new ChartTier(2, 100f, 2, 3, 0, 0, 40f, 60, 90, 0f, false, 0f, 1f, 0f, 0f);

        ChartContentPlan noLoot = ChartContentPlanner.plan(9L, clearOnly, List.of(), List.of(), EVERY_FACTION);

        assertEquals(ChartObjective.CLEAR, noLoot.objective());
        assertEquals(1, noLoot.count(FleetRole.GUARD));
        assertFalse(noLoot.fleets().get(0).guardsPoi());
        for (long seed = 0; seed < 20; seed++) {
            ChartContentPlan plan = plan(seed, 1);
            assertEquals(ChartObjective.CLEAR, plan.objective());
            assertTrue(plan.count(FleetRole.GUARD) >= 1);
            for (PlannedFleet fleet : plan.fleets()) {
                if (fleet.guardsPoi()) {
                    assertTrue(plan.pois().get(fleet.poiIndex()).guarded());
                }
            }
        }
    }

    @Test
    void objectivesFollowTheTierWeightsAndWhatTheTierCanHave() {
        Set<ChartObjective> seenAtFifteen = EnumSet.noneOf(ChartObjective.class);
        for (long seed = 0; seed < 200; seed++) {
            seenAtFifteen.add(plan(seed, 15).objective());
            ChartObjective atTwo = plan(seed, 2).objective();
            assertEquals(ChartObjective.CLEAR, atTwo);
        }
        assertEquals(EnumSet.allOf(ChartObjective.class), seenAtFifteen);
        ChartTier vaultWithoutVaults = new ChartTier(6, 200f, 3, 3, 0, 0, 50f, 100, 100, 20f, false, 0f, 0f, 0f, 5f);
        assertEquals(ChartObjective.CLEAR, ChartContentPlanner.pickObjective(vaultWithoutVaults, false, new Random(3)));
        assertEquals(ChartObjective.VAULT, ChartContentPlanner.pickObjective(vaultWithoutVaults, true, new Random(3)));
    }

    @Test
    void theBossGuardsTheVaultWhenThereIsOne() {
        boolean checked = false;
        for (long seed = 0; seed < 200 && !checked; seed++) {
            ChartContentPlan plan = plan(seed, 12);
            if (plan.objective() == ChartObjective.VAULT) {
                PlannedFleet boss = plan.fleets().stream().filter(fleet -> fleet.role() == FleetRole.BOSS).findFirst().orElseThrow();
                assertTrue(plan.pois().get(boss.poiIndex()).poi().isVault());
                checked = true;
            }
        }
        assertTrue(checked);
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
