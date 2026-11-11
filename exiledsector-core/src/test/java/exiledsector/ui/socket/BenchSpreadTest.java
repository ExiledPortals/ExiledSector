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
    private static final int TRAY_COLUMNS_AT_SMALLEST_SCALE = 4;
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
            assertEquals(contracted.artWidth(), contracted.controlsLeft() + tileSize + gap, TOLERANCE, "scale " + scale + " margin after the tray controls");
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
    void theTrayControlsTakeTheRightmostColumnAndPushTheCraftingItemsLeft() {
        for (float scale : SCALES) {
            BenchSpread withTrays = BenchSpread.full(BenchMode.HULL_ITEMS, scale, HULL_BENCH);
            BenchSpread withoutTrays = BenchSpread.full(BenchMode.HULL_ITEMS, scale, new BenchSpread.Contents(true, true, false));
            float tileSize = BenchScene.tileSize(scale);
            float gap = BenchScene.COLUMN_GAP * scale;

            assertEquals(withoutTrays.columnLeft(), withTrays.controlsLeft(), TOLERANCE, "scale " + scale);
            assertEquals(gap, withTrays.controlsLeft() - (withTrays.columnLeft() + tileSize), TOLERANCE, "scale " + scale);
        }
    }

    @Test
    void openingATrayMovesTheSocketAndColumnLeftAndKeepsTheBenchWidth() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        for (float scale : SCALES) {
            BenchSpread full = BenchSpread.full(mode, scale, HULL_BENCH);
            BenchSpread trayOpen = BenchSpread.trayOpen(mode, scale, HULL_BENCH);
            float tileSize = BenchScene.tileSize(scale);
            float gap = BenchScene.COLUMN_GAP * scale;

            assertEquals(full.artWidth(), trayOpen.artWidth(), TOLERANCE, "scale " + scale + " bench width");
            assertEquals(full.controlsLeft(), trayOpen.controlsLeft(), TOLERANCE, "scale " + scale + " tray controls stay put");
            assertTrue(trayOpen.socketX() < full.socketX(), "scale " + scale + " socket moves left");
            assertTrue(trayOpen.columnLeft() < full.columnLeft(), "scale " + scale + " column moves left");
            assertTrue(trayOpen.socketX() - plateHalf(scale) >= 0f, "scale " + scale + " details plate stays on the bench");
            assertEquals(gap, trayOpen.trayLeft() - (trayOpen.columnLeft() + tileSize), TOLERANCE, "scale " + scale + " column to the tray");
            assertEquals(gap, trayOpen.controlsLeft() - trayOpen.trayRight(scale), TOLERANCE, "scale " + scale + " tray to the controls");
            assertTrue(trayOpen.trayWidth(scale) > 0f, "scale " + scale + " tray has room");
            assertEquals(0f, full.trayWidth(scale), TOLERANCE, "scale " + scale + " a closed tray has no width");
        }
    }

    @Test
    void anOpenTrayFitsTheSmallestScreen() {
        BenchSpread trayOpen = BenchSpread.trayOpen(BenchMode.HULL_ITEMS, SMALLEST_SCALE, HULL_BENCH);

        assertTrue(trayOpen.artWidth() + CraftingBenchPanel.FRAME_INSET * 2f <= SCREEN_WIDTH);
        assertTrue(ItemGrid.columnsFor(trayOpen.trayWidth(SMALLEST_SCALE)) >= TRAY_COLUMNS_AT_SMALLEST_SCALE);
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
    void openingATrayMovesTheSocketSteadilyLeft() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        BenchSpread full = BenchSpread.full(mode, 4f, HULL_BENCH);
        BenchSpread trayOpen = BenchSpread.trayOpen(mode, 4f, HULL_BENCH);

        float previousSocket = full.socketX();
        for (int step = 1; step <= 20; step++) {
            float socketX = full.towards(trayOpen, BenchSpread.eased(step / 20f)).socketX();
            assertTrue(socketX <= previousSocket, "step " + step);
            previousSocket = socketX;
        }
    }
}
