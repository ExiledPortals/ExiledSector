package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasTerritoryBridgesTest {

    private static final AtlasCell WEST = new AtlasCell(0, 0);
    private static final AtlasCell EAST = new AtlasCell(2, 1);
    private static final AtlasCell FAR = new AtlasCell(3, 3);
    private static final AtlasCell DIAGONAL = new AtlasCell(1, 1);
    private static final AtlasCell SIDE_EAST = new AtlasCell(1, 0);
    private static final AtlasCell SIDE_NORTH = new AtlasCell(0, 1);

    @Test
    void aLaneWalksThroughSideBySideCellsBetweenItsEnds() {
        assertEquals(List.of(WEST, new AtlasCell(1, 0), new AtlasCell(1, 1), EAST), AtlasTerritoryBridges.path(WEST, EAST, cell -> true));
        assertEquals(List.of(WEST, new AtlasCell(1, 0), new AtlasCell(2, 0), new AtlasCell(3, 0)),
                AtlasTerritoryBridges.path(WEST, new AtlasCell(3, 0), cell -> true));
        assertEquals(List.of(WEST, SIDE_NORTH), AtlasTerritoryBridges.path(WEST, SIDE_NORTH, cell -> true));
        assertEquals(7, AtlasTerritoryBridges.path(WEST, FAR, cell -> true).size());
    }

    @Test
    void aDiagonalLaneTurnsThroughWhicheverCornerCellIsOpen() {
        assertEquals(List.of(WEST, SIDE_NORTH, DIAGONAL), AtlasTerritoryBridges.path(WEST, DIAGONAL, cell -> !cell.equals(SIDE_EAST)));
        assertEquals(List.of(WEST, SIDE_EAST, DIAGONAL), AtlasTerritoryBridges.path(WEST, DIAGONAL, cell -> !cell.equals(SIDE_NORTH)));
    }

    @Test
    void aDiagonalLaneWithBothCornerCellsBlockedStepsThroughTheCorner() {
        assertEquals(List.of(WEST, DIAGONAL), AtlasTerritoryBridges.path(WEST, DIAGONAL, cell -> false));
    }

    @Test
    void lanesBetweenOneTerritorysSystemsBridgeTheEmptyCellsTheyCross() {
        Function<AtlasCell, String> territories = cell -> cell.equals(WEST) || cell.equals(EAST) ? "held" : null;
        Map<AtlasCell, List<AtlasCell>> lanes = Map.of(WEST, List.of(EAST, FAR), EAST, List.of(WEST), FAR, List.of(WEST));

        AtlasTerritoryBridges.Bridges bridges = AtlasTerritoryBridges.bridges(List.of(WEST, EAST, FAR), territories,
                cell -> lanes.getOrDefault(cell, List.of()), lanes::containsKey);

        assertEquals(Map.of(new AtlasCell(1, 0).pack(), "held", new AtlasCell(1, 1).pack(), "held"), bridges.cells());
        assertEquals(Map.of(), bridges.corners());
    }

    @Test
    void aLaneSqueezedBetweenOtherTerritoriesJoinsItsEndsThroughTheirCorner() {
        Map<AtlasCell, List<AtlasCell>> lanes = Map.of(WEST, List.of(DIAGONAL), DIAGONAL, List.of(WEST), SIDE_EAST, List.of(), SIDE_NORTH, List.of());

        AtlasTerritoryBridges.Bridges bridges = AtlasTerritoryBridges.bridges(lanes.keySet(), AtlasTerritoryBridgesTest::squeezed,
                cell -> lanes.getOrDefault(cell, List.of()), lanes::containsKey);

        assertEquals(Map.of(), bridges.cells());
        assertEquals(Map.of(AtlasTerritoryBorders.cornerBetween(WEST, DIAGONAL), "held"), bridges.corners());
    }

    @Test
    void bridgedTerritoriesDrawAsOneShape() {
        Function<AtlasCell, String> territories = cell -> cell.equals(WEST) || cell.equals(EAST) ? "held" : null;
        Map<AtlasCell, List<AtlasCell>> lanes = Map.of(WEST, List.of(EAST), EAST, List.of(WEST));
        AtlasTerritoryBridges.Bridges bridges = AtlasTerritoryBridges.bridges(List.of(WEST, EAST), territories, lanes::get, lanes::containsKey);
        Set<Long> cells = new HashSet<>(bridges.cells().keySet());
        cells.add(WEST.pack());
        cells.add(EAST.pack());

        List<AtlasTerritoryBorders.Region> unbridged = AtlasTerritoryBorders.build(1L, Set.of(WEST.pack(), EAST.pack()), territories, lanes::containsKey);
        List<AtlasTerritoryBorders.Region> bridged = AtlasTerritoryBorders.build(1L, cells,
                cell -> territories.apply(cell) != null ? territories.apply(cell) : bridges.cells().get(cell.pack()), lanes::containsKey);

        assertEquals(2, unbridged.size());
        assertEquals(1, bridged.size());
        assertEquals(4, bridged.get(0).cells());
    }

    @Test
    void joinedCornersDrawDiagonalCellsAsOneShape() {
        Set<Long> cells = Set.of(WEST.pack(), DIAGONAL.pack(), SIDE_EAST.pack(), SIDE_NORTH.pack());
        Map<Long, String> corners = Map.of(AtlasTerritoryBorders.cornerBetween(WEST, DIAGONAL), "held");

        List<AtlasTerritoryBorders.Region> apart = AtlasTerritoryBorders.build(1L, cells, AtlasTerritoryBridgesTest::squeezed, cell -> true);
        List<AtlasTerritoryBorders.Region> joined = AtlasTerritoryBorders.build(1L, cells, AtlasTerritoryBridgesTest::squeezed, cell -> true, corners);

        assertEquals(2, apart.stream().filter(region -> region.groupId().equals("held")).count());
        List<AtlasTerritoryBorders.Region> held = joined.stream().filter(region -> region.groupId().equals("held")).toList();
        assertEquals(1, held.size());
        assertEquals(2, held.get(0).cells());
        assertEquals(1, held.get(0).loops().size());
        assertEquals(2, joined.stream().filter(region -> region.groupId().equals("other")).count());
    }

    private static String squeezed(AtlasCell cell) {
        return cell.equals(WEST) || cell.equals(DIAGONAL) ? "held" : "other";
    }
}
