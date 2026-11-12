package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchSpreadTest {

    private static final float TOLERANCE = 0.001f;
    private static final BenchSpread.Contents HULL_BENCH = new BenchSpread.Contents(true, true, true);
    private static final BenchSpread.Contents FORGE = new BenchSpread.Contents(false, false, false);
    private static final float[] SCALES = {3f, 4f, 5f, 6f};
    private static final float SMALLEST_SCALE = 3f;
    private static final float SCREEN_WIDTH = 1280f;

    private static float orbitTileLeft(BenchMode mode, BenchSpread spread, float scale) {
        return spread.socketX() - mode.orbitRadiusX() * scale * (float) Math.cos(BenchScene.ORBIT_DROP_RADIANS) - BenchScene.tileSize(scale) / 2f;
    }

    private static float plateHalf(float scale) {
        return BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;
    }

    @Test
    void theContractedHullBenchClosesEveryGapToOneSmallStep() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        for (float scale : SCALES) {
            BenchSpread contracted = BenchSpread.contracted(mode, scale, HULL_BENCH);
            float tileSize = BenchScene.tileSize(scale);
            float gap = BenchScene.COLUMN_GAP * scale;
            float socketLeft = Math.min(orbitTileLeft(mode, contracted, scale), contracted.socketX() - plateHalf(scale));
            float socketRight = contracted.socketX() * 2f - socketLeft;

            assertEquals(gap, socketLeft, TOLERANCE, "scale " + scale + " margin before the socket");
            assertEquals(gap, contracted.columnLeft() - socketRight, TOLERANCE, "scale " + scale + " socket to the column");
            assertEquals(gap, contracted.controlsLeft() - (contracted.columnLeft() + tileSize), TOLERANCE, "scale " + scale + " column to the tray controls");
            assertEquals(contracted.artWidth(), contracted.controlsLeft() + tileSize, TOLERANCE, "scale " + scale + " tray controls reach the right edge");
        }
    }

    @Test
    void theContractedBenchStillHoldsTheDetailsPlate() {
        for (BenchMode mode : BenchMode.values()) {
            for (BenchSpread.Contents contents : new BenchSpread.Contents[]{HULL_BENCH, FORGE}) {
                for (float scale : SCALES) {
                    BenchSpread contracted = BenchSpread.contracted(mode, scale, contents);

                    assertTrue(contracted.socketX() - plateHalf(scale) >= 0f, mode + " plate leaves the left edge at scale " + scale);
                    assertTrue(contracted.socketX() + plateHalf(scale) <= contracted.artWidth(), mode + " plate leaves the right edge at scale " + scale);
                    assertTrue(contracted.artWidth() < BenchSpread.full(mode, scale, contents).artWidth(), mode + " did not contract at scale " + scale);
                }
            }
        }
    }

    @Test
    void theColumnClearsTheDetailsPlate() {
        for (float scale : SCALES) {
            BenchSpread contracted = BenchSpread.contracted(BenchMode.HULL_ITEMS, scale, HULL_BENCH);
            float plateRight = contracted.socketX() + plateHalf(scale);
            assertTrue(contracted.columnLeft() > plateRight, "scale " + scale);
        }
    }

    @Test
    void theTrayControlsHugTheRightEdgeAndTheCraftingItemsSitBesideThem() {
        for (float scale : SCALES) {
            BenchSpread withTrays = BenchSpread.full(BenchMode.HULL_ITEMS, scale, HULL_BENCH);
            float tileSize = BenchScene.tileSize(scale);
            float gap = BenchScene.COLUMN_GAP * scale;

            assertEquals(withTrays.artWidth(), withTrays.controlsLeft() + tileSize, TOLERANCE, "scale " + scale);
            assertEquals(gap, withTrays.controlsLeft() - (withTrays.columnLeft() + tileSize), TOLERANCE, "scale " + scale);
        }
    }

    @Test
    void aWideTrayOpensBesideTheTrayControlsAndPushesTheColumnAndSocketLeft() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        for (float scale : SCALES) {
            BenchSpread full = BenchSpread.full(mode, scale, HULL_BENCH);
            float widestTray = BenchSpread.maxTrayWidth(mode, scale, HULL_BENCH);
            BenchSpread trayOpen = BenchSpread.trayOpen(mode, scale, HULL_BENCH, widestTray * 2f);
            float tileSize = BenchScene.tileSize(scale);
            float gap = BenchScene.COLUMN_GAP * scale;
            float socketLeft = Math.min(orbitTileLeft(mode, trayOpen, scale), trayOpen.socketX() - plateHalf(scale));
            float socketRight = trayOpen.socketX() * 2f - socketLeft;

            assertEquals(full.artWidth(), trayOpen.artWidth(), TOLERANCE, "scale " + scale + " bench width");
            assertEquals(full.controlsLeft(), trayOpen.controlsLeft(), TOLERANCE, "scale " + scale + " tray controls stay put");
            assertEquals(widestTray, trayOpen.trayWidth(), TOLERANCE, "scale " + scale + " the tray is capped");
            assertEquals(gap, trayOpen.controlsLeft() - (trayOpen.trayLeft() + trayOpen.trayWidth()), TOLERANCE, "scale " + scale + " tray to the controls");
            assertEquals(gap, trayOpen.trayLeft() - (trayOpen.columnLeft() + tileSize), TOLERANCE, "scale " + scale + " column to the tray");
            assertEquals(gap, trayOpen.columnLeft() - socketRight, TOLERANCE, "scale " + scale + " socket to the column");
            assertEquals(gap, socketLeft, TOLERANCE, "scale " + scale + " margin before the socket");
            assertEquals(0f, full.trayWidth(), TOLERANCE, "scale " + scale + " a closed tray has no width");
            assertEquals(full.controlsLeft() - gap, full.trayLeft(), TOLERANCE, "scale " + scale + " a closed tray sits at the controls");
        }
    }

    @Test
    void aNarrowTrayKeepsTheSocketWhereItWas() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        for (float scale : SCALES) {
            BenchSpread full = BenchSpread.full(mode, scale, HULL_BENCH);
            float narrowWidth = BenchScene.tileSize(scale);
            BenchSpread trayOpen = BenchSpread.trayOpen(mode, scale, HULL_BENCH, narrowWidth);

            assertEquals(narrowWidth, trayOpen.trayWidth(), TOLERANCE, "scale " + scale + " the wanted width is kept");
            assertEquals(full.socketX(), trayOpen.socketX(), TOLERANCE, "scale " + scale + " the socket stays put");
            assertTrue(trayOpen.columnLeft() < full.columnLeft(), "scale " + scale + " the column moves aside");
        }
    }

    @Test
    void anOpenTrayFitsTwoListColumnsOnTheSmallestScreen() {
        BenchSpread trayOpen = BenchSpread.trayOpen(BenchMode.HULL_ITEMS, SMALLEST_SCALE, HULL_BENCH,
                BenchSpread.maxTrayWidth(BenchMode.HULL_ITEMS, SMALLEST_SCALE, HULL_BENCH));
        float listWidth = trayOpen.trayWidth() - CraftingBenchPanel.TRAY_SIDE_INSET * 2f;

        assertTrue(trayOpen.artWidth() + CraftingBenchPanel.FRAME_INSET * 2f <= SCREEN_WIDTH);
        assertTrue(BenchTrayList.columnWidth(listWidth, 2) >= BenchTrayList.MIN_COLUMN_WIDTH);
    }

    @Test
    void aForgeContractsToItsDetailsPlateWithTheArtCroppedAroundTheSocket() {
        BenchMode mode = BenchMode.FORGE;
        float scale = 3f;
        BenchSpread contracted = BenchSpread.contracted(mode, scale, FORGE);
        float gap = BenchScene.COLUMN_GAP * scale;
        float visibleLeft = BenchScene.visibleArtLeft(contracted.artShift(), contracted.artWidth(), scale);

        assertEquals((gap + plateHalf(scale)) * 2f, contracted.artWidth(), TOLERANCE);
        assertEquals(mode.socketX() * scale, visibleLeft + contracted.socketX(), TOLERANCE, "the cropped art keeps the forge behind the socket");
    }

    @Test
    void theTransitionRunsSmoothlyFromFullToContracted() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        BenchSpread full = BenchSpread.full(mode, 4f, HULL_BENCH);
        BenchSpread contracted = BenchSpread.contracted(mode, 4f, HULL_BENCH);

        assertSame(full, full.towards(contracted, BenchSpread.eased(0f)));
        assertSame(contracted, full.towards(contracted, BenchSpread.eased(1f)));
        float previousWidth = full.artWidth();
        for (int step = 1; step <= 20; step++) {
            float width = full.towards(contracted, BenchSpread.eased(step / 20f)).artWidth();
            assertTrue(width <= previousWidth, "step " + step);
            previousWidth = width;
        }
    }

    @Test
    void openingATrayGrowsItSteadilyLeftwards() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        BenchSpread full = BenchSpread.full(mode, 4f, HULL_BENCH);
        BenchSpread trayOpen = BenchSpread.trayOpen(mode, 4f, HULL_BENCH, BenchSpread.maxTrayWidth(mode, 4f, HULL_BENCH));

        float previousTrayLeft = full.trayLeft();
        float previousSocket = full.socketX();
        for (int step = 1; step <= 20; step++) {
            BenchSpread shown = full.towards(trayOpen, BenchSpread.eased(step / 20f));
            assertTrue(shown.trayLeft() <= previousTrayLeft, "step " + step + " tray edge");
            assertTrue(shown.socketX() <= previousSocket, "step " + step + " socket");
            previousTrayLeft = shown.trayLeft();
            previousSocket = shown.socketX();
        }
    }
}
