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

    private static float orbitTileLeft(BenchMode mode, BenchSpread spread, float scale) {
        return spread.socketX() - mode.orbitRadiusX() * scale * (float) Math.cos(BenchScene.ORBIT_DROP_RADIANS) - BenchScene.tileSize(scale) / 2f;
    }

    @Test
    void theContractedHullBenchClosesEveryGapToOneSmallStep() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        for (float scale : SCALES) {
            BenchSpread contracted = BenchSpread.contracted(mode, scale, HULL_BENCH);
            float tileSize = BenchScene.tileSize(scale);
            float gap = BenchScene.COLUMN_GAP * scale;
            float prefixLeft = contracted.prefixX() - tileSize / 2f;
            float suffixRight = contracted.suffixX() + tileSize / 2f;

            assertEquals(gap, prefixLeft, TOLERANCE, "scale " + scale + " margin before the prefix inhibitor");
            assertEquals(gap, orbitTileLeft(mode, contracted, scale) - (prefixLeft + tileSize), TOLERANCE, "scale " + scale + " prefix to nanoswarm");
            assertEquals(contracted.socketX() - contracted.prefixX(), contracted.suffixX() - contracted.socketX(), TOLERANCE,
                    "scale " + scale + " inhibitors stay level with the socket");
            assertEquals(gap, contracted.columnLeft() - suffixRight, TOLERANCE, "scale " + scale + " suffix to the column");
            assertEquals(contracted.artWidth(), contracted.columnLeft() + tileSize + gap, TOLERANCE, "scale " + scale + " margin after the column");
        }
    }

    @Test
    void theContractedBenchStillHoldsTheDetailsPlate() {
        for (BenchMode mode : BenchMode.values()) {
            for (BenchSpread.Contents contents : new BenchSpread.Contents[]{HULL_BENCH, FORGE}) {
                for (float scale : SCALES) {
                    BenchSpread contracted = BenchSpread.contracted(mode, scale, contents);
                    float plateHalf = BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;

                    assertTrue(contracted.socketX() - plateHalf >= 0f, mode + " plate leaves the left edge at scale " + scale);
                    assertTrue(contracted.socketX() + plateHalf <= contracted.artWidth(), mode + " plate leaves the right edge at scale " + scale);
                    assertTrue(contracted.artWidth() < BenchSpread.full(mode, scale).artWidth(), mode + " did not contract at scale " + scale);
                }
            }
        }
    }

    @Test
    void theColumnClearsTheDetailsPlate() {
        for (float scale : SCALES) {
            BenchSpread contracted = BenchSpread.contracted(BenchMode.HULL_ITEMS, scale, HULL_BENCH);
            float plateRight = contracted.socketX() + BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;
            assertTrue(contracted.columnLeft() > plateRight, "scale " + scale);
        }
    }

    @Test
    void aForgeContractsToItsDetailsPlateWithTheArtCroppedAroundTheSocket() {
        BenchMode mode = BenchMode.FORGE;
        float scale = 3f;
        BenchSpread contracted = BenchSpread.contracted(mode, scale, FORGE);
        float gap = BenchScene.COLUMN_GAP * scale;
        float plateHalf = BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;
        float visibleLeft = BenchScene.visibleArtLeft(contracted.artShift(mode, scale), contracted.artWidth(), scale);

        assertEquals((gap + plateHalf) * 2f, contracted.artWidth(), TOLERANCE);
        assertEquals(mode.socketX() * scale, visibleLeft + contracted.socketX(), TOLERANCE, "the cropped art keeps the forge behind the socket");
    }

    @Test
    void theTransitionRunsSmoothlyFromFullToContracted() {
        BenchMode mode = BenchMode.HULL_ITEMS;
        BenchSpread full = BenchSpread.full(mode, 4f);
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
}
