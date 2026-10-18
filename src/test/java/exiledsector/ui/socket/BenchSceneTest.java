package exiledsector.ui.socket;

import exiledsector.items.CraftingCurrency;
import exiledsector.items.StarChartCurrency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchSceneTest {

    @Test
    void theArtScalesByWholePixelsAndFallsBackBelowDoubleSize() {
        assertEquals(4f, BenchScene.scaleFor(1300f, 740f));
        assertEquals(3f, BenchScene.scaleFor(1279f, 900f), "width limits to x3");
        assertEquals(3f, BenchScene.scaleFor(2000f, 719f), "height limits to x3");
        assertEquals(2f, BenchScene.scaleFor(640f, 360f));
        assertEquals(1.5f, BenchScene.scaleFor(480f, 300f), 1e-6f, "tiny areas fit exactly instead of snapping to x1");
    }

    @Test
    void thePanelWrapsTheArtInItsFrame() {
        float scale = SocketWorkbenchPanel.scaleFor(1300f + SocketWorkbenchPanel.FRAME_INSET * 2f, 740f + SocketWorkbenchPanel.FRAME_INSET * 2f);
        assertEquals(4f, scale);
        assertEquals(1280f + SocketWorkbenchPanel.FRAME_INSET * 2f, SocketWorkbenchPanel.sizeFor(scale, BenchScene.ART_WIDTH));
        assertEquals(720f + SocketWorkbenchPanel.FRAME_INSET * 2f, SocketWorkbenchPanel.sizeFor(scale, BenchScene.ART_HEIGHT));
    }

    @Test
    void storageSitsBesideTheBenchOnlyWhenTheBenchCanStillDrawAtTripleSize() {
        assertTrue(ModificationBay.fitsSideBySide(1840f, 1000f), "1920x1080 with margins");
        assertFalse(ModificationBay.fitsSideBySide(1286f, 688f), "1366x768 with margins uses the drawer");
    }

    @Test
    void tilesStayClearOfTheConsolesTheNoticeAndTheDetailsPlateInEveryMode() {
        assertTilesClear(BenchMode.HULL_ITEMS, CraftingCurrency.values().length);
        assertTilesClear(BenchMode.STAR_CHARTS, StarChartCurrency.KERNELS_ON_CHARTS.size() + StarChartCurrency.values().length);
    }

    private static void assertTilesClear(BenchMode mode, int tileCount) {
        float halfTile = BenchScene.TILE_SIZE / 2f;
        float[][] centres = new float[tileCount][];
        for (int index = 0; index < tileCount; index++) {
            float angle = SocketWorkbenchPanel.tileAngle(index, tileCount);
            float centreX = mode.socketX() + mode.orbitRadiusX() * (float) Math.cos(angle);
            float centreY = mode.socketY() - mode.orbitRadiusY() * (float) Math.sin(angle);
            centres[index] = new float[]{centreX, centreY};
            String tile = mode + " tile " + index;
            assertTrue(centreY + halfTile < BenchScene.DETAILS_TOP, tile + " overlaps the details plate");
            assertTrue(centreY - halfTile > BenchScene.NOTICE_TOP + BenchScene.NOTICE_HEIGHT, tile + " overlaps the notice");
            if (mode.showsInhibitors()) {
                assertTrue(centreX - halfTile > BenchScene.LEFT_CONSOLE_X + halfTile, tile + " overlaps the left console");
                assertTrue(centreX + halfTile < BenchScene.RIGHT_CONSOLE_X - halfTile, tile + " overlaps the right console");
            }
        }
        for (int index = 1; index < tileCount; index++) {
            float gapX = Math.abs(centres[index][0] - centres[index - 1][0]);
            float gapY = Math.abs(centres[index][1] - centres[index - 1][1]);
            assertTrue(Math.max(gapX, gapY) >= BenchScene.TILE_SIZE, mode + " tiles " + (index - 1) + " and " + index + " overlap");
        }
    }

    @Test
    void theDetailsPlateSitsUnderTheSocketGraphicInEveryModeAndFitsTheArt() {
        float hullSocketBottom = BenchMode.HULL_ITEMS.socketY() + BenchScene.SLOT_RADIUS * 1.05f;
        assertTrue(BenchScene.DETAILS_TOP - hullSocketBottom < 5f, "the hull bench plate starts just under its socket");
        for (BenchMode mode : BenchMode.values()) {
            assertTrue(BenchScene.DETAILS_TOP > mode.socketY() + BenchScene.SLOT_RADIUS * 1.05f, mode + " socket graphic overlaps the plate");
        }
        assertTrue(BenchScene.DETAILS_TOP + BenchScene.DETAILS_HEIGHT <= BenchScene.ART_HEIGHT);
        assertTrue(BenchScene.DETAILS_LEFT - BenchScene.TILE_SIZE / 2f > BenchScene.LEFT_CONSOLE_X);
        assertTrue(BenchScene.DETAILS_LEFT + BenchScene.DETAILS_WIDTH + BenchScene.TILE_SIZE / 2f < BenchScene.RIGHT_CONSOLE_X);
    }
}
