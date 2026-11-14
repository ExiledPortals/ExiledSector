package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartBosses;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.turn.Intent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkirmishPlannerTest {

    private static final int TIER = 8;
    private static final int ATTACKING_OFFICER = 7;
    private static final int DEFENDING_OFFICER = 9;

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

    private static String lowestRank() {
        return OfficerRanks.all().get(0).id();
    }

    private static String highestRank() {
        return OfficerRanks.all().get(OfficerRanks.all().size() - 1).id();
    }

    private static ChartGenContext fought(SkirmishFight fight) {
        return fought(fight.encode());
    }

    private static ChartGenContext fought(String anomalyData) {
        return ChartGenContext.none().withAtlasNode(new AtlasRunNode(1L, "hegemony", List.of(), List.of(FactionSkirmish.ANOMALY_ID))
                .withAnomalyData(FactionSkirmish.ANOMALY_ID, anomalyData));
    }

    private static SkirmishFight fight(String attackerFactionId, String attackerRankId, String defenderRankId) {
        return new SkirmishFight(attackerFactionId, attackerRankId, defenderRankId, Intent.challenge(ATTACKING_OFFICER, DEFENDING_OFFICER));
    }

    private static ChartContentPlan plan(ChartGenContext context) {
        return ChartContentPlanner.plan(5L, ChartTiers.forTier(TIER), ChartPois.all(), ChartFactions.all(), id -> true, context,
                StarChartSettings.DEFAULT_FLEET_LIMIT, List.of(FactionSkirmish.ANOMALY_ID));
    }

    private static SkirmishPlan skirmishOf(ChartContentPlan plan) {
        return (SkirmishPlan) plan.anomalyPlan(FactionSkirmish.ANOMALY_ID);
    }

    @Test
    void theDefenderKeepsItsStationAndBothOfficersBringAFleetSizedByTheirOwnRank() {
        ChartContentPlan plan = plan(fought(fight("tritachyon", highestRank(), lowestRank())));
        SkirmishPlan skirmish = skirmishOf(plan);

        assertEquals(ChartBossType.STATION, plan.bossType());
        assertNotNull(plan.bossStation());
        assertTrue(plan.fleets().stream().noneMatch(fleet -> fleet.role() == FleetRole.BOSS));
        assertEquals("hegemony", plan.faction().factionId());
        assertEquals("tritachyon", skirmish.attackerFactionId());
        assertEquals(ATTACKING_OFFICER, skirmish.attackerOfficerId());
        assertEquals(DEFENDING_OFFICER, skirmish.defenderOfficerId());
        assertEquals(SkirmishPlanner.officerFleet(ChartTiers.forTier(TIER), highestRank()), skirmish.attackerFleet());
        assertEquals(SkirmishPlanner.officerFleet(ChartTiers.forTier(TIER), lowestRank()), skirmish.defenderFleet());
        assertTrue(skirmish.attackerFleet().strength().combatPoints() >= skirmish.defenderFleet().strength().combatPoints());
        assertFalse(skirmish.attackerFleet().guardsPoi());
        assertFalse(skirmish.defenderFleet().guardsPoi());
    }

    @Test
    void aSameFactionChallengeIsFoughtLikeAnyOther() {
        SkirmishPlan skirmish = skirmishOf(plan(fought(fight("hegemony", lowestRank(), lowestRank()))));

        assertNotNull(skirmish);
        assertEquals("hegemony", skirmish.attackerFactionId());
        assertEquals(skirmish.attackerFleet(), skirmish.defenderFleet());
    }

    @Test
    void aSkirmishWithoutBothOfficersOrFromAnOldSavePlansNoFight() {
        assertNull(skirmishOf(plan(fought(new SkirmishFight("tritachyon", lowestRank(), lowestRank(), null)))));
        assertNull(skirmishOf(plan(fought(new SkirmishFight("tritachyon", lowestRank(), lowestRank(),
                Intent.challenge(ATTACKING_OFFICER, Intent.NO_OFFICER))))));
        assertNull(skirmishOf(plan(fought(fight(null, lowestRank(), lowestRank())))));
        assertNull(skirmishOf(plan(fought(fight("tritachyon", null, lowestRank())))));
        assertNull(skirmishOf(plan(fought(StateFields.join("attacker", "tritachyon", Intent.push(ATTACKING_OFFICER, null, DEFENDING_OFFICER).encode())))));
        assertNull(skirmishOf(plan(ChartGenContext.none())));
    }
}
