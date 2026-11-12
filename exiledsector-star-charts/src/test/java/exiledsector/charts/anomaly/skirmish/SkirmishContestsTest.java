package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import exiledsector.charts.atlas.TestBorders;
import exiledsector.charts.atlas.TestBorders.Border;
import exiledsector.charts.atlas.TestTerritories;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static exiledsector.charts.atlas.TestBorders.NEUTRAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkirmishContestsTest {

    private AtlasState state;
    private AtlasGenerator generator;

    private static void declare(AtlasState state, Intent... intents) {
        state.setDeclaredIntents(List.of(intents).stream().map(Intent::encode).toList());
    }

    private static boolean skirmishIn(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasAnomalies.effective(generator, state, cell).contains(FactionSkirmish.ANOMALY_ID);
    }

    @BeforeEach
    void setUp() {
        SkirmishFactions.useForTests(Set.of(NEUTRAL));
        ChartTables.useAnomalies(List.of(FactionSkirmish.INSTANCE),
                List.of(new ChartAnomalies.Definition(FactionSkirmish.ANOMALY_ID, 1f, 1, 15, "", "", true, Set.of(), false)));
        state = new AtlasState(TestBorders.SEED);
        generator = TestBorders.generator(state);
    }

    @AfterEach
    void tearDown() {
        SkirmishFactions.useForTests(Set.of());
        ChartTables.resetAnomalies();
    }

    @Test
    void aDeclaredPushPlacesASkirmishLedByThePushingOfficer() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        Intent push = Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId());
        declare(state, push);

        SkirmishRecords.sync(generator, state, null);

        assertTrue(skirmishIn(generator, state, border.contested()));
        SkirmishContest contest = SkirmishContests.at(generator, state, border.contested());
        assertEquals(new SkirmishContest(push, border.attacker(), border.defender()), contest);
        assertEquals("tritachyon", FactionSkirmish.attacker(generator, state, border.contested()));
        assertEquals("hegemony", FactionSkirmish.defender(generator, state, border.contested()));
    }

    @Test
    void aDeclaredChallengePlacesASkirmishOnTheDefendersClaimBesideTheChallenger() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        Intent challenge = Intent.challenge(border.attacker().recordId(), border.defender().recordId());
        declare(state, challenge);

        SkirmishRecords.sync(generator, state, null);

        assertEquals(Set.of(border.contested()), SkirmishContests.declaredCells(generator, state));
        assertTrue(skirmishIn(generator, state, border.contested()));
        assertEquals(challenge, SkirmishContests.at(generator, state, border.contested()).intent());
    }

    @Test
    void theSkirmishLeavesWhenTheIntentIsGoneButStaysWhileItsRunIsOpen() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        declare(state, Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId()));
        SkirmishRecords.sync(generator, state, null);

        declare(state);
        SkirmishRecords.sync(generator, state, border.contested());
        assertEquals(1, state.anomalyRecords(border.contested()).size());

        SkirmishRecords.sync(generator, state, null);
        assertTrue(state.anomalyRecords(border.contested()).isEmpty());
        assertFalse(skirmishIn(generator, state, border.contested()));
    }

    @Test
    void noSkirmishIsLedByAnythingButAnActiveOfficerOfAnotherFactionThatTakesSides() {
        Border border = TestBorders.border(generator, state, "hegemony", "hegemony");
        int missingOfficer = border.attacker().recordId() + 50;
        declare(state, Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId()),
                Intent.push(missingOfficer, border.contested(), border.defender().recordId()),
                Intent.challenge(border.attacker().recordId(), border.defender().recordId()));
        SkirmishRecords.sync(generator, state, null);
        assertTrue(SkirmishContests.declaredCells(generator, state).isEmpty());
        assertFalse(skirmishIn(generator, state, border.contested()));

        AtlasOfficer neutral = TestTerritories.establish(state, border.attacker().commandCentre(), NEUTRAL);
        declare(state, Intent.push(neutral.recordId(), border.contested(), border.defender().recordId()));
        SkirmishRecords.sync(generator, state, null);
        assertFalse(skirmishIn(generator, state, border.contested()));

        for (AtlasCell cell : AtlasCell.ORIGIN.around(20)) {
            SkirmishContest contest = SkirmishContests.at(generator, state, cell);
            assertNull(contest, cell.toString());
        }
    }

    @Test
    void aPushOnAnotherDefenderOrACellOutsideTheBorderPlacesNothing() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        declare(state, Intent.push(border.attacker().recordId(), border.contested(), border.attacker().recordId()),
                Intent.push(border.attacker().recordId(), border.defender().commandCentre(), border.defender().recordId()));

        SkirmishRecords.sync(generator, state, null);

        assertTrue(state.anomalyRecords().isEmpty());
    }

    @Test
    void assaultOrdersPlaceASkirmishLedByANeighbouringOfficer() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        AtlasCatalysts.Catalyst assaultOrders = new AtlasCatalysts.Catalyst("test_assault_orders", FactionSkirmish.ANOMALY_ID, AtlasCatalysts.Family.ORDERS,
                1f, 1);
        AtlasCatalystUse use = AtlasCatalystUse.of(assaultOrders, generator, state, null, Set.of(border.contested()));
        assertTrue(use.check(border.contested()).allowed());

        AtlasAnomalies.insert(generator, state, border.contested(), FactionSkirmish.ANOMALY_ID, AtlasAnomalyRecord.Source.PLAYER, null);

        SkirmishContest contest = SkirmishContests.at(generator, state, border.contested());
        assertNotNull(contest);
        assertEquals(border.attacker(), contest.attacker());
        assertEquals(IntentKind.PUSH, contest.intent().kind());
        assertEquals(border.contested(), contest.intent().target());
        assertEquals(border.defender().recordId(), contest.intent().targetRecordId());
        assertTrue(skirmishIn(generator, state, border.contested()));
    }

    @Test
    void assaultOrdersSayWhyASystemCannotBeAttacked() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");

        assertEquals(FactionSkirmish.COMMAND_CENTRE_KEY,
                FactionSkirmish.INSTANCE.insertVerdict(generator, state, border.defender().commandCentre()).refusalKey());

        SkirmishFactions.useForTests(Set.of(NEUTRAL, "tritachyon"));
        assertEquals(FactionSkirmish.NO_ATTACKER_KEY, FactionSkirmish.INSTANCE.insertVerdict(generator, state, border.contested()).refusalKey());

        SkirmishFactions.useForTests(Set.of(NEUTRAL, "hegemony"));
        assertEquals(FactionSkirmish.NO_SIDE_KEY, FactionSkirmish.INSTANCE.insertVerdict(generator, state, border.contested()).refusalKey());

        SkirmishFactions.useForTests(Set.of());
        AtlasTerritoryTransfer.vacate(generator, state, border.contested());
        assertEquals(FactionSkirmish.UNCLAIMED_KEY, FactionSkirmish.INSTANCE.insertVerdict(generator, state, border.contested()).refusalKey());
        assertEquals(AtlasAnomalies.InsertCheck.DOES_NOT_FIT,
                AtlasAnomalies.canInsert(generator, state, border.contested(), FactionSkirmish.ANOMALY_ID, null));
    }
}
