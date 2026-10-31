package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeamStrokeTest {

    @Test
    void piecesCoverTheWholeStrokeWithTextureCoordinatesInsideOneTile() {
        float[][] cases = {{100f, 40f, 0f, 0f}, {100f, 40f, 13.5f, 0f}, {250f, 33f, 32.9995f, 9f}, {3f, 40f, 39f, 0f}};
        for (float[] stroke : cases) {
            float length = stroke[0];
            float tile = stroke[1];
            float scroll = stroke[2];
            float maxPiece = stroke[3];
            float start = 0f;
            int pieces = 0;
            while (start < length) {
                float tileStart = BeamStroke.tileStart(start, tile, scroll);
                float end = BeamStroke.pieceEnd(start, tileStart, length, tile, maxPiece);
                float startU = (start - tileStart) / tile;
                float endU = (end - tileStart) / tile;

                assertTrue(end > start, "every piece advances");
                assertTrue(startU >= 0f && endU <= 1.0001f, "texture stays within one tile: " + startU + ".." + endU);
                if (maxPiece > 0f) {
                    assertTrue(end - start <= maxPiece + 0.0001f);
                }
                start = end;
                pieces++;
            }
            assertEquals(length, start, 0.0001f);
            assertTrue(pieces <= length / tile + 2 + (maxPiece > 0f ? length / maxPiece : 0f));
        }
    }

    @Test
    void longStrokesKeepAdvancingDespiteFloatRounding() {
        float tile = 11.2f;
        for (float scroll = 0f; scroll < tile; scroll += 0.37f) {
            float length = 60000f;
            float start = 0f;
            while (start < length) {
                float tileStart = BeamStroke.tileStart(start, tile, scroll);
                float end = BeamStroke.pieceEnd(start, tileStart, length, tile, 0f);
                assertTrue(end > start, "stalled at " + start + " with scroll " + scroll);
                assertTrue(start >= tileStart && end - tileStart <= tile + 4f * Math.ulp(end), "texture stays within one tile at " + start);
                start = end;
            }
        }
    }

    @Test
    void scrollingShiftsWhereTheTilesBreak() {
        assertEquals(0f, BeamStroke.tileStart(0f, 40f, 0f));
        assertEquals(-10f, BeamStroke.tileStart(0f, 40f, 10f));
        assertEquals(30f, BeamStroke.tileStart(30f, 40f, 10f));
        assertEquals(5f, BeamStroke.positiveModulo(-35f, 40f));
    }

    @Test
    void outlinedRingsFollowTheirShapeAndCloseOnThemselves() {
        float[] square = new float[32];
        Arrays.fill(square, 1f);
        for (int i = 0; i < square.length; i += 2) {
            square[i] = 2f;
        }
        BeamStroke.RingPath path = new BeamStroke.RingPath();
        path.set(10f, 20f, 5f, square);
        float[] start = new float[4];
        float[] end = new float[4];
        path.at(0f, start);
        path.at(path.length(), end);

        assertEquals(20f, start[0], 0.001f);
        assertEquals(20f, start[1], 0.001f);
        assertEquals(start[0], end[0], 0.001f);
        assertEquals(start[1], end[1], 0.001f);
        assertTrue(path.length() > 2f * Math.PI * 5f);
    }

    @Test
    void plainRingsAreCircles() {
        BeamStroke.RingPath path = new BeamStroke.RingPath();
        path.set(0f, 0f, 10f, null);
        float[] quarter = new float[4];
        path.at(path.length() / 4f, quarter);

        assertEquals(2f * Math.PI * 10f, path.length(), 0.001f);
        assertEquals(0f, quarter[0], 0.001f);
        assertEquals(10f, quarter[1], 0.001f);
    }

    @Test
    void helixStrandsWindAroundTheLineAtAnEvenWidth() {
        BeamStroke.LinePath line = new BeamStroke.LinePath();
        line.set(0f, 0f, 100f, 0f);
        BeamStroke.Helix helix = new BeamStroke.Helix(5f, 20f, 0f, 0.3f, 1f);
        BeamStroke.HelixPath path = new BeamStroke.HelixPath();
        path.set(line, helix, 10f);
        float[] crest = new float[4];
        float[] crossing = new float[4];
        path.at(-5f, crest);
        path.at(0f, crossing);

        assertEquals(-5f, crest[0], 0.001f);
        assertEquals(5f, crest[1], 0.001f);
        assertEquals(0f, crest[2], 0.001f);
        assertEquals(1f, crest[3], 0.001f);
        assertEquals(0f, crossing[1], 0.001f);
        assertEquals(1f, (float) Math.hypot(crossing[2], crossing[3]), 0.001f);
        assertTrue(crossing[2] > 0f, "the strand leans along its own slope");
        assertEquals(0.3f, path.shade(0f), 0.001f);
        assertEquals(1f, path.shade(10f), 0.001f);
    }

    @Test
    void ringPiecesTrackScreenSizeWithinBounds() {
        assertEquals(1f, BeamStroke.ringPiece(12f), 0.0001f);
        assertEquals(8f, BeamStroke.ringPiece(200f), 0.0001f);
        assertEquals(2000f / 45f, BeamStroke.ringPiece(2000f), 0.0001f);
    }
}
