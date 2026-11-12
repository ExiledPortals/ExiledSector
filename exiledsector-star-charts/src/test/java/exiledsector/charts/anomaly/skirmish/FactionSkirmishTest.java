package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.ChartTables;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyCandidate;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import exiledsector.charts.atlas.TestTerritories;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.UseVerdict;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionSkirmishTest {

    private static final long SEED = 0x5C1A7L;
    private static final String NEUTRAL = "remnant";
    private static final AtlasRules RULES = new AtlasRules(List.of(AtlasRegion.DEFAULT_NORMAL), Map.of("compact", 1f),
            Map.of("hegemony", 1f, "tritachyon", 1f, "pirates", 1f, NEUTRAL, 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER,
            List.of(), List.of());

    private static AtlasTraits traits(String factionId, List<String> neighbourFactionIds, String specialId, String infestationId) {
        return new AtlasTraits(new AtlasCell(2, 1), 5f, AtlasRegion.NORMAL, factionId, factionId, "compact", Set.of(), neighbourFactionIds,
                specialId, infestationId);
    }

    private static AtlasCell borderSystem(AtlasGenerator generator, AtlasState state) {
        for (AtlasCell cell : AtlasCell.ORIGIN.around(20)) {
            AtlasCell neighbour = cell.equals(AtlasCell.ORIGIN) || generator.isVoid(cell) ? null
                    : generator.lanes(cell).stream().filter(lane -> !lane.equals(AtlasCell.ORIGIN)).findFirst().orElse(null);
            if (neighbour == null) {
                continue;
            }
            TestTerritories.establish(state, neighbour, "hegemony");
            if (FactionSkirmish.INSTANCE.fitsAt(generator.traits(cell)) && FactionSkirmish.INSTANCE.holdsAt(generator, state, cell)) {
                return cell;
            }
            AtlasTerritoryTransfer.vacate(generator, state, neighbour);
        }
        throw new AssertionError("no border system");
    }

    private static Set<String> laneOwners(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        Set<String> owners = new HashSet<>();
        generator.lanes(cell).forEach(neighbour -> owners.add(AtlasOpening.ownerOf(generator, state, neighbour)));
        return owners;
    }

    @BeforeEach
    void setUp() {
        SkirmishFactions.useForTests(Set.of(NEUTRAL));
    }

    @AfterEach
    void tearDown() {
        SkirmishFactions.useForTests(Set.of());
        ChartTables.resetAnomalies();
    }

    @Test
    void fitsOnlyWhereTwoFactionsThatTakeSidesMeetOutsideSpecialAndInfestedSystems() {
        assertTrue(FactionSkirmish.INSTANCE.fitsAt(traits("hegemony", List.of("hegemony", "tritachyon"), null, null)));
        assertFalse(FactionSkirmish.INSTANCE.fitsAt(traits("hegemony", List.of("hegemony", "hegemony"), null, null)));
        assertFalse(FactionSkirmish.INSTANCE.fitsAt(traits("hegemony", List.of(NEUTRAL), null, null)));
        assertFalse(FactionSkirmish.INSTANCE.fitsAt(traits(NEUTRAL, List.of("hegemony"), null, null)));
        assertFalse(FactionSkirmish.INSTANCE.fitsAt(traits("hegemony", List.of("tritachyon"), "bastion", null)));
        assertFalse(FactionSkirmish.INSTANCE.fitsAt(traits("hegemony", List.of("tritachyon"), null, "remnant_infestation")));
    }

    @Test
    void theAttackerOwnsALaneNeighbourAndNeverChangesWhileTheMapStaysTheSame() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
        AtlasCell cell = borderSystem(generator, state);

        String defender = FactionSkirmish.defender(generator, state, cell);
        String attacker = FactionSkirmish.attacker(generator, state, cell);

        assertNotEquals(defender, attacker);
        assertNotEquals(NEUTRAL, attacker);
        assertTrue(laneOwners(generator, state, cell).contains(attacker));
        assertEquals(attacker, FactionSkirmish.attacker(new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state), state, cell));
    }

    @Test
    void theSkirmishDropsFromASystemOnceNoNeighbourCanAttack() {
        ChartTables.useAnomalies(List.of(FactionSkirmish.INSTANCE),
                List.of(new ChartAnomalies.Definition(FactionSkirmish.ANOMALY_ID, 1f, 1, 15, "", "", true, Set.of(), false)));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
        AtlasCell cell = borderSystem(generator, state);
        AtlasAnomalies.insert(generator, state, cell, FactionSkirmish.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null);
        assertEquals(List.of(FactionSkirmish.ANOMALY_ID), AtlasAnomalies.effective(generator, state, cell));

        Set<String> neutral = new HashSet<>(laneOwners(generator, state, cell));
        neutral.remove(FactionSkirmish.defender(generator, state, cell));
        SkirmishFactions.useForTests(neutral);

        assertEquals(List.of(), AtlasAnomalies.effective(generator, state, cell));
    }

    @Test
    void assaultOrdersInsertOnlyWhereTheCurrentOwnerCanBeAttackedAndSayWhyNot() {
        ChartTables.useAnomalies(List.of(FactionSkirmish.INSTANCE),
                List.of(new ChartAnomalies.Definition(FactionSkirmish.ANOMALY_ID, 1f, 1, 15, "", "", true, Set.of(), false)));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
        AtlasCell cell = borderSystem(generator, state);
        AtlasCatalysts.Catalyst assaultOrders = new AtlasCatalysts.Catalyst("test_assault_orders", FactionSkirmish.ANOMALY_ID, AtlasCatalysts.Family.ORDERS,
                1f, 1);
        AtlasCatalystUse use = AtlasCatalystUse.of(assaultOrders, generator, state, null, Set.of(cell));
        assertTrue(use.check(cell).allowed());
        String owner = FactionSkirmish.defender(generator, state, cell);

        TestTerritories.establish(state, cell, NEUTRAL);
        UseVerdict neutralOwner = use.check(cell);
        assertEquals(FactionSkirmish.NO_SIDE_KEY, neutralOwner.refusalKey());
        assertEquals(ChartFactions.displayName(NEUTRAL), neutralOwner.args().get("faction"));

        AtlasTerritoryTransfer.vacate(generator, state, cell);
        assertEquals(owner, FactionSkirmish.defender(generator, state, cell));
        Set<String> neutral = new HashSet<>(laneOwners(generator, state, cell));
        neutral.remove(FactionSkirmish.defender(generator, state, cell));
        SkirmishFactions.useForTests(neutral);

        assertEquals(FactionSkirmish.NO_ATTACKER_KEY, use.check(cell).refusalKey());
        assertEquals(AtlasAnomalies.InsertCheck.DOES_NOT_FIT, AtlasAnomalies.canInsert(generator, state, cell, FactionSkirmish.ANOMALY_ID, null));
    }

    @Test
    void openingASkirmishSystemRecordsTheAttackerAndTheSideStayingOutByDefault() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
        AtlasCell cell = borderSystem(generator, state);
        AtlasRunNode skirmish = new AtlasRunNode(cell.pack(), "hegemony", List.of(), List.of(FactionSkirmish.ANOMALY_ID), false);
        AtlasRunNode plain = new AtlasRunNode(cell.pack(), "hegemony", List.of(), List.of(), false);

        SkirmishJoin joined = SkirmishJoin.of(ChartAnomalies.joinRunNode(skirmish, generator, state, cell, SkirmishSide.DEFENDER));

        assertEquals(FactionSkirmish.attacker(generator, state, cell), joined.attackerId());
        assertEquals(SkirmishSide.DEFENDER, joined.side());
        assertEquals(SkirmishSide.NEITHER, SkirmishJoin.of(ChartAnomalies.joinRunNode(skirmish, generator, state, cell, null)).side());
        assertEquals(plain, ChartAnomalies.joinRunNode(plain, generator, state, cell, SkirmishSide.ATTACKER));
    }

    @Test
    void onlyWinningOnTheAttackersSideJoinsTheSystemToTheAttackersNeighbouringTerritory() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
        AtlasCell cell = borderSystem(generator, state);
        AtlasCell neighbour = generator.lanes(cell).stream().filter(lane -> state.officerAt(lane) != null).findFirst().orElseThrow();

        FactionSkirmish.captureForWinner(generator, state, cell, new SkirmishJoin(SkirmishSide.DEFENDER, "hegemony"));
        FactionSkirmish.captureForWinner(generator, state, cell, new SkirmishJoin(SkirmishSide.NEITHER, "hegemony"));
        FactionSkirmish.captureForWinner(generator, state, cell, new SkirmishJoin(SkirmishSide.ATTACKER, null));
        assertNull(state.commandCentreOf(cell));

        FactionSkirmish.captureForWinner(generator, state, cell, new SkirmishJoin(SkirmishSide.ATTACKER, "hegemony"));
        assertEquals(neighbour, state.commandCentreOf(cell));
        assertEquals("hegemony", AtlasOpening.territoryOwnerOf(generator, state, cell));
    }

    @Test
    void theJoinSurvivesTheRunNodeEncodingAndReachesTheChartContext() {
        AtlasRunNode node = new AtlasRunNode(9L, "hegemony", List.of(), List.of(FactionSkirmish.ANOMALY_ID), true)
                .withAnomalyData(FactionSkirmish.ANOMALY_ID, new SkirmishJoin(SkirmishSide.ATTACKER, "tritachyon").encode());
        AtlasRunNode decoded = AtlasRunNode.decode(node.encode());

        assertEquals(node, decoded);
        assertEquals(new SkirmishJoin(SkirmishSide.ATTACKER, "tritachyon"), SkirmishJoin.of(decoded));
        assertEquals(new SkirmishJoin(SkirmishSide.ATTACKER, "tritachyon"),
                SkirmishJoin.of(ChartGenContext.none().withAtlasNode(decoded).withAnomalies(List.of("black_hole"))));
        assertEquals(new SkirmishJoin(SkirmishSide.NEITHER, null), SkirmishJoin.decode(new SkirmishJoin(null, "").encode()));
        assertEquals(null, SkirmishJoin.decode("attacker"));
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
    void theAtlasRollSkipsSystemsTheSkirmishDoesNotFit() throws Exception {
        ChartTables.loadAnomalies();
        AtlasAnomalyCandidate candidate = AtlasData.rules().anomalies().stream()
                .filter(anomaly -> anomaly.id().equals(FactionSkirmish.ANOMALY_ID)).findFirst().orElseThrow();

        assertTrue(candidate.weighting().weightFor(traits("hegemony", List.of("tritachyon"), null, null), 1f) > 0f);
        assertEquals(0f, candidate.weighting().weightFor(traits("hegemony", List.of("hegemony"), null, null), 1f));
    }
}
