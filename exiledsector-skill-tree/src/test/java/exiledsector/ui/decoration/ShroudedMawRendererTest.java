package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShroudedMawRendererTest {

    private static final float FRAME_SECONDS = 1f / 60f;

    private static float ellipseShare(ShroudedMawRenderer.Puff puff) {
        float side = puff.side() / ShroudedMawRenderer.SHROUD_RADIUS_LENGTHS;
        float forward = puff.forward() / (ShroudedMawRenderer.SHROUD_RADIUS_LENGTHS * ShroudedMawRenderer.FORWARD_SQUASH);
        return (float) Math.hypot(side, forward);
    }

    @Test
    void shroudPuffsStayInsideTheFlattenedShroudAndFadeBetweenLives() {
        for (int index = 0; index < ShroudedMawRenderer.SHROUD_PUFFS; index++) {
            for (float atSeconds = 0f; atSeconds < 60f; atSeconds += 0.7f) {
                ShroudedMawRenderer.Puff puff = ShroudedMawRenderer.shroudPuff(index, atSeconds);
                assertTrue(ellipseShare(puff) <= 1.0001f, "puff " + index + " left the shroud at " + atSeconds + "s");
                assertTrue(puff.alpha() >= 0f && puff.alpha() <= 1f);
                assertTrue(puff.flash() >= 0f && puff.flash() <= 1f);
                assertTrue(puff.cell() >= 0 && puff.cell() < ShroudedMawRenderer.SHEET_SIDE * ShroudedMawRenderer.SHEET_SIDE);
            }
        }
    }

    @Test
    void darkPuffsStayInsideTheirShareOfTheShroud() {
        for (int index = 0; index < ShroudedMawRenderer.DARK_PUFFS; index++) {
            for (float atSeconds = 0f; atSeconds < 60f; atSeconds += 0.7f) {
                ShroudedMawRenderer.Puff puff = ShroudedMawRenderer.darkPuff(index, atSeconds);
                assertTrue(ellipseShare(puff) <= ShroudedMawRenderer.DARK_RADIUS_SHARE + 0.0001f);
                assertEquals(0f, puff.flash());
            }
        }
    }

    @Test
    void aVisiblePuffNeverJumpsBetweenFrames() {
        float largestStep = 0f;
        for (int index = 0; index < ShroudedMawRenderer.SHROUD_PUFFS; index++) {
            ShroudedMawRenderer.Puff previous = ShroudedMawRenderer.shroudPuff(index, 0f);
            for (float atSeconds = FRAME_SECONDS; atSeconds < 30f; atSeconds += FRAME_SECONDS) {
                ShroudedMawRenderer.Puff current = ShroudedMawRenderer.shroudPuff(index, atSeconds);
                if (previous.alpha() > 0.05f && current.alpha() > 0.05f) {
                    largestStep = Math.max(largestStep, (float) Math.hypot(current.side() - previous.side(), current.forward() - previous.forward()));
                }
                previous = current;
            }
        }
        assertTrue(largestStep < 0.005f, "largest step " + largestStep + " body lengths");
    }

    @Test
    void onlyAFewPuffsFlashAtOnce() {
        int flashing = 0;
        int samples = 0;
        for (float atSeconds = 0f; atSeconds < 120f; atSeconds += 0.5f) {
            for (int index = 0; index < ShroudedMawRenderer.SHROUD_PUFFS; index++) {
                if (ShroudedMawRenderer.shroudPuff(index, atSeconds).flash() > 0f) {
                    flashing++;
                }
            }
            samples++;
        }
        float meanFlashing = flashing / (float) samples;
        assertTrue(meanFlashing > 0.5f && meanFlashing < 8f, "mean flashing puffs " + meanFlashing);
    }

    @Test
    void theBodyWarpStaysWithinItsAmplitude() {
        for (int vertex = 0; vertex < 25; vertex++) {
            for (float atSeconds = 0f; atSeconds < 30f; atSeconds += 0.3f) {
                assertTrue(Math.abs(ShroudedMawRenderer.warp(vertex, 0, atSeconds)) <= 1f);
                assertTrue(Math.abs(ShroudedMawRenderer.warp(vertex, 1, atSeconds)) <= 1f);
            }
        }
    }

    @Test
    void theHashSpreadsOverTheUnitInterval() {
        float smallest = 1f;
        float largest = 0f;
        float sum = 0f;
        int count = 0;
        for (int index = 0; index < 200; index++) {
            for (int life = -5; life < 5; life++) {
                float value = ShroudedMawRenderer.hash(index, life, 3);
                assertTrue(value >= 0f && value < 1f);
                smallest = Math.min(smallest, value);
                largest = Math.max(largest, value);
                sum += value;
                count++;
            }
        }
        assertTrue(smallest < 0.01f && largest > 0.99f);
        assertEquals(0.5f, sum / count, 0.03f);
    }
}
