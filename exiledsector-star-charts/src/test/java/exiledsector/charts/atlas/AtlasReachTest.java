package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtlasReachTest {

    private static final long SEED = 0xa71a5L;
    private static final String THREAT = "$encounteredThreat";

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(
                new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of()),
                new AtlasRegion("graveyard", 10f, 0, "", "", Map.of(), Map.of(), Map.of()),
                new AtlasRegion("expanse", 10f, 0, "", THREAT, Map.of(), Map.of(), Map.of()));
        List<AtlasSpecial> specials = List.of(
                new AtlasSpecial("sensor", AtlasSpecial.Kind.SENSOR_ARRAY, AtlasSpecial.Placement.CHUNK, 1f, 1f, 6f, 1, 1f, "", "", ""),
                new AtlasSpecial("threat_nexus", AtlasSpecial.Kind.INFESTATION, AtlasSpecial.Placement.CHUNK, 1f, 1f, 2.5f, 1, 1f, "", THREAT, ""),
                new AtlasSpecial("bastion", AtlasSpecial.Kind.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", "", ""));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.5f, 0f, 0.5f),
                AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, specials, List.of());
    }

    private static AtlasGenerator generator(AtlasState state, Set<String> flags) {
        return new AtlasGenerator(SEED, rules(), new AtlasUnlocks(modId -> false, flags), state);
    }

    private static Map<AtlasCell, Integer> lanesFrom(AtlasGenerator generator, Set<AtlasCell> sources, int limit) {
        Map<AtlasCell, Integer> steps = new HashMap<>();
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>(sources);
        sources.forEach(source -> steps.put(source, 0));
        while (!frontier.isEmpty()) {
            AtlasCell cell = frontier.poll();
            if (steps.get(cell) >= limit) {
                continue;
            }
            for (AtlasCell next : generator.lanes(cell)) {
                if (!steps.containsKey(next)) {
                    steps.put(next, steps.get(cell) + 1);
                    frontier.add(next);
                }
            }
        }
        return steps;
    }

    @Test
    void theOriginsLaneNeighboursAreReachableAndCompletingOneOpensItsOwn() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Set.of());

        Set<AtlasCell> reachable = AtlasReach.reachable(generator, state);
        assertEquals(new HashSet<>(generator.lanes(AtlasCell.ORIGIN)), reachable);

        AtlasCell next = reachable.iterator().next();
        state.add(next, AtlasState.COMPLETED);
        Set<AtlasCell> after = AtlasReach.reachable(generator, state);

        assertFalse(after.contains(next));
        assertTrue(after.containsAll(generator.lanes(next).stream().filter(cell -> !state.isCompleted(cell)).toList()));
    }

    @Test
    void visibilityReachesTwoLanesAndACompletedSensorArrayRevealsSix() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Set.of());

        assertEquals(lanesFrom(generator, Set.of(AtlasCell.ORIGIN), AtlasReach.VISIBLE_LANES).keySet(), AtlasReach.visible(generator, state));

        AtlasCell sensor = null;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(12)) {
            AtlasSpecial special = generator.specialAt(cell);
            if (special != null && special.id().equals("sensor")) {
                sensor = cell;
                break;
            }
        }
        state.add(sensor, AtlasState.COMPLETED);
        Set<AtlasCell> visible = AtlasReach.visible(generator, state);

        assertTrue(visible.containsAll(lanesFrom(generator, Set.of(sensor), 6).keySet()));
    }

    @Test
    void aBastionAllowsOneAttempt() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state, Set.of());
        AtlasCell bastion = AtlasCell.nearest(0f, 25f);
        AtlasCell neighbour = generator.lanes(bastion).get(0);
        state.add(neighbour, AtlasState.COMPLETED);

        assertTrue(AtlasReach.isReachable(generator, state, bastion));
        state.add(bastion, AtlasState.ATTEMPTED);
        assertFalse(AtlasReach.isReachable(generator, state, bastion));
    }

    @Test
    void revealedBiomesAndThreatChunksKeepWhatTheyWereWhenFirstSeen() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator locked = generator(state, Set.of());
        Set<AtlasCell> seenEarly = new HashSet<>(AtlasCell.ORIGIN.around(10));
        state.recordReveal(locked, seenEarly);
        AtlasGenerator unlocked = generator(state, Set.of(THREAT));
        AtlasGenerator fresh = generator(new AtlasState(SEED), Set.of(THREAT));

        for (AtlasCell cell : seenEarly) {
            assertEquals(locked.node(cell).regionId(), unlocked.node(cell).regionId(), cell.toString());
            assertNotEquals("expanse", unlocked.node(cell).regionId());
            AtlasSpecial early = unlocked.specialAt(cell);
            assertTrue(early == null || !early.id().equals("threat_nexus"), cell.toString());
            assertNotEquals("threat_nexus", unlocked.node(cell).infestationId(), cell.toString());
        }
        boolean threatLater = false;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(40)) {
            AtlasSpecial special = fresh.specialAt(cell);
            threatLater |= special != null && special.id().equals("threat_nexus");
        }
        assertTrue(threatLater);
        assertTrue(state.recordedSites() > 0);
    }

    @Test
    void theStateIsCreatedOncePerSaveAndTiersGiveOnePointEach() {
        SectorAPI sector = mock(SectorAPI.class);
        Map<String, Object> data = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(data);

        AtlasState state = AtlasState.get(sector);

        assertSame(state, AtlasState.get(sector));
        assertSame(state, AtlasState.getIfCreated(sector));
        assertTrue(state.clearTier(3));
        assertFalse(state.clearTier(3));
        assertTrue(state.clearTier(4));
        assertEquals(2, state.atlasPoints());
        assertTrue(state.isCompleted(AtlasCell.ORIGIN));
    }
}
