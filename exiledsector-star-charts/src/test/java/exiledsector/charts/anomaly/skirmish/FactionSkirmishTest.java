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
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.OfficerTurnTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionSkirmishTest {

    private AtlasState state;
    private AtlasGenerator generator;
    private Border border;
    private Intent push;

    private static AtlasTraits traits(String factionId) {
        return new AtlasTraits(new AtlasCell(2, 1), 5f, AtlasRegion.NORMAL, factionId, factionId, "compact", Set.of(), List.of("tritachyon"), null, null);
    }

    private AtlasRunNode joinNode() {
        AtlasRunNode skirmish = new AtlasRunNode(border.contested().pack(), "hegemony", List.of(), List.of(FactionSkirmish.ANOMALY_ID));
        return ChartAnomalies.joinRunNode(skirmish, generator, state, border.contested(), null);
    }

    @BeforeEach
    void setUp() {
        state = new AtlasState(TestBorders.SEED);
        generator = TestBorders.generator(state);
        border = TestBorders.border(generator, state, "tritachyon", "hegemony");
        push = Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId());
        state.setDeclaredIntents(List.of(push.encode()));
    }

    @AfterEach
    void tearDown() {
        OfficerTurnTables.useForTests(null);
        ChartTables.resetAnomalies();
    }

    @Test
    void openingTheSkirmishRecordsBothOfficersAndTheContest() {
        AtlasRunNode plain = new AtlasRunNode(border.contested().pack(), "hegemony", List.of(), List.of());
        SkirmishFight fight = SkirmishFight.of(joinNode());

        assertEquals(new SkirmishFight("tritachyon", border.attacker().rankId(), border.defender().rankId(), push), fight);
        assertTrue(fight.betweenOfficers());
        assertEquals(Set.of(border.attacker().recordId(), border.defender().recordId()), OfficerFleets.fightingRecordIdsOf(joinNode()));
        assertEquals(plain, ChartAnomalies.joinRunNode(plain, generator, state, border.contested(), null));
        assertEquals(Set.of(), OfficerFleets.fightingRecordIdsOf(plain));
    }

    @Test
    void theFightSurvivesTheRunNodeEncodingAndReachesTheChartContext() {
        AtlasRunNode decoded = AtlasRunNode.decode(joinNode().encode());
        SkirmishFight expected = new SkirmishFight("tritachyon", border.attacker().rankId(), border.defender().rankId(), push);

        assertEquals(expected, SkirmishFight.of(decoded));
        assertEquals(expected, SkirmishFight.of(ChartGenContext.none().withAtlasNode(decoded).withAnomalies(List.of("black_hole"))));
        assertEquals(new SkirmishFight(null, null, null, null), SkirmishFight.decode(new SkirmishFight("", "", "", null).encode()));
        assertNull(SkirmishFight.decode("attacker"));
    }

    @Test
    void theAttackersWinMovesTheCellAndTheDefendersWinKeepsIt() {
        assertFalse(FactionSkirmish.settle(generator, state, border.contested(), push, SkirmishResult.DEFENDER_WINS));
        assertEquals(border.defender().commandCentre(), state.commandCentreOf(border.contested()));
        assertTrue(state.declaredIntents().isEmpty());

        state.setDeclaredIntents(List.of(push.encode()));
        assertTrue(FactionSkirmish.settle(generator, state, border.contested(), push, SkirmishResult.ATTACKER_WINS));
        assertEquals(border.attacker().commandCentre(), state.commandCentreOf(border.contested()));
        assertEquals("tritachyon", AtlasOpening.territoryOwnerOf(generator, state, border.contested()));
    }

    @Test
    void aDroppedFightEndsWithoutAWinnerAndAnUndecidedOneWaitsForTheNextTurn() {
        assertFalse(FactionSkirmish.settle(generator, state, border.contested(), push, SkirmishResult.UNDECIDED));
        assertEquals(List.of(push.encode()), state.declaredIntents());

        assertFalse(FactionSkirmish.settle(generator, state, border.contested(), push, SkirmishResult.DROPPED));
        assertTrue(state.declaredIntents().isEmpty());
        assertEquals(border.defender().commandCentre(), state.commandCentreOf(border.contested()));
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
