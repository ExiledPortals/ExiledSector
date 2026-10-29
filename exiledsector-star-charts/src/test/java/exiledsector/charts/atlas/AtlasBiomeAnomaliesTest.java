package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.station.DomainMothership;
import exiledsector.charts.anomaly.station.RemnantNexusStation;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasBiomeAnomaliesTest {

    private static final long SEED = 0xb10e5L;
    private static final int SCAN_RADIUS = 36;
    private static final String EXPANSE = "remnant_expanse";
    private static final String GRAVEYARD = "derelict_graveyard";
    private static final String NEXUS = RemnantNexusStation.ID;
    private static final String MOTHERSHIP = DomainMothership.ID;
    private static final AtlasMarkerRule ALWAYS = new AtlasMarkerRule(1f, 0f, 1f);

    private static final List<AtlasRegion> REGIONS = List.of(
            new AtlasRegion(AtlasRegion.NORMAL, 6f, 0, "", "", Map.of(), Map.of("pirates", 1f), Map.of()),
            new AtlasRegion(GRAVEYARD, 6f, 0, "", "", Map.of(), Map.of("derelict", 1f), Map.of()),
            new AtlasRegion(EXPANSE, 6f, 0, "", "", Map.of(), Map.of("remnant", 1f), Map.of()));

    private static final List<AtlasSpecial> SPECIALS = List.of(
            new AtlasSpecial("sensor", AtlasSpecial.Kind.SENSOR_ARRAY, AtlasSpecial.Placement.CHUNK, 0.5f, 4f, 6f, 1, 1f, "", "", ""),
            new AtlasSpecial("nexus", AtlasSpecial.Kind.INFESTATION, AtlasSpecial.Placement.CHUNK, 0.3f, 6f, 2.5f, 1, 1f, "", "", ""));

    private static AtlasAnomalyCandidate perBiome(String id, String biome) {
        return new AtlasAnomalyCandidate(id, 5f, "", false, Set.of(), null, ALWAYS, Set.of(biome));
    }

    private static AtlasRules rules(List<AtlasAnomalyCandidate> anomalies) {
        return new AtlasRules(REGIONS, Map.of("compact", 1f), Map.of("pirates", 1f, "derelict", 1f, "remnant", 1f),
                new AtlasMarkerRule(0.3f, 0f, 0.3f), ALWAYS, SPECIALS, anomalies);
    }

    private static List<AtlasAnomalyCandidate> stations() {
        return List.of(perBiome(NEXUS, EXPANSE), perBiome(MOTHERSHIP, GRAVEYARD));
    }

    private static AtlasGenerator generator(List<AtlasAnomalyCandidate> anomalies) {
        return new AtlasGenerator(SEED, rules(anomalies), AtlasUnlocks.NONE, AtlasMemory.NONE);
    }

    private static List<AtlasBiome> pockets(AtlasGenerator generator, int radius) {
        List<AtlasBiome> pockets = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (AtlasCell cell : AtlasCell.ORIGIN.around(radius)) {
            AtlasBiome biome = generator.biomeOf(cell);
            if (!biome.isNormal() && seen.add(biome.siteKey())) {
                pockets.add(biome);
            }
        }
        return pockets;
    }

    private static List<AtlasCell> cellsOf(AtlasGenerator generator, AtlasBiome pocket) {
        List<AtlasCell> cells = new ArrayList<>();
        int centreX = Math.round(pocket.centreX());
        int centreY = Math.round(pocket.centreY());
        int window = AtlasBiomeAnomalies.POCKET_WINDOW + 2;
        for (int gridX = centreX - window; gridX <= centreX + window; gridX++) {
            for (int gridY = centreY - window; gridY <= centreY + window; gridY++) {
                AtlasCell cell = new AtlasCell(gridX, gridY);
                if (generator.biomeOf(cell).siteKey() == pocket.siteKey()) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    private static boolean canHold(AtlasGenerator generator, AtlasCell cell) {
        return !cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && generator.specialAt(cell) == null && generator.infestationAt(cell) == null;
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
    }

    @Test
    void exactlyOneSystemInEachPocketHoldsItsBiomesStationEvenWhenTheChanceRollWouldAlwaysPass() {
        AtlasGenerator generator = generator(stations());
        int pocketsChecked = 0;
        for (AtlasBiome pocket : pockets(generator, SCAN_RADIUS - AtlasBiomeAnomalies.POCKET_WINDOW)) {
            String wanted = EXPANSE.equals(pocket.regionId()) ? NEXUS : MOTHERSHIP;
            String other = wanted.equals(NEXUS) ? MOTHERSHIP : NEXUS;
            List<AtlasCell> holders = new ArrayList<>();
            int eligible = 0;
            for (AtlasCell cell : cellsOf(generator, pocket)) {
                List<String> ids = generator.node(cell).anomalyIds();
                if (ids.contains(wanted)) {
                    holders.add(cell);
                    assertEquals(wanted, ids.get(0), cell.toString());
                }
                assertFalse(ids.contains(other), cell.toString());
                eligible += canHold(generator, cell) ? 1 : 0;
            }

            assertEquals(eligible > 0 ? 1 : 0, holders.size(), pocket.toString());
            if (!holders.isEmpty()) {
                assertEquals(holders.get(0), generator.biomeAnomalyCell(pocket, wanted));
                assertTrue(canHold(generator, holders.get(0)));
                pocketsChecked++;
            }
        }
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS)) {
            if (generator.biomeOf(cell).isNormal()) {
                assertFalse(generator.node(cell).anomalyIds().contains(NEXUS), cell.toString());
                assertFalse(generator.node(cell).anomalyIds().contains(MOTHERSHIP), cell.toString());
            }
        }

        assertTrue(pocketsChecked >= 6, "pockets " + pocketsChecked);
    }

    @Test
    void theSameAtlasAlwaysPicksTheSameSystemWhateverIsAskedFirst() {
        AtlasGenerator first = generator(stations());
        AtlasGenerator second = generator(stations());
        List<AtlasBiome> pockets = pockets(first, 20);
        List<AtlasCell> reversed = new ArrayList<>(AtlasCell.ORIGIN.around(20));
        Collections.reverse(reversed);
        reversed.forEach(second::node);

        for (AtlasBiome pocket : pockets) {
            String wanted = EXPANSE.equals(pocket.regionId()) ? NEXUS : MOTHERSHIP;
            assertEquals(first.biomeAnomalyCell(pocket, wanted), second.biomeAnomalyCell(pocket, wanted), pocket.toString());
        }
        assertFalse(pockets.isEmpty());
    }

    @Test
    void addingTheStationsMovesNoRolledAnomalyExceptOnesThatWouldConflict() {
        AtlasAnomalyCandidate breach = new AtlasAnomalyCandidate("breach", 1f, "", false, Set.of(), null, new AtlasMarkerRule(0.4f, 0f, 0.4f));
        AtlasAnomalyCandidate hunter = new AtlasAnomalyCandidate("hunter", 1f, "", false, Set.of(NEXUS), null, ALWAYS);
        AtlasGenerator without = generator(List.of(breach, hunter));
        List<AtlasAnomalyCandidate> withStations = new ArrayList<>(stations());
        withStations.add(breach);
        withStations.add(hunter);
        AtlasGenerator with = generator(withStations);
        int nexusSystems = 0;

        for (AtlasCell cell : AtlasCell.ORIGIN.around(24)) {
            List<String> before = without.node(cell).anomalyIds();
            List<String> after = new ArrayList<>(with.node(cell).anomalyIds());
            boolean nexus = after.remove(NEXUS);
            after.remove(MOTHERSHIP);
            List<String> expected = new ArrayList<>(before);
            if (nexus) {
                nexusSystems++;
                expected.remove("hunter");
            }
            assertEquals(expected, after, cell.toString());
        }

        assertTrue(nexusSystems > 0);
    }

    @Test
    void aStationStaysWhenAFailedRunSpendsTheSystemAndCanNeverBeInserted() throws Exception {
        ChartTables.loadAnomalies();
        AtlasGenerator generator = generator(stations());
        AtlasBiome pocket = pockets(generator, 20).stream().filter(biome -> EXPANSE.equals(biome.regionId())).findFirst().orElseThrow();
        AtlasCell holder = generator.biomeAnomalyCell(pocket, NEXUS);
        AtlasState state = new AtlasState(SEED);
        state.add(holder, AtlasState.MARKERS_SPENT);

        assertEquals(List.of(NEXUS), AtlasAnomalies.effective(generator, state, holder));
        for (AtlasCell cell : AtlasCell.ORIGIN.around(10)) {
            assertEquals(AtlasAnomalies.InsertCheck.NOT_INSERTABLE, AtlasAnomalies.canInsert(generator, state, cell, NEXUS, null));
            assertEquals(AtlasAnomalies.InsertCheck.NOT_INSERTABLE, AtlasAnomalies.canInsert(generator, state, cell, MOTHERSHIP, null));
        }
    }

    @Test
    void theStationCountsTowardTheSystemLimitButIsNeverCrowdedOut() throws Exception {
        ChartTables.loadAnomalies();
        AtlasGenerator generator = generator(stations());
        AtlasBiome pocket = pockets(generator, 20).stream().filter(biome -> GRAVEYARD.equals(biome.regionId())).findFirst().orElseThrow();
        AtlasCell holder = generator.biomeAnomalyCell(pocket, MOTHERSHIP);
        AtlasState state = new AtlasState(SEED);
        int inserted = 0;

        for (ChartAnomalies.Definition definition : ChartAnomalies.definitions()) {
            if (AtlasAnomalies.insert(generator, state, holder, definition.id(), AtlasAnomalyRecord.Source.WORLD, null).check().allows()) {
                inserted++;
            }
        }
        List<String> present = AtlasAnomalies.effective(generator, state, holder);

        assertEquals(AtlasAnomalies.MAX_PER_SYSTEM, present.size());
        assertEquals(MOTHERSHIP, present.get(0));
        assertEquals(AtlasAnomalies.MAX_PER_SYSTEM - 1, inserted);
        String absent = ChartAnomalies.definitions().stream().filter(ChartAnomalies.Definition::insertable).map(ChartAnomalies.Definition::id)
                .filter(id -> !present.contains(id)).findFirst().orElseThrow();
        assertEquals(AtlasAnomalies.InsertCheck.FULL, AtlasAnomalies.canInsert(generator, state, holder, absent, null));
    }

    @Test
    void everyShippedBiomesEntryNamesAnAtlasRegion() throws Exception {
        ChartTables.loadAnomalies();
        JSONArray rows = ChartTables.csvRows(AtlasData.REGIONS_PATH);
        Set<String> regionIds = new HashSet<>();
        for (int index = 0; index < rows.length(); index++) {
            regionIds.add(rows.getJSONObject(index).getString("id"));
        }

        assertEquals(Set.of(EXPANSE), ChartAnomalies.definition(NEXUS).biomes());
        assertEquals(Set.of(GRAVEYARD), ChartAnomalies.definition(MOTHERSHIP).biomes());
        for (ChartAnomalies.Definition definition : ChartAnomalies.definitions()) {
            assertTrue(regionIds.containsAll(definition.biomes()), definition.id());
        }
    }

    @Test
    void aBiomeWithoutAStationListedGetsNone() {
        AtlasGenerator generator = generator(List.of(perBiome(NEXUS, EXPANSE)));

        for (AtlasBiome pocket : pockets(generator, 20)) {
            if (GRAVEYARD.equals(pocket.regionId())) {
                assertNull(generator.biomeAnomalyCell(pocket, MOTHERSHIP));
                assertNull(generator.biomeAnomalyCell(pocket, NEXUS));
            }
        }
    }
}
