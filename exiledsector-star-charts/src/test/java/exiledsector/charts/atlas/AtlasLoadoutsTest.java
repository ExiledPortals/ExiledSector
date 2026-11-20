package exiledsector.charts.atlas;

import exiledsector.charts.anomaly.DenseNebula;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasLoadoutsTest {

    private static final long SEED = 0x10ad0L;
    private static final AtlasEffects WIDER = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 2f)));
    private static final AtlasEffects NEBULOUS = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 150f)));

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of()));
        List<AtlasAnomalyCandidate> anomalies = List.of(new AtlasAnomalyCandidate(DenseNebula.ANOMALY_ID, 1f, "", false, Set.of(), null,
                new AtlasAnomalyChance(0.4f, 0f, 0.4f)));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER,
                List.of(), anomalies);
    }

    private static AtlasGenerator generator(AtlasState state, AtlasEffects effects) {
        return new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, effects, state);
    }

    private static boolean hasNebula(AtlasGenerator generator, AtlasCell cell) {
        return generator.node(cell).anomalyIds().contains(DenseNebula.ANOMALY_ID);
    }

    @Test
    void eachAtlasKeepsItsOwnNodesAndTheFirstIsActiveAtFirst() {
        AtlasState state = new AtlasState(SEED);
        assertEquals(0, state.activeAtlasLoadout());

        state.setAtlasTreeNodeIds(Set.of("a", "b"));
        assertTrue(state.selectAtlasLoadout(2));
        assertEquals(Set.of(), state.atlasTreeNodeIds());
        state.setAtlasTreeNodeIds(Set.of("c"));

        assertTrue(state.selectAtlasLoadout(0));
        assertEquals(Set.of("a", "b"), state.atlasTreeNodeIds());
        assertEquals(Set.of(), state.atlasTreeNodeIds(1));
        assertEquals(Set.of("c"), state.atlasTreeNodeIds(2));
    }

    @Test
    void onlyThreeAtlasesExistAndSelectingTheActiveOneChangesNothing() {
        AtlasState state = new AtlasState(SEED);

        assertFalse(state.selectAtlasLoadout(0));
        assertFalse(state.selectAtlasLoadout(-1));
        assertFalse(state.selectAtlasLoadout(AtlasState.ATLAS_LOADOUTS));
        assertEquals(0, state.activeAtlasLoadout());
    }

    @Test
    void anOldSavesAllocationBecomesTheFirstAtlas() throws Exception {
        AtlasState state = new AtlasState(SEED);
        setField(state, "atlasTreeNodeIds", new TreeSet<>(Set.of("a", "b")));
        setField(state, "atlasTreeLoadouts", null);
        setField(state, "treeEffectSnapshots", null);
        setField(state, "revealSnapshots", null);
        setField(state, "completionSnapshots", null);

        AtlasState repaired = state.repaired();

        assertEquals(0, repaired.activeAtlasLoadout());
        assertEquals(Set.of("a", "b"), repaired.atlasTreeNodeIds());
        assertEquals(Set.of(), repaired.atlasTreeNodeIds(1));
        assertEquals(Set.of(), repaired.atlasTreeNodeIds(2));
        assertNull(repaired.revealedWith(AtlasCell.ORIGIN));
        assertEquals(Set.of("a", "b"), repaired.repaired().atlasTreeNodeIds());
    }

    @Test
    void atlasEffectsSurviveBeingWrittenDown() {
        AtlasEffects both = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_DENSE_NEBULA_CHANCE_PERCENT, 8f),
                new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 1f)));

        assertEquals(both, AtlasEffects.decode(both.encode()));
        assertEquals(AtlasEffects.NONE, AtlasEffects.decode(AtlasEffects.NONE.encode()));
        assertEquals(AtlasEffects.NONE, AtlasEffects.decode(null));
        assertEquals(AtlasEffects.NONE, AtlasEffects.decode("NOT_AN_EFFECT:1;ATLAS_VISIBILITY_LANES:x"));
    }

    @Test
    void aSystemKeepsTheAnomalyChanceOfTheAtlasItWasRevealedWith() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator plain = generator(state, AtlasEffects.NONE);
        AtlasGenerator nebulous = plain.withEffects(NEBULOUS);
        List<AtlasCell> onlyWithTheTree = AtlasCell.ORIGIN.around(6).stream()
                .filter(cell -> !plain.isVoid(cell) && !cell.equals(AtlasCell.ORIGIN))
                .filter(cell -> hasNebula(nebulous, cell) && !hasNebula(plain, cell)).toList();
        assertTrue(onlyWithTheTree.size() >= 2);
        AtlasCell revealedPlain = onlyWithTheTree.get(0);
        AtlasCell revealedNebulous = onlyWithTheTree.get(1);

        state.recordReveal(plain, List.of(revealedPlain));
        state.recordReveal(nebulous, List.of(revealedNebulous));

        assertFalse(hasNebula(nebulous, revealedPlain));
        assertTrue(hasNebula(plain, revealedNebulous));
        assertEquals(NEBULOUS, state.revealedWith(revealedNebulous));
    }

    @Test
    void aSightedSystemOutsideTheVisibleAtlasKeepsTheChanceItWasFirstShownWith() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator plain = generator(state, AtlasEffects.NONE);
        AtlasGenerator nebulous = plain.withEffects(NEBULOUS);
        AtlasCell sighted = AtlasCell.ORIGIN.around(6).stream()
                .filter(cell -> !plain.isVoid(cell) && !cell.equals(AtlasCell.ORIGIN))
                .filter(cell -> hasNebula(nebulous, cell) && !hasNebula(plain, cell)).findFirst().orElseThrow();

        state.recordSighting(plain, List.of(sighted));
        state.recordReveal(nebulous, List.of(sighted));

        assertFalse(hasNebula(nebulous, sighted));
        assertEquals(AtlasEffects.NONE, state.revealedWith(sighted));
    }

    @Test
    void aCompletedSystemKeepsTheVisibilityOfTheAtlasItWasCompletedWith() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator plain = generator(state, AtlasEffects.NONE);
        AtlasGenerator wider = plain.withEffects(WIDER);
        state.recordReveal(plain, AtlasReach.visible(plain, state));
        AtlasCell neighbour = plain.lanes(AtlasCell.ORIGIN).get(0);

        AtlasProgress.complete(wider, state, neighbour, 1);

        assertEquals(WIDER, state.completedWith(neighbour));
        assertEquals(AtlasReach.VISIBLE_LANES + 2, AtlasReach.visibleLanesAround(plain, state, neighbour));
        assertEquals(AtlasReach.VISIBLE_LANES, AtlasReach.visibleLanesAround(wider, state, AtlasCell.ORIGIN));
        assertEquals(AtlasReach.visible(wider, state), AtlasReach.visible(plain, state));
    }

    @Test
    void aSystemCompletedBeforeAtlasesExistedTakesTheAtlasActiveWhenTheMapIsNextSeen() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator wider = generator(state, WIDER);
        AtlasCell neighbour = wider.lanes(AtlasCell.ORIGIN).get(0);
        state.add(neighbour, AtlasState.COMPLETED);
        assertNull(state.completedWith(neighbour));

        state.recordReveal(wider, AtlasReach.visible(wider, state));

        assertNotNull(state.completedWith(neighbour));
        assertEquals(WIDER, state.completedWith(AtlasCell.ORIGIN));
    }

    private static void setField(AtlasState state, String name, Object value) throws Exception {
        Field field = AtlasState.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(state, value);
    }
}
