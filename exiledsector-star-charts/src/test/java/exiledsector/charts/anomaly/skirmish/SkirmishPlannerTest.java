package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartBosses;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFaction;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartPois;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkirmishPlannerTest {

    private static final int TIER = 8;

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadShipped();
        ChartBosses.useForTests(List.of(new ChartBosses.Definition(ChartBossType.FLEET, 1f, 1, 15, 1f)),
                List.of(new ChartBosses.Station("station2_midline_Standard", Set.of(), 1, 15, 1f)));
    }

    @AfterEach
    void tearDown() {
        ChartBosses.resetForTests();
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    private static ChartGenContext joined(SkirmishSide side) {
        return ChartGenContext.none().withAtlasNode(new AtlasRunNode(1L, "hegemony", List.of(), List.of(FactionSkirmish.ANOMALY_ID), false)
                .withAnomalyData(FactionSkirmish.ANOMALY_ID, new SkirmishJoin(side, "tritachyon").encode()));
    }

    private static ChartContentPlan plan(ChartGenContext context) {
        return ChartContentPlanner.plan(5L, ChartTiers.forTier(TIER), ChartPois.all(), ChartFactions.all(), id -> true, context,
                StarChartSettings.DEFAULT_FLEET_LIMIT, List.of(FactionSkirmish.ANOMALY_ID));
    }

    private static SkirmishPlan skirmishOf(ChartContentPlan plan) {
        return (SkirmishPlan) plan.anomalyPlan(FactionSkirmish.ANOMALY_ID);
    }

    private static float points(List<PlannedFleet> fleets) {
        return (float) fleets.stream().mapToDouble(PlannedFleet::combatPoints).sum();
    }

    @Test
    void theDefenderAlwaysHasAStationAndTheAttackerMatchesItsFleetsWithACommander() {
        ChartContentPlan plan = plan(joined(SkirmishSide.NEITHER));
        ChartContentPlan plain = ChartContentPlanner.plan(5L, ChartTiers.forTier(TIER), ChartPois.all(), ChartFactions.all(), id -> true,
                joined(SkirmishSide.NEITHER), StarChartSettings.DEFAULT_FLEET_LIMIT, List.of());

        assertEquals(ChartBossType.STATION, plan.bossType());
        assertNotNull(plan.bossStation());
        assertEquals(ChartBossType.FLEET, plain.bossType());
        assertNull(skirmishOf(plain));
        assertEquals("hegemony", plan.faction().factionId());
        assertEquals("tritachyon", skirmishOf(plan).attackerFactionId());
        assertEquals(SkirmishSide.NEITHER, skirmishOf(plan).side());
        List<PlannedFleet> attackers = skirmishOf(plan).attackerFleets();
        long ordinaryDefenders = plan.fleets().stream().filter(fleet -> !fleet.role().bossFleet()).count();
        assertEquals(ordinaryDefenders, attackers.stream().filter(fleet -> fleet.role() == FleetRole.PATROL).count());
        assertEquals(1, attackers.stream().filter(fleet -> fleet.role() == FleetRole.BOSS).count());
        assertTrue(attackers.stream().noneMatch(PlannedFleet::guardsPoi));
        assertEquals(plan.fleetPoints(FleetRole.GUARD) + plan.fleetPoints(FleetRole.PATROL),
                points(attackers.stream().filter(fleet -> fleet.role() == FleetRole.PATROL).toList()), 0.01f);
    }

    @Test
    void theAlliedSideIsWeakerByTheDivisorAndTheEnemyKeepsTheNormalBudget() {
        ChartContentPlan neither = plan(joined(SkirmishSide.NEITHER));
        ChartContentPlan withDefender = plan(joined(SkirmishSide.DEFENDER));
        ChartContentPlan withAttacker = plan(joined(SkirmishSide.ATTACKER));

        assertEquals(points(neither.fleets()) / SkirmishPlanner.ALLY_STRENGTH_DIVISOR, points(withDefender.fleets()), 0.01f);
        assertEquals(points(skirmishOf(neither).attackerFleets()), points(skirmishOf(withDefender).attackerFleets()), 0.01f);
        assertEquals(points(neither.fleets()), points(withAttacker.fleets()), 0.01f);
        assertEquals(points(skirmishOf(neither).attackerFleets()) / SkirmishPlanner.ALLY_STRENGTH_DIVISOR,
                points(skirmishOf(withAttacker).attackerFleets()), 0.01f);
        assertEquals(neither.bossStation().variantId(), withDefender.bossStation().variantId());
        assertEquals(neither.bossStation().fallbackBoss().combatPoints() / SkirmishPlanner.ALLY_STRENGTH_DIVISOR,
                withDefender.bossStation().fallbackBoss().combatPoints(), 0.01f);
        assertEquals(neither.bossStation(), withAttacker.bossStation());
    }

    @Test
    void aMissingOrInvalidAttackerIsPickedFromFactionsThatTakeSides() {
        List<ChartFaction> factions = List.of(new ChartFaction("hegemony", 5f), new ChartFaction("pirates", 5f), new ChartFaction("remnant", 0f));

        assertEquals("pirates", SkirmishPlanner.attacker("pirates", "hegemony", factions, id -> true, new Random(1)));
        assertEquals("pirates", SkirmishPlanner.attacker(null, "hegemony", factions, id -> true, new Random(1)));
        assertEquals("pirates", SkirmishPlanner.attacker("hegemony", "hegemony", factions, id -> true, new Random(1)));
        assertEquals("pirates", SkirmishPlanner.attacker("unloaded", "hegemony", factions, id -> !id.equals("unloaded"), new Random(1)));
        assertNull(SkirmishPlanner.attacker(null, "hegemony", factions, id -> !id.equals("pirates"), new Random(1)));

        ChartGenContext consoleRun = ChartGenContext.none().withAnomalyData(FactionSkirmish.ANOMALY_ID, new SkirmishJoin(SkirmishSide.ATTACKER, null).encode());
        ChartContentPlan picked = plan(consoleRun);
        assertNotEquals(picked.faction().factionId(), skirmishOf(picked).attackerFactionId());
        assertEquals(SkirmishSide.ATTACKER, skirmishOf(picked).side());
    }
}
