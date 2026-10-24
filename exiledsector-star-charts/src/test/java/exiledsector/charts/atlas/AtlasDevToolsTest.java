package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtlasDevToolsTest {

    private static final long SEED = 0xde7001L;

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        List<AtlasSpecial> specials = List.of(
                new AtlasSpecial("sensor", AtlasSpecial.Kind.SENSOR_ARRAY, AtlasSpecial.Placement.CHUNK, 0.5f, 4f, 6f, 1, 1f, "", "", ""),
                new AtlasSpecial("nexus", AtlasSpecial.Kind.INFESTATION, AtlasSpecial.Placement.CHUNK, 0.5f, 8f, 2.5f, 1, 1f, "", "", "remnant"),
                new AtlasSpecial("bastion", AtlasSpecial.Kind.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", "", ""));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.3f, 0f, 0.3f),
                AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, specials, List.of(), List.of());
    }

    private static AtlasGenerator generator(AtlasState state) {
        return new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
    }

    @Test
    void sitesAreListedNearestFirstAndTheNearestOpenOneSkipsCompletedSites() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        List<AtlasDevTools.Site> sites = AtlasDevTools.sites(generator, AtlasDevTools.SITE_SEARCH_RADIUS);
        for (int index = 1; index < sites.size(); index++) {
            assertTrue(sites.get(index - 1).cell().centreDistance() <= sites.get(index).cell().centreDistance());
        }
        AtlasCell nearest = AtlasDevTools.nearestOpenSite(generator, state, "NEXUS");
        assertNotNull(nearest);
        assertEquals("nexus", generator.specialAt(nearest).id());

        state.add(nearest, AtlasState.COMPLETED);

        assertNotEquals(nearest, AtlasDevTools.nearestOpenSite(generator, state, "nexus"));
        assertNull(AtlasDevTools.nearestOpenSite(generator, state, "nothing"));
    }

    @Test
    void reachingABastionCompletesALanePathAvoidingOtherSitesAndLeavesTheBastionReachable() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell bastion = AtlasDevTools.nearestOpenSite(generator, state, "bastion");

        List<AtlasCell> path = AtlasDevTools.reach(generator, state, bastion);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertTrue(AtlasReach.isReachable(generator, state, bastion));
        assertFalse(state.isCompleted(bastion));
        AtlasCell previous = AtlasCell.ORIGIN;
        for (AtlasCell step : path) {
            assertNull(generator.specialAt(step), step.toString());
            assertTrue(generator.lanes(previous).contains(step), step.toString());
            assertTrue(state.isCompleted(step));
            previous = step;
        }
        assertTrue(generator.lanes(previous).contains(bastion));
        assertEquals(0, state.atlasPoints());
        assertEquals(List.of(), AtlasDevTools.reach(generator, state, bastion));
    }

    @Test
    void completingANexusGrantsItsRewardsAndFailingASystemSpendsItsMarkers() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell nexus = AtlasDevTools.nearestOpenSite(generator, state, "nexus");

        AtlasDevTools.complete(generator, state, nexus, 4);

        assertTrue(state.isCompleted(nexus));
        assertEquals(2, state.atlasPoints());
        assertEquals(List.of(4), List.copyOf(state.clearedTiers()));

        AtlasCell other = generator.lanes(AtlasCell.ORIGIN).get(0);
        AtlasDevTools.fail(state, other);

        assertTrue(state.has(other, AtlasState.MARKERS_SPENT));
        assertFalse(state.isCompleted(other));
    }

    @Test
    void aVoidCellHasNoPath() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell empty = AtlasCell.ORIGIN.around(6).stream().filter(generator::isVoid).findFirst().orElseThrow();

        assertNull(AtlasDevTools.pathTo(generator, state, empty));
    }

    @Test
    void resettingReplacesTheStateWithTheGivenSeed() {
        SectorAPI sector = mock(SectorAPI.class);
        Map<String, Object> data = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(data);
        AtlasState old = AtlasState.get(sector);
        old.addAtlasPoints(3);

        AtlasState fresh = AtlasDevTools.reset(sector, 77L);

        assertSame(fresh, AtlasState.get(sector));
        assertEquals(77L, fresh.seed());
        assertEquals(0, fresh.atlasPoints());
        assertNotEquals(old, AtlasDevTools.reset(sector, null));
    }
}
