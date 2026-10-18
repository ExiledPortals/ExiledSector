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
    void fleetsStayWithinTheTierRangeAndThePerFleetCapAndSpendThePower() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier tierData = ChartTiers.forTier(tier);
            for (long seed = 0; seed < 20; seed++) {
                ChartContentPlan plan = plan(seed, tier);
                long ordinary = plan.count(FleetRole.GUARD) + plan.count(FleetRole.PATROL);
                assertTrue(ordinary >= tierData.fleetsMin() && ordinary <= tierData.fleetsMax(), "tier " + tier + ": " + ordinary + " fleets");
                assertEquals(tierData.boss() ? 1 : 0, plan.count(FleetRole.BOSS), "tier " + tier);
                float expectedPower = tierData.fleetPower() * plan.faction().powerMult();
                float spent = plan.fleetPower(FleetRole.GUARD) + plan.fleetPower(FleetRole.PATROL);
                assertEquals(expectedPower, spent, Math.max(1f, ordinary * ChartContentPlanner.MIN_FLEET_POWER), "tier " + tier);
                for (PlannedFleet fleet : plan.fleets()) {
                    float cap = fleet.role() == FleetRole.BOSS ? ChartContentPlanner.BOSS_POWER_CAP : ChartContentPlanner.FLEET_POWER_CAP;
                    assertTrue(fleet.combatPoints() <= cap, fleet.toString());
                    assertTrue(fleet.combatPoints() >= ChartContentPlanner.MIN_FLEET_POWER, fleet.toString());
                    assertTrue(fleet.quality() <= ChartContentPlanner.MAX_QUALITY);
                }
            }
        }
    }

    @Test
    void aHugeBudgetSplitsIntoMoreCappedFleetsAndPutsTheRestIntoQualityAndOfficers() {
        ChartTier huge = new ChartTier(15, 9000f, 2, 2, 1, 1, 0.5f, 0, 100, 100, 0f, false, 0f, 1f, 0f, 0f);

        ChartContentPlan plan = ChartContentPlanner.plan(5L, huge, List.of(), List.of(), EVERY_FACTION);

        assertEquals(ChartContentPlanner.MAX_FLEETS, plan.fleets().size());
        for (PlannedFleet fleet : plan.fleets()) {
            assertTrue(fleet.combatPoints() <= ChartContentPlanner.FLEET_POWER_CAP);
            assertTrue(fleet.quality() > 0.5f);
            assertEquals(2, fleet.officerLevelBonus());
        }
    }

    @Test
    void theClearObjectiveAlwaysHasAGuardEvenWithoutGuardedLoot() {
        ChartTier clearOnly = new ChartTier(2, 100f, 2, 3, 0, 0, 0.4f, 0, 60, 90, 0f, false, 0f, 1f, 0f, 0f);

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
        ChartTier vaultWithoutVaults = new ChartTier(6, 300f, 3, 3, 0, 0, 0.5f, 0, 100, 100, 20f, false, 0f, 0f, 0f, 5f);
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
