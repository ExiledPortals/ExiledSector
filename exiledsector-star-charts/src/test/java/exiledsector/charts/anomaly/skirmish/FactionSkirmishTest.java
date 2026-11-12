package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyCandidate;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.atlas.TestBorders;
import exiledsector.charts.atlas.TestBorders.Border;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.OfficerTurnTables;
import exiledsector.charts.officer.turn.TurnRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.charts.atlas.TestBorders.NEUTRAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionSkirmishTest {

    private static final TurnRules ATTACKER_ALWAYS_WINS = new TurnRules(4, 6f, 100f, 0f, 0f, 1, 3, 2, 0, 50, 6, Map.of());
    private static final TurnRules DEFENDER_ALWAYS_WINS = new TurnRules(4, 6f, 0f, 0f, 0f, 1, 3, 2, 0, 50, 6, Map.of());

    private AtlasState state;
    private AtlasGenerator generator;
    private Border border;
    private Intent push;

    private static AtlasTraits traits(String factionId) {
        return new AtlasTraits(new AtlasCell(2, 1), 5f, AtlasRegion.NORMAL, factionId, factionId, "compact", Set.of(), List.of("tritachyon"), null, null);
    }

    private SkirmishJoin joinedOn(SkirmishSide side) {
        return SkirmishJoin.of(joinNode(side));
    }

    private AtlasRunNode joinNode(SkirmishSide side) {
        AtlasRunNode skirmish = new AtlasRunNode(border.contested().pack(), "hegemony", List.of(), List.of(FactionSkirmish.ANOMALY_ID));
        return ChartAnomalies.joinRunNode(skirmish, generator, state, border.contested(), side);
    }

    @BeforeEach
    void setUp() {
        SkirmishFactions.useForTests(Set.of(NEUTRAL));
        state = new AtlasState(TestBorders.SEED);
        generator = TestBorders.generator(state);
        border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        push = Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId());
        state.setDeclaredIntents(List.of(push.encode()));
    }

    @AfterEach
    void tearDown() {
        SkirmishFactions.useForTests(Set.of());
        OfficerTurnTables.useForTests(null);
        ChartTables.resetAnomalies();
    }

    @Test
    void openingTheSkirmishRecordsTheContestAndTheSideStayingOutByDefault() {
        AtlasRunNode plain = new AtlasRunNode(border.contested().pack(), "hegemony", List.of(), List.of());

        assertEquals(new SkirmishJoin(SkirmishSide.ATTACKER, "tritachyon", push), joinedOn(SkirmishSide.ATTACKER));
        assertEquals(SkirmishSide.NEITHER, joinedOn(null).side());
        assertEquals(plain, ChartAnomalies.joinRunNode(plain, generator, state, border.contested(), SkirmishSide.ATTACKER));
    }

    @Test
    void sidingWithTheDefenderMakesTheAttackingOfficerTheRunsOfficer() {
        assertEquals(border.attacker().recordId(), OfficerFleets.recordIdOf(joinNode(SkirmishSide.DEFENDER)));
        assertNull(OfficerFleets.recordIdOf(joinNode(SkirmishSide.ATTACKER)));
        assertNull(OfficerFleets.recordIdOf(joinNode(SkirmishSide.NEITHER)));
    }

    @Test
    void theJoinSurvivesTheRunNodeEncodingAndReachesTheChartContext() {
        AtlasRunNode decoded = AtlasRunNode.decode(joinNode(SkirmishSide.DEFENDER).encode());
        SkirmishJoin expected = new SkirmishJoin(SkirmishSide.DEFENDER, "tritachyon", push);

        assertEquals(expected, SkirmishJoin.of(decoded));
        assertEquals(expected, SkirmishJoin.of(ChartGenContext.none().withAtlasNode(decoded).withAnomalies(List.of("black_hole"))));
        assertEquals(new SkirmishJoin(SkirmishSide.NEITHER, null, null), SkirmishJoin.decode(new SkirmishJoin(null, "", null).encode()));
        assertNull(SkirmishJoin.decode("attacker"));
    }

    @Test
    void stayingOutDecidesThePushOnItsAbstractRoll() {
        OfficerTurnTables.useForTests(DEFENDER_ALWAYS_WINS);
        assertFalse(FactionSkirmish.stayedOut(generator, state, border.contested(), push));
        assertEquals(border.defender().commandCentre(), state.commandCentreOf(border.contested()));
        assertTrue(state.declaredIntents().isEmpty());

        state.setDeclaredIntents(List.of(push.encode()));
        OfficerTurnTables.useForTests(ATTACKER_ALWAYS_WINS);
        assertTrue(FactionSkirmish.stayedOut(generator, state, border.contested(), push));
        assertEquals(border.attacker().commandCentre(), state.commandCentreOf(border.contested()));
        assertEquals("tritachyon", AtlasOpening.territoryOwnerOf(generator, state, border.contested()));
    }

    @Test
    void theConsoleSkirmishIsAlwaysLedByAnOfficer() {
        AtlasOfficer attacker = FactionSkirmish.anyAttacker(state);
        ChartGenContext joined = FactionSkirmish.joinedOn(new ChartLayoutRequest(77L, null), SkirmishSide.ATTACKER, attacker).chartContext();
        SkirmishJoin join = SkirmishJoin.of(joined);

        assertEquals(border.defender(), attacker);
        assertEquals(attacker.factionId(), join.attackerId());
        assertEquals(attacker.recordId(), join.contest().officerRecordId());
        SkirmishFactions.useForTests(Set.of(NEUTRAL, "hegemony", "tritachyon"));
        assertNull(FactionSkirmish.anyAttacker(state));
    }

    @Test
    void theShippedListKeepsTheFactionsThatNeverTakeSidesOut() throws Exception {
        SkirmishFactions.register(ChartTables.csvRows(SkirmishFactions.PATH));

        for (String neutral : List.of("remnant", "derelict", "threat", "rat_abyssals")) {
            assertFalse(SkirmishFactions.takesSides(neutral), neutral);
        }
        assertTrue(SkirmishFactions.takesSides("hegemony"));
        assertFalse(SkirmishFactions.takesSides(null));
        assertFalse(SkirmishFactions.takesSides(""));
    }

    @Test
    void theAtlasNeverRollsASkirmishOnItsOwn() throws Exception {
        ChartTables.loadAnomalies();
        AtlasAnomalyCandidate candidate = AtlasData.rules().anomalies().stream()
                .filter(anomaly -> anomaly.id().equals(FactionSkirmish.ANOMALY_ID)).findFirst().orElseThrow();

        assertEquals(0f, candidate.weighting().weightFor(traits("hegemony"), 1f));
        assertEquals(0f, ChartAnomalies.definitions().stream().filter(definition -> definition.id().equals(FactionSkirmish.ANOMALY_ID))
                .findFirst().orElseThrow().openingChance());
    }
}
