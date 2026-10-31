package exiledsector.charts.atlas;

import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.PowerfulBoss;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.charts.wormhole.GatewayTierLimit;
import exiledsector.items.StarChart;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtlasOpeningTest {

    private static final long SEED = 0x0b5e55L;

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        List<AtlasSpecial> specials = List.of(
                new AtlasSpecial("nexus", AtlasSpecial.Kind.INFESTATION, AtlasSpecial.Placement.CHUNK, 1f, 3f, 2.5f, 1, 1f, "", "", "remnant"),
                new AtlasSpecial("bastion", AtlasSpecial.Kind.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", "", ""));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.3f, 0f, 0.3f),
                new AtlasMarkerRule(1f, 0f, 1f), specials,
                List.of(new AtlasAnomalyCandidate(SpatialBreach.ANOMALY_ID, 1f, ""), new AtlasAnomalyCandidate(PowerfulBoss.ANOMALY_ID, 1f, "")));
    }

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
        AtlasData.registerNodeEffects(new JSONArray()
                .put(new JSONObject().put("id", AtlasOpening.INFESTED).put("effects", "CHART_FLEET_SIZE_PERCENT:25"))
                .put(new JSONObject().put("id", AtlasOpening.CLEANSED).put("effects", "CHART_REWARD_PERCENT:50"))
                .put(new JSONObject().put("id", AtlasOpening.NEXUS).put("effects", "CHART_BOSS_SIZE_PERCENT:100; CHART_FLEET_SIZE_PERCENT:-10")));
    }

    @AfterEach
    void tearDown() {
        AtlasData.resetForTests();
        ChartTables.resetAnomalies();
    }

    private static AtlasCell findInfested(AtlasGenerator generator, boolean nexus) {
        for (AtlasCell cell : AtlasCell.ORIGIN.around(20)) {
            AtlasNode node = generator.node(cell);
            AtlasSpecial special = generator.specialAt(cell);
            boolean isNexus = special != null && special.infests();
            if (!node.isVoid() && node.infestationId() != null && isNexus == nexus) {
                return cell;
            }
        }
        throw new AssertionError("no infested cell");
    }

    @Test
    void aPowerfulBossInfestedNodeCarriesItsFactionLinesAndAnomaliesUntilItsMarkersAreSpent() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell cell = findInfested(generator, false);

        AtlasRunNode node = AtlasOpening.runNode(generator, state, cell);

        assertEquals("remnant", node.factionId());
        assertEquals(Set.of(SpatialBreach.ANOMALY_ID, PowerfulBoss.ANOMALY_ID), Set.copyOf(node.anomalyIds()));
        assertTrue(node.guaranteedBossTierUp());
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 25f)), node.effects());
        ChartGenContext context = ChartGenContext.none().withAtlasNode(node).withAnomalyEffects(node.anomalyIds());
        assertEquals(ChartAnomalies.definition(PowerfulBoss.ANOMALY_ID).effects().get(0).value(), context.bossSizePercent());
        assertTrue(context.bossSpoilsMult() > 1f);

        state.add(cell, AtlasState.MARKERS_SPENT | AtlasState.CLEANSED);
        AtlasRunNode spent = AtlasOpening.runNode(generator, state, cell);

        assertEquals(List.of(), spent.anomalyIds());
        assertFalse(spent.guaranteedBossTierUp());
        assertEquals(generator.node(cell).factionId(), spent.factionId());
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_REWARD_PERCENT, 50f)), spent.effects());
    }

    @Test
    void aCaptureHandsTheSystemToItsCaptorButAnInfestationHoldsItUntilCleansed() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell infested = findInfested(generator, false);

        state.capture(generator, infested, "hegemony");

        assertEquals(Map.of(infested, "hegemony"), state.captures());
        assertEquals("hegemony", AtlasOpening.territoryOwnerOf(generator, state, infested));
        assertEquals("remnant", AtlasOpening.ownerOf(generator, state, infested));
        state.add(infested, AtlasState.CLEANSED);
        assertEquals("hegemony", AtlasOpening.ownerOf(generator, state, infested));
        assertEquals("hegemony", AtlasOpening.runNode(generator, state, infested).factionId());

        state.capture(generator, infested, "pirates");

        assertEquals(Map.of(), state.captures());
        assertEquals("pirates", AtlasOpening.ownerOf(generator, state, infested));
        state.capture(generator, infested, "tritachyon");
        assertTrue(state.release(infested));
        assertFalse(state.release(infested));
        assertEquals("pirates", AtlasOpening.territoryOwnerOf(generator, state, infested));
    }

    @Test
    void clearingANexusCleansesItsInfestationAndGrantsAPoint() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell nexus = findInfested(generator, true);
        AtlasCell infested = null;
        for (AtlasCell cell : nexus.around(3)) {
            if (!cell.equals(nexus) && nexus.equals(generator.infestationSource(cell))) {
                infested = cell;
            }
        }
        assertNotNull(infested);

        AtlasProgress.complete(generator, state, nexus, 7);

        assertTrue(state.isCompleted(nexus));
        assertTrue(state.has(infested, AtlasState.CLEANSED));
        assertEquals(2, state.atlasPoints());
        AtlasProgress.complete(generator, state, nexus, 7);
        assertEquals(2, state.atlasPoints());
    }

    @Test
    void clearingANexusLeavesTheZoneOfANeighbouringNexusOfTheSameKindInfested() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell nexus = null;
        AtlasCell neighbourZone = null;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(20)) {
            AtlasSpecial special = generator.specialAt(cell);
            if (special == null || !special.infests() || nexus != null) {
                continue;
            }
            for (AtlasCell near : cell.around(4)) {
                AtlasCell source = generator.infestationSource(near);
                if (source != null && !source.equals(cell)) {
                    nexus = cell;
                    neighbourZone = near;
                }
            }
        }
        assertNotNull(nexus);

        AtlasProgress.complete(generator, state, nexus, 7);

        assertFalse(state.has(neighbourZone, AtlasState.CLEANSED));
        for (AtlasCell cell : nexus.around(4)) {
            boolean ownZone = !generator.isVoid(cell) && nexus.equals(generator.infestationSource(cell));
            assertEquals(ownZone, state.has(cell, AtlasState.CLEANSED), cell.toString());
        }
    }

    @Test
    void aBastionGivesAKeyFragmentAndANexusNeedsFourModifiers() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell bastion = AtlasCell.nearest(0f, 25f);

        AtlasProgress.complete(generator, state, bastion, 15);

        assertEquals(1, state.keyFragments());
        assertEquals(AtlasOpening.Blocker.NOT_REACHABLE, AtlasOpening.check(generator, state, findInfested(generator, true), null));
        assertEquals(0, AtlasOpening.explicitModifiers(null));
    }

    private static AtlasCell reachablePlainCell(AtlasGenerator generator, AtlasState state) {
        for (AtlasCell start : AtlasCell.ORIGIN.around(10)) {
            if (generator.isVoid(start)) {
                continue;
            }
            for (AtlasCell neighbour : generator.lanes(start)) {
                if (!generator.isVoid(neighbour) && generator.specialAt(neighbour) == null) {
                    state.add(start, AtlasState.COMPLETED);
                    return neighbour;
                }
            }
        }
        throw new AssertionError("no reachable plain cell");
    }

    private static StarChart chartOfTier(int tier) {
        StarChart chart = mock(StarChart.class);
        when(chart.tier()).thenReturn(tier);
        when(chart.rolls()).thenReturn(List.of());
        return chart;
    }

    @Test
    void theGatewayTierLimitBlocksChartsAboveIt() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell cell = reachablePlainCell(generator, state);

        assertEquals(AtlasOpening.Blocker.GATEWAY_TIER_LIMIT,
                AtlasOpening.check(generator, state, cell, chartOfTier(6), GatewayTierLimit.BASE_MAX_TIER));
        assertEquals(AtlasOpening.Blocker.NONE, AtlasOpening.check(generator, state, cell, chartOfTier(5), GatewayTierLimit.BASE_MAX_TIER));
        assertEquals(AtlasOpening.Blocker.NONE, AtlasOpening.check(generator, state, cell, null, GatewayTierLimit.BASE_MAX_TIER));
        assertEquals(AtlasOpening.Blocker.NONE, AtlasOpening.check(generator, state, cell, chartOfTier(16)));
        assertTrue(AtlasOpening.Blocker.GATEWAY_TIER_LIMIT.reasonKey().startsWith("chart.atlas.blocked.gateway_tier_limit."));
    }

    @Test
    void aRunNodeSurvivesEncoding() {
        AtlasRunNode node = new AtlasRunNode(new AtlasCell(-3, 7).pack(), "remnant",
                List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 50f),
                        new ChartGenContext.AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, -30f)), List.of("spatial_breach", "black_hole"), true);

        assertEquals(node, AtlasRunNode.decode(node.encode()));
        assertEquals(new AtlasRunNode(5L, "", List.of(), List.of(), false), AtlasRunNode.decode(new AtlasRunNode(5L, null, null, null, false).encode()));
        assertEquals(null, AtlasRunNode.decode("garbage"));
        assertEquals(Set.of(), Set.copyOf(AtlasRunNode.decode("1|||| false").effects()));
    }

    @Test
    void onlyWinningOnTheAttackersSideCapturesTheSystem() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell cell = new AtlasCell(3, 2);
        AtlasRunNode node = new AtlasRunNode(cell.pack(), "pirates", List.of(), List.of("faction_skirmish"), false);

        AtlasProgress.captureForWinner(generator, state, cell, node.withSkirmish("hegemony", SkirmishSide.DEFENDER));
        AtlasProgress.captureForWinner(generator, state, cell, node.withSkirmish("hegemony", SkirmishSide.NEITHER));
        AtlasProgress.captureForWinner(generator, state, cell, node);
        assertEquals(Map.of(), state.captures());

        AtlasProgress.captureForWinner(generator, state, cell, node.withSkirmish("hegemony", SkirmishSide.ATTACKER));
        assertEquals(Map.of(cell, "hegemony"), state.captures());
    }

    @Test
    void aRunNodeKeepsItsSkirmishSidesAndOlderNodesReadWithout() {
        AtlasRunNode node = new AtlasRunNode(9L, "hegemony", List.of(), List.of("faction_skirmish", "spatial_breach"), true, "tritachyon",
                SkirmishSide.ATTACKER);
        AtlasRunNode decoded = AtlasRunNode.decode(node.encode());

        assertEquals(node, decoded);
        assertEquals(List.of("faction_skirmish", "spatial_breach"), decoded.anomalyIds());
        assertTrue(decoded.guaranteedBossTierUp());
        AtlasRunNode older = AtlasRunNode.decode("9|hegemony||spatial_breach|true");
        assertEquals(new AtlasRunNode(9L, "hegemony", List.of(), List.of("spatial_breach"), true), older);
        assertEquals(null, older.skirmishSide());
        assertEquals(null, new AtlasRunNode(9L, "", List.of(), List.of(), false, "tritachyon", null).skirmishAttackerId());
        assertEquals(SkirmishSide.ATTACKER, ChartGenContext.none().withAtlasNode(node).skirmishSide());
        assertEquals("tritachyon", ChartGenContext.none().withAtlasNode(node).withAnomalies(List.of("black_hole")).skirmishAttackerId());
    }

    @Test
    void aRunNodeKeepsItsGuaranteedUniqueAndSkirmishNodesSavedBeforeItReadWithout() {
        AtlasRunNode node = new AtlasRunNode(9L, "hegemony", List.of(), List.of("relay_network"), false)
                .withExtraEffects(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 50f)), true);
        AtlasRunNode decoded = AtlasRunNode.decode(node.encode());

        assertEquals(node, decoded);
        assertTrue(decoded.guaranteedBossUnique());
        assertEquals(50f, decoded.effects().get(0).value());
        assertTrue(ChartGenContext.none().withAtlasNode(decoded).withAnomalyEffects(List.of("relay_network")).withSkirmishSide(SkirmishSide.NEITHER)
                .guaranteedBossUnique());
        AtlasRunNode skirmishEra = AtlasRunNode.decode("9|hegemony||faction_skirmish|true|tritachyon|attacker");
        assertEquals(SkirmishSide.ATTACKER, skirmishEra.skirmishSide());
        assertFalse(skirmishEra.guaranteedBossUnique());
        assertTrue(skirmishEra.withSkirmish("tritachyon", SkirmishSide.DEFENDER).withExtraEffects(List.of(), true)
                .withSkirmish("tritachyon", SkirmishSide.ATTACKER).guaranteedBossUnique());
    }

    @Test
    void aRunNodeSavedWithSeparateExpansionAndPhenomenonFieldsReadsBothAsAnomalies() {
        AtlasRunNode old = AtlasRunNode.decode("9|pirates|CHART_BOSS_SIZE_PERCENT:50.0|spatial_breach|black_hole|true");

        assertNotNull(old);
        assertEquals(List.of("spatial_breach", "black_hole"), old.anomalyIds());
        assertTrue(old.guaranteedBossTierUp());
        assertEquals(List.of(), AtlasRunNode.decode("9|pirates||||false").anomalyIds());
    }
}
