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
            assertTrue(centreY + halfTile < BenchScene.detailsTop(mode), tile + " overlaps the details plate");
            assertTrue(centreY - halfTile > BenchScene.NOTICE_TOP + BenchScene.NOTICE_HEIGHT, tile + " overlaps the notice");
            assertTrue(centreX - halfTile > mode.leftWardX() + halfTile, tile + " overlaps the left inhibitor");
            assertTrue(centreX + halfTile < mode.rightWardX() - halfTile, tile + " overlaps the right inhibitor");
        }
        for (int index = 1; index < tileCount; index++) {
            float gapX = Math.abs(centres[index][0] - centres[index - 1][0]);
            float gapY = Math.abs(centres[index][1] - centres[index - 1][1]);
            assertTrue(Math.max(gapX, gapY) >= BenchScene.TILE_SIZE, mode + " tiles " + (index - 1) + " and " + index + " overlap");
        }
    }

    @Test
    void theDetailsPlateStartsJustUnderTheSocketGraphicInEveryModeAndFitsTheArt() {
        assertEquals(111f, BenchScene.detailsTop(BenchMode.HULL_ITEMS), 0.1f, "the hull bench plate keeps its place");
        for (BenchMode mode : BenchMode.values()) {
            float gap = BenchScene.detailsTop(mode) - (mode.socketY() + BenchScene.SLOT_RADIUS * 1.05f);
            assertTrue(gap > 0f && gap < 3f, mode + " plate does not start just under its socket graphic");
            assertTrue(BenchScene.detailsTop(mode) < BenchScene.CHART_DETAILS_BOTTOM, mode + " plate has no height");
        }
        assertTrue(BenchScene.DETAILS_BOTTOM <= BenchScene.ART_HEIGHT);
        for (BenchMode mode : BenchMode.values()) {
            assertTrue(BenchScene.detailsLeft(mode) - BenchScene.TILE_SIZE / 2f > mode.leftWardX(), mode + " left inhibitor overlaps the plate");
            assertTrue(BenchScene.detailsLeft(mode) + BenchScene.DETAILS_WIDTH + BenchScene.TILE_SIZE / 2f < mode.rightWardX(),
                    mode + " right inhibitor overlaps the plate");
        }
    }

    @Test
    void theStarfieldBenchesCentreTheSocketAndMirrorTheInhibitorsBesideIt() {
        for (BenchMode mode : new BenchMode[]{BenchMode.HULL_ITEMS, BenchMode.STAR_CHARTS}) {
            assertEquals(BenchScene.ART_WIDTH / 2f, mode.socketX(), 1e-6f, mode + " socket is off-centre");
            assertEquals(mode.socketX() - mode.leftWardX(), mode.rightWardX() - mode.socketX(), 1e-6f, mode + " inhibitors are lopsided");
            assertEquals(mode.socketY(), mode.wardY(), 1e-6f, mode + " inhibitors are not level with the socket");
        }
        for (BenchMode mode : BenchMode.values()) {
            float halfTile = BenchScene.TILE_SIZE / 2f;
            assertTrue(mode.leftWardX() - halfTile >= 0f && mode.rightWardX() + halfTile <= BenchScene.ART_WIDTH, mode + " inhibitor tile leaves the art");
        }
    }

    @Test
    void theOpenWormholeButtonSitsUnderTheShortenedChartPlateInsideTheArt() {
        assertTrue(BenchScene.CHART_DETAILS_BOTTOM < BenchScene.DETAILS_BOTTOM);
        assertTrue(BenchScene.CHART_DETAILS_BOTTOM < BenchScene.OPEN_BUTTON_TOP, "the button overlaps the plate");
        assertTrue(BenchScene.OPEN_BUTTON_TOP + BenchScene.OPEN_BUTTON_HEIGHT <= BenchScene.ART_HEIGHT, "the button runs off the art");
    }
}
