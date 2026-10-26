package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasAmbience;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDecoration;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMarkerRule;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.LaneFleetTraffic;
import exiledsector.ui.node.NodeMetrics;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasSceneryTest {

    private static final long SEED = 0x5ce4eL;
    private static final Map<String, AtlasDecoration> DECORATIONS = Map.of(
            "probe", new AtlasDecoration("probe", List.of("graphics/stations/survey_probe.png"), "", 1, 1, 0.05f, 0.2f, 1, 0f, 4f, 0.02f, null, null, 0.9f,
                    0f, 0f, List.of("debris")),
            "debris", new AtlasDecoration("debris", List.of("graphics/debris/debris_sheet0.png"), "graphics/debris/debris_sheet0_glow.png", 4, 4,
                    0.012f, 0.5f, 12, 0.1f, 25f, 0.008f, null, null, 1f, 0f, 0f, List.of()));
    private static final AtlasAmbience GRAVEYARD = new AtlasAmbience(0f, null, Map.of("probe", 1f, "debris", 2f), 2f);

    private static AtlasMapCanvas canvas() {
        AtlasRules rules = new AtlasRules(List.of(new AtlasRegion("graveyard", 1f, 0, "", "", Map.of(), Map.of(), Map.of())), Map.of("compact", 1f),
                Map.of("pirates", 1f), new AtlasMarkerRule(0.5f, 0f, 0.5f), AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER,
                List.of(), List.of());
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
        AtlasScenery scenery = new AtlasScenery(SEED, id -> "graveyard".equals(id) ? GRAVEYARD : AtlasAmbience.BARE, DECORATIONS::get);
        return new AtlasMapCanvas(generator, state, scenery);
    }

    @Test
    void decorationsScatterThroughKnownSpaceClearOfEverySystemAndStayPut() {
        AtlasMapCanvas canvas = canvas();
        AtlasMapCanvas again = canvas();
        float clearance = AtlasDecorations.CLEARANCE_CELLS * AtlasMapCanvas.CELL_PIXELS;
        int pieces = 0;
        Map<String, Integer> kinds = new HashMap<>();
        for (AtlasCell visible : canvas.visibleCells()) {
            for (AtlasCell cell : visible.around(1)) {
                assertTrue(canvas.scenery().isKnown(cell));
                List<AtlasDecorations.Piece> placed = canvas.scenery().decorations().piecesIn(cell);
                assertEquals(placed, again.scenery().decorations().piecesIn(cell), cell.toString());
                assertWreckageSurroundsEachProbe(placed);
                for (AtlasDecorations.Piece piece : placed) {
                    pieces++;
                    kinds.merge(piece.decoration().id(), 1, Integer::sum);
                    float spread = piece.decoration().spread() * AtlasMapCanvas.CELL_PIXELS;
                    for (AtlasCell near : cell.around(1)) {
                        if (!canvas.generator().isVoid(near)) {
                            double gap = Math.hypot(canvas.displayX(near) - piece.x(), canvas.displayY(near) - piece.y());
                            assertTrue(gap >= clearance - spread - 1f, piece + " sits " + gap + " from " + near);
                        }
                    }
                }
            }
        }

        assertTrue(pieces > 0);
        assertTrue(kinds.containsKey("probe") && kinds.containsKey("debris"), kinds.toString());
        assertFalse(canvas.scenery().isKnown(new AtlasCell(400, 400)));
    }

    private static void assertWreckageSurroundsEachProbe(List<AtlasDecorations.Piece> placed) {
        AtlasDecoration debris = DECORATIONS.get("debris");
        float reach = debris.spread() * AtlasMapCanvas.CELL_PIXELS + 1f;
        for (int index = 0; index < placed.size(); index++) {
            AtlasDecorations.Piece probe = placed.get(index);
            if (!"probe".equals(probe.decoration().id())) {
                continue;
            }
            assertTrue(index + debris.count() < placed.size(), "no wreckage after " + probe);
            for (AtlasDecorations.Piece piece : placed.subList(index + 1, index + 1 + debris.count())) {
                assertEquals("debris", piece.decoration().id());
                assertTrue(Math.hypot(piece.x() - probe.x(), piece.y() - probe.y()) <= reach, piece + " strays from " + probe);
            }
        }
    }

    @Test
    void glowsPulseAndPingsRingOutOnTheirBeat() {
        assertEquals(1f, AtlasDecorations.pulseLevel(0f, 0.3f, 12f));
        assertEquals(0f, AtlasDecorations.pulseLevel(1.5f, 0f, 0f), 0.001f);
        assertEquals(1f, AtlasDecorations.pulseLevel(1.5f, 0f, 1f / 3f), 0.001f);
        assertEquals(0.5f, AtlasDecorations.pulseLevel(1.5f, 0.25f, 0f), 0.001f);
        assertEquals(-1f, AtlasDecorations.pingProgress(0f, 0f, 3f));
        assertEquals(0.5f, AtlasDecorations.pingProgress(6f, 0f, 6.5f), 0.001f);
        assertEquals(-1f, AtlasDecorations.pingProgress(6f, 0f, 3f));
        assertEquals(0.25f, AtlasDecorations.pingProgress(6f, 0.5f, 3.25f), 0.001f);
    }

    @Test
    void factionTerritoriesCoverEveryKnownCellWithItsFaction() {
        AtlasMapCanvas canvas = canvas();
        Set<Long> known = new HashSet<>();
        for (AtlasCell visible : canvas.visibleCells()) {
            for (AtlasCell cell : visible.around(1)) {
                known.add(cell.pack());
                assertEquals(canvas.generator().factionAt(cell), canvas.scenery().territoryAt(cell), cell.toString());
            }
        }
        int territoryCells = 0;
        for (AtlasTerritoryBorders.Region territory : canvas.scenery().borders().regions()) {
            territoryCells += territory.cells();
            assertEquals("pirates", territory.groupId());
        }

        assertEquals(known.size(), territoryCells);
    }

    @Test
    void biomesOutlineKnownBiomeCellsButNeverOpenSpace() {
        AtlasMapCanvas canvas = canvas();
        Set<Long> biomeCells = new HashSet<>();
        for (AtlasCell visible : canvas.visibleCells()) {
            for (AtlasCell cell : visible.around(1)) {
                String regionId = canvas.scenery().regionAt(cell);
                assertEquals(AtlasRegion.NORMAL.equals(regionId) ? null : regionId, canvas.scenery().biomeAt(cell), cell.toString());
                if (canvas.scenery().biomeAt(cell) != null) {
                    biomeCells.add(cell.pack());
                }
            }
        }
        int coveredCells = 0;
        for (AtlasTerritoryBorders.Region biome : canvas.scenery().biomes().regions()) {
            coveredCells += biome.cells();
            assertEquals("graveyard", biome.groupId());
        }

        assertEquals(null, canvas.scenery().biomeAt(AtlasCell.ORIGIN));
        assertTrue(coveredCells <= biomeCells.size());
    }

    @Test
    void openSpaceStaysBare() {
        AtlasMapCanvas canvas = canvas();
        AtlasScenery bare = new AtlasScenery(SEED, id -> AtlasAmbience.BARE, DECORATIONS::get);
        AtlasMapCanvas plain = new AtlasMapCanvas(canvas.generator(), canvas.state(), bare);
        for (AtlasCell cell : plain.visibleCells()) {
            assertTrue(plain.scenery().decorations().piecesIn(cell).isEmpty());
        }
    }

    @Test
    void weightedPicksFollowTheirWeights() {
        Random random = new Random(2L);
        Map<String, Integer> picks = new HashMap<>();
        for (int roll = 0; roll < 4000; roll++) {
            picks.merge(AtlasDecorations.pick(new java.util.TreeMap<>(Map.of("a", 1f, "b", 3f, "c", 0f)), random), 1, Integer::sum);
        }

        assertFalse(picks.containsKey("c"));
        assertTrue(picks.get("b") > picks.get("a") * 2.5f && picks.get("b") < picks.get("a") * 3.5f, picks.toString());
    }

    @Test
    void everyVisibleSystemButTheGatewayIsAPortWithItsVisibleLanes() {
        AtlasMapCanvas canvas = canvas();
        List<LaneFleetTraffic.Port> ports = AtlasFleetTraffic.ports(canvas);
        long systems = canvas.visibleCells().stream().filter(cell -> !canvas.generator().isVoid(cell) && !cell.equals(AtlasCell.ORIGIN)).count();

        assertEquals(systems, ports.size());
        for (LaneFleetTraffic.Port port : ports) {
            assertTrue(port.innerRadius() > NodeMetrics.donutRadius(NodeMetrics.NODE_SIZE * AtlasMapCanvas.NODE_SCALE));
            assertTrue(port.outerRadius() > port.innerRadius());
            for (long neighbour : port.neighbours()) {
                assertTrue(canvas.visibleCells().contains(AtlasCell.unpack(neighbour)));
            }
        }
    }

    @Test
    void fleetsDrawnAtDoubleSizeStillCircleTheirSystems() {
        List<LaneFleetTraffic.Port> ports = AtlasFleetTraffic.ports(canvas());
        List<LaneFleetTraffic.Port> scaled = AtlasFleetTraffic.fleetSpace(ports);
        TreeViewport view = new TreeViewport(640f, 360f, 0.25f);
        TreeViewport fleetView = new TreeViewport(640f, 360f, 0.25f * AtlasFleetTraffic.FLEET_SCALE);

        assertEquals(2f, AtlasFleetTraffic.FLEET_SCALE);
        for (int index = 0; index < ports.size(); index++) {
            LaneFleetTraffic.Port port = ports.get(index);
            LaneFleetTraffic.Port fleetPort = scaled.get(index);
            assertEquals(view.screenX(port.x()), fleetView.screenX(fleetPort.x()), 0.01f);
            assertEquals(view.screenY(port.y()), fleetView.screenY(fleetPort.y()), 0.01f);
            assertEquals(port.outerRadius() * view.zoom(), fleetPort.outerRadius() * fleetView.zoom(), 0.01f);
            assertEquals(port.neighbours(), fleetPort.neighbours());
        }
    }
}
