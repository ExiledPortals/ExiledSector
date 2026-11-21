package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.TestBorders;
import exiledsector.charts.atlas.TestBorders.Border;
import exiledsector.charts.atlas.TestTerritories;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.turn.Intent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        ChartTables.useAnomalies(List.of(FactionSkirmish.INSTANCE),
                List.of(new ChartAnomalies.Definition(FactionSkirmish.ANOMALY_ID, 1f, 1, 15, "", "", true, Set.of(), false)));
        state = new AtlasState(TestBorders.SEED);
        generator = TestBorders.generator(state);
    }

    @AfterEach
    void tearDown() {
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
        assertNull(SkirmishContests.battleCell(generator, state, push));
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
    void aChallengeAcrossTheAtlasIsFoughtInTheDefendersFreeSystemNearestTheChallenger() {
        List<AtlasCell> open = AtlasCell.ORIGIN.around(20).stream()
                .filter(cell -> TestBorders.open(generator, state, cell) && generator.node(cell).specialId() == null).toList();
        AtlasCell challengerCentre = open.get(0);
        AtlasCell nearClaim = open.stream().filter(cell -> squaredDistance(challengerCentre, cell) > 9 && squaredDistance(challengerCentre, cell) <= 25
                && !generator.lanes(challengerCentre).contains(cell)).findFirst().orElseThrow();
        List<AtlasCell> farCells = open.stream().filter(cell -> squaredDistance(challengerCentre, cell) > 100).toList();
        AtlasOfficer defender = TestTerritories.establish(state, farCells.get(0), "hegemony");
        state.claim(farCells.get(1), defender.commandCentre());
        state.claim(nearClaim, defender.commandCentre());
        AtlasOfficer challenger = TestTerritories.establish(state, challengerCentre, "tritachyon");
        Intent challenge = Intent.challenge(challenger.recordId(), defender.recordId());
        declare(state, challenge);

        SkirmishRecords.sync(generator, state, null);

        assertEquals(Set.of(nearClaim), SkirmishContests.declaredCells(generator, state));
        assertEquals(challenge, SkirmishContests.at(generator, state, nearClaim).intent());
        assertEquals(nearClaim, SkirmishContests.battleCell(generator, state, challenge));
    }

    private static long squaredDistance(AtlasCell from, AtlasCell to) {
        long dx = (long) from.gridX() - to.gridX();
        long dy = (long) from.gridY() - to.gridY();
        return dx * dx + dy * dy;
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
    void anOfficerOfTheSameFactionCanChallengeButNeverPush() {
        Border border = TestBorders.border(generator, state, "hegemony", "hegemony");
        int missingOfficer = border.attacker().recordId() + 50;
        declare(state, Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId()),
                Intent.push(missingOfficer, border.contested(), border.defender().recordId()));
        SkirmishRecords.sync(generator, state, null);
        assertTrue(SkirmishContests.declaredCells(generator, state).isEmpty());
        assertFalse(skirmishIn(generator, state, border.contested()));

        Intent challenge = Intent.challenge(border.attacker().recordId(), border.defender().recordId());
        declare(state, challenge);
        SkirmishRecords.sync(generator, state, null);
        assertTrue(skirmishIn(generator, state, border.contested()));
        assertEquals(new SkirmishContest(challenge, border.attacker(), border.defender()), SkirmishContests.at(generator, state, border.contested()));
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
    void aPlayerInsertedSkirmishFromAnOldSaveIsDroppedUnlessItsRunIsOpen() {
        Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        declare(state, Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId()));
        AtlasAnomalies.insert(generator, state, border.contested(), FactionSkirmish.ANOMALY_ID, AtlasAnomalyRecord.Source.PLAYER, null);

        SkirmishRecords.dropPlayerInserted(state, border.contested());
        assertEquals(1, state.anomalyRecords(border.contested()).size());

        SkirmishRecords.dropPlayerInserted(state, null);
        assertTrue(state.anomalyRecords(border.contested()).isEmpty());
    }
}
