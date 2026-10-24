package exiledsector.charts.atlas;

import exiledsector.charts.AtlasRunNode;
import exiledsector.charts.ChartEffect;
import exiledsector.charts.ChartGenContext;
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

class AtlasOpeningTest {

    private static final long SEED = 0x0b5e55L;

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        List<AtlasSpecial> specials = List.of(
                new AtlasSpecial("nexus", AtlasSpecial.INFESTATION, AtlasSpecial.Placement.CHUNK, 1f, 3f, 2.5f, 1, 1f, "", "", "remnant"),
                new AtlasSpecial("bastion", AtlasReach.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", ""));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.3f, 0f, 0.3f),
                new AtlasMarkerRule(1f, 0f, 1f), new AtlasMarkerRule(1f, 0f, 1f), AtlasMarkerRule.NEVER, specials,
                List.of(new AtlasMarkerCandidate("relay", 1f, "")), List.of());
    }

    @BeforeEach
    void setUp() throws Exception {
        AtlasData.registerNodeEffects(new JSONArray()
                .put(new JSONObject().put("id", AtlasOpening.POWERFUL_BOSS).put("effects", "CHART_BOSS_SIZE_PERCENT:50"))
                .put(new JSONObject().put("id", AtlasOpening.INFESTED).put("effects", "CHART_FLEET_SIZE_PERCENT:25"))
                .put(new JSONObject().put("id", AtlasOpening.CLEANSED).put("effects", "CHART_REWARD_PERCENT:50"))
                .put(new JSONObject().put("id", AtlasOpening.NEXUS).put("effects", "CHART_BOSS_SIZE_PERCENT:100; CHART_FLEET_SIZE_PERCENT:-10")));
    }

    @AfterEach
    void tearDown() {
        AtlasData.resetForTests();
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
    void aPowerfulInfestedNodeCarriesItsMarkersFactionAndExpansionUntilItsMarkersAreSpent() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell cell = findInfested(generator, false);

        AtlasRunNode node = AtlasOpening.runNode(generator, state, cell);

        assertEquals("remnant", node.factionId());
        assertEquals("relay", node.expansionId());
        assertTrue(node.guaranteedBossTierUp());
        assertTrue(node.effects().contains(new ChartGenContext.AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 50f)));
        assertTrue(node.effects().contains(new ChartGenContext.AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 25f)));

        state.add(cell, AtlasState.MARKERS_SPENT | AtlasState.CLEANSED);
        AtlasRunNode spent = AtlasOpening.runNode(generator, state, cell);

        assertEquals("", spent.expansionId());
        assertFalse(spent.guaranteedBossTierUp());
        assertEquals(generator.node(cell).factionId(), spent.factionId());
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_REWARD_PERCENT, 50f)), spent.effects());
    }

    @Test
    void clearingANexusCleansesItsInfestationAndGrantsAPoint() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell nexus = findInfested(generator, true);
        AtlasCell infested = null;
        for (AtlasCell cell : nexus.around(3)) {
            if (!cell.equals(nexus) && "nexus".equals(generator.node(cell).infestationId())) {
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
    void aBastionGivesAKeyFragmentAndANexusNeedsFourModifiers() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasCell bastion = AtlasCell.nearest(0f, 25f);

        AtlasProgress.complete(generator, state, bastion, 15);

        assertEquals(1, state.keyFragments());
        assertEquals(AtlasOpening.Blocker.NOT_REACHABLE, AtlasOpening.check(generator, state, findInfested(generator, true), null));
        assertEquals(0, AtlasOpening.explicitModifiers(null));
    }

    @Test
    void aRunNodeSurvivesEncoding() {
        AtlasRunNode node = new AtlasRunNode(new AtlasCell(-3, 7).pack(), "remnant",
                List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 50f),
                        new ChartGenContext.AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, -30f)), "relay", "black_hole", true);

        assertEquals(node, AtlasRunNode.decode(node.encode()));
        assertEquals(new AtlasRunNode(5L, "", List.of(), "", "", false), AtlasRunNode.decode(new AtlasRunNode(5L, null, null, null, null, false).encode()));
        assertEquals(null, AtlasRunNode.decode("garbage"));
        assertEquals(Set.of(), Set.copyOf(AtlasRunNode.decode("1||||| false").effects()));
    }
}
