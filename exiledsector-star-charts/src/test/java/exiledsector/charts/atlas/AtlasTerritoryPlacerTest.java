package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRanks;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTerritoryPlacerTest {

    private static final long SEED = 0x7e221L;
    private static final int GROWTH_STEPS = 8;
    private static final int COMPLETIONS_PER_STEP = 2;

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(
                new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of()),
                new AtlasRegion("badlands", 4f, 0, "", "", Map.of(), Map.of("luddic_path", 2f, "pirates", 1f), Map.of()));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 5f, "hegemony", 3f, "tritachyon", 2f, "independent", 2f),
                new AtlasAnomalyChance(0.5f, 0f, 0.5f), AtlasAnomalyChance.NEVER, List.of(), List.of());
    }

    private static AtlasGenerator generator(AtlasState state) {
        return new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
    }

    private static void grow(AtlasGenerator generator, AtlasState state, TerritoryContext context, Consumer<Set<AtlasCell>> afterEachReveal) {
        for (int step = 0; step < GROWTH_STEPS; step++) {
            afterEachReveal.accept(AtlasReach.refreshReveal(generator, state, context));
            List<AtlasCell> reachable = new ArrayList<>(AtlasReach.reachable(generator, state));
            reachable.sort(Comparator.comparingLong(AtlasCell::pack));
            reachable.stream().limit(COMPLETIONS_PER_STEP).forEach(cell -> AtlasProgress.complete(generator, state, cell, 1));
        }
    }

    private static Set<AtlasCell> withinLanes(AtlasGenerator generator, AtlasCell start, int lanes) {
        Map<AtlasCell, Integer> distances = new HashMap<>();
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>(List.of(start));
        distances.put(start, 0);
        while (!frontier.isEmpty()) {
            AtlasCell cell = frontier.poll();
            if (distances.get(cell) >= lanes) {
                continue;
            }
            for (AtlasCell neighbour : generator.lanes(cell)) {
                if (distances.putIfAbsent(neighbour, distances.get(cell) + 1) == null) {
                    frontier.add(neighbour);
                }
            }
        }
        return distances.keySet();
    }

    private static Set<AtlasCell> connectedThroughOwnTerritory(AtlasGenerator generator, AtlasState state, AtlasCell centre) {
        Set<AtlasCell> reached = new HashSet<>(List.of(centre));
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>(List.of(centre));
        while (!frontier.isEmpty()) {
            for (AtlasCell neighbour : generator.lanes(frontier.poll())) {
                if (centre.equals(state.commandCentreOf(neighbour)) && reached.add(neighbour)) {
                    frontier.add(neighbour);
                }
            }
        }
        return reached;
    }

    private static List<AtlasCell> centres(AtlasState state) {
        return state.officers().stream().filter(AtlasOfficer::isActive).map(AtlasOfficer::commandCentre).toList();
    }

    @Test
    void theFirstRevealDealsTheFullBoardOnceAndPlacesTerritories() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        AtlasReach.refreshReveal(generator, state, TerritoryContext.NONE);
        List<Integer> recordIds = state.officers().stream().map(AtlasOfficer::recordId).toList();
        AtlasReach.refreshReveal(generator, state, TerritoryContext.NONE);

        assertEquals(45, state.officers().size());
        assertEquals(recordIds, state.officers().stream().map(AtlasOfficer::recordId).toList());
        assertFalse(centres(state).isEmpty());
    }

    @Test
    void theSameSeedAndProgressPlaceTheSameTerritories() {
        AtlasState first = new AtlasState(SEED);
        AtlasState second = new AtlasState(SEED);

        grow(generator(first), first, TerritoryContext.NONE, visible -> { });
        grow(generator(second), second, TerritoryContext.NONE, visible -> { });

        assertEquals(first.claims(), second.claims());
        assertEquals(first.officers(), second.officers());
    }

    @Test
    void everyClaimedSystemBelongsToACommandingOfficerAndConnectsToItsCentre() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        grow(generator, state, TerritoryContext.NONE, visible -> {
            for (AtlasCell centre : new HashSet<>(state.claims().values())) {
                AtlasOfficer officer = state.officerAt(centre);
                assertNotNull(officer, "no officer commands " + centre);
                assertEquals(centre, officer.commandCentre());
                Set<AtlasCell> territory = state.claimsOf(centre);
                assertTrue(territory.size() <= AtlasTerritoryPlacer.MAX_STARTING_CLAIM, centre + " holds " + territory.size());
                assertEquals(territory, connectedThroughOwnTerritory(generator, state, centre));
                territory.forEach(cell -> assertFalse(generator.isVoid(cell) || state.isCompleted(cell), cell.toString()));
            }
        });
    }

    @Test
    void commandCentresStayAtLeastTheSpacingApart() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        grow(generator, state, TerritoryContext.NONE, visible -> {
            Set<AtlasCell> centres = new HashSet<>(centres(state));
            for (AtlasCell centre : centres) {
                for (AtlasCell near : withinLanes(generator, centre, AtlasTerritoryPlacer.CENTRE_SPACING_LANES - 1)) {
                    assertTrue(near.equals(centre) || !centres.contains(near), centre + " is too close to " + near);
                }
            }
        });
    }

    @Test
    void aRevealPlacesOnlyAFewTerritoriesOfEachFactionInView() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Set<AtlasCell> visible = AtlasReach.refreshReveal(generator, state, TerritoryContext.NONE);

        Map<String, Set<AtlasCell>> visibleCentres = new HashMap<>();
        for (AtlasCell cell : visible) {
            AtlasCell centre = state.commandCentreOf(cell);
            if (centre != null) {
                visibleCentres.computeIfAbsent(state.claimedFaction(cell), factionId -> new HashSet<>()).add(centre);
            }
        }
        assertFalse(visibleCentres.isEmpty());
        visibleCentres.forEach((factionId, centres) ->
                assertTrue(centres.size() <= AtlasTerritoryPlacer.MAX_VISIBLE_TERRITORIES, factionId + " shows " + centres.size()));
    }

    @Test
    void officersComeFromTheTerritoryFactionsOfTheirCentresRegion() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        grow(generator, state, TerritoryContext.NONE, visible -> { });

        for (AtlasOfficer officer : state.officers()) {
            if (officer.isActive()) {
                assertTrue(generator.territoryWeights(officer.commandCentre()).containsKey(officer.factionId()),
                        officer.factionId() + " at " + officer.commandCentre());
                assertFalse(Set.of("independent", "mercenary").contains(officer.factionId()));
            }
        }
    }

    @Test
    void officersTakeCommandLowestRankFirst() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        grow(generator, state, TerritoryContext.NONE, visible -> {
            for (AtlasOfficer placed : state.officers()) {
                if (placed.neverPlaced()) {
                    continue;
                }
                int placedOrder = OfficerRanks.order(placed.rankId());
                for (AtlasOfficer waiting : state.officers()) {
                    assertFalse(waiting.neverPlaced() && waiting.factionId().equals(placed.factionId())
                                    && OfficerRanks.order(waiting.rankId()) < placedOrder,
                            waiting.rankId() + " of " + waiting.factionId() + " still waits behind a " + placed.rankId());
                }
            }
        });
        assertTrue(state.officers().stream().anyMatch(officer -> !officer.neverPlaced()));
    }

    @Test
    void theCommissionFactionTakesNoNewCommandCentres() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);

        grow(generator, state, new TerritoryContext("pirates", null, FlagshipCatalogue.NONE), visible -> { });

        assertFalse(centres(state).isEmpty());
        assertTrue(state.officers().stream().filter(officer -> "pirates".equals(officer.factionId())).allMatch(AtlasOfficer::neverPlaced));
    }

    @Test
    void aCompletedCommandCentresOfficerFleesAndItsTerritoryBreaksUp() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasReach.refreshReveal(generator, state, TerritoryContext.NONE);
        AtlasCell centre = centres(state).get(0);
        int recordId = state.officerAt(centre).recordId();

        AtlasProgress.complete(generator, state, centre, 1);

        assertTrue(state.officer(recordId).hasFled());
        assertTrue(state.claimsOf(centre).isEmpty());
        assertFalse(state.isClaimed(centre));
    }
}
