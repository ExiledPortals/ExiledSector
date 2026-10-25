package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

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
    void scrollingShiftsWhereTheTilesBreak() {
        assertEquals(0f, BeamStroke.tileStart(0f, 40f, 0f));
        assertEquals(-10f, BeamStroke.tileStart(0f, 40f, 10f));
        assertEquals(30f, BeamStroke.tileStart(30f, 40f, 10f));
        assertEquals(5f, BeamStroke.positiveModulo(-35f, 40f));
    }
}
