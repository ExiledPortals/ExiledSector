package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTerritoryBordersTest {

    private static final float DISPLAY_REACH = 0.4f * AtlasMapCanvas.DISPLAY_JITTER_SHARE;

    private static String regionAt(AtlasCell cell) {
        int x = cell.gridX();
        int y = cell.gridY();
        if (x >= 20 && x <= 22 && y >= -1 && y <= 1) {
            return x == 21 && y == 0 ? null : "ring";
        }
        if ((x == 10 && y == 10) || (x == 11 && y == 11)) {
            return "diagonal";
        }
        if (x < 0) {
            return "west";
        }
        return y > 3 ? "north" : null;
    }

    private static Set<Long> known() {
        Set<Long> known = new HashSet<>();
        for (int x = -8; x <= 24; x++) {
            for (int y = -6; y <= 12; y++) {
                known.add(new AtlasCell(x, y).pack());
            }
        }
        return known;
    }

    private static boolean inside(AtlasTerritoryBorders.Region region, float x, float y) {
        boolean inside = false;
        for (AtlasTerritoryBorders.Loop loop : region.loops()) {
            float[] points = loop.outline();
            for (int index = 0, previous = points.length - 2; index < points.length; previous = index, index += 2) {
                float ax = points[index];
                float ay = points[index + 1];
                float bx = points[previous];
                float by = points[previous + 1];
                if ((ay > y) != (by > y) && x < (bx - ax) * (y - ay) / (by - ay) + ax) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    @Test
    void everySystemSitsInsideItsOwnBiomeBorderAndOpenSpaceHasNone() {
        Set<Long> known = known();
        Function<AtlasCell, String> regions = AtlasTerritoryBordersTest::regionAt;
        List<AtlasTerritoryBorders.Region> built = AtlasTerritoryBorders.build(known, regions);

        assertEquals(List.of("diagonal", "diagonal", "north", "ring", "west"), built.stream().map(AtlasTerritoryBorders.Region::groupId).sorted().toList());
        for (long packed : known) {
            AtlasCell cell = AtlasCell.unpack(packed);
            String regionId = regionAt(cell);
            for (float[] offset : new float[][] {{0f, 0f}, {DISPLAY_REACH, DISPLAY_REACH}, {-DISPLAY_REACH, DISPLAY_REACH},
                    {DISPLAY_REACH, -DISPLAY_REACH}, {-DISPLAY_REACH, -DISPLAY_REACH}}) {
                float x = (cell.gridX() + offset[0]) * AtlasMapCanvas.CELL_PIXELS;
                float y = (cell.gridY() + offset[1]) * AtlasMapCanvas.CELL_PIXELS;
                int containing = 0;
                for (AtlasTerritoryBorders.Region region : built) {
                    if (inside(region, x, y)) {
                        containing++;
                        assertEquals(regionId, region.groupId(), cell + " drawn inside " + region.groupId());
                    }
                }
                assertEquals(regionId == null ? 0 : 1, containing, cell + " at " + offset[0] + "," + offset[1]);
            }
        }
    }

    @Test
    void ringsKeepTheirHoleAndLabelsSitInsideTheirPatch() {
        List<AtlasTerritoryBorders.Region> built = AtlasTerritoryBorders.build(known(), AtlasTerritoryBordersTest::regionAt);
        AtlasTerritoryBorders.Region ring = built.stream().filter(region -> region.groupId().equals("ring")).findFirst().orElseThrow();

        assertEquals(2, ring.loops().size());
        assertEquals(8, ring.cells());
        assertFalse(inside(ring, 21 * AtlasMapCanvas.CELL_PIXELS, 0f));
        for (AtlasTerritoryBorders.Region region : built) {
            assertTrue(inside(region, region.labelX(), region.labelY()), region.groupId());
            for (AtlasTerritoryBorders.Loop loop : region.loops()) {
                assertEquals(loop.outline().length, loop.inner().length);
                assertTrue(loop.outline().length >= 8 && loop.outline().length % 2 == 0);
            }
        }
    }

    @Test
    void outlinesAreSmoothWithNoSharpTurns() {
        for (AtlasTerritoryBorders.Region region : AtlasTerritoryBorders.build(known(), AtlasTerritoryBordersTest::regionAt)) {
            for (AtlasTerritoryBorders.Loop loop : region.loops()) {
                float[] points = loop.outline();
                int count = points.length / 2;
                for (int index = 0; index < count; index++) {
                    int previous = (index + count - 1) % count;
                    int next = (index + 1) % count;
                    double inX = points[index * 2] - points[previous * 2];
                    double inY = points[index * 2 + 1] - points[previous * 2 + 1];
                    double outX = points[next * 2] - points[index * 2];
                    double outY = points[next * 2 + 1] - points[index * 2 + 1];
                    double turn = Math.toDegrees(Math.abs(Math.atan2(inX * outY - inY * outX, inX * outX + inY * outY)));
                    assertTrue(turn < 50.0, region.groupId() + " turns " + turn + " degrees");
                }
            }
        }
    }

    @Test
    void outlinesFollowThePlayersRevealedSpaceOnly() {
        Set<Long> known = Set.of(new AtlasCell(-3, 0).pack(), new AtlasCell(-2, 0).pack());
        List<AtlasTerritoryBorders.Region> built = AtlasTerritoryBorders.build(known, AtlasTerritoryBordersTest::regionAt);

        assertEquals(1, built.size());
        assertEquals(2, built.get(0).cells());
        assertTrue(built.get(0).maxX() < -1.4f * AtlasMapCanvas.CELL_PIXELS);
    }
}
