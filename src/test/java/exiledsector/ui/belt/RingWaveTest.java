package exiledsector.ui.belt;

import exiledsector.ui.util.UnitCircle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RingWaveTest {

    private static final int SEGMENTS = 161;

    @Test
    void theWaveMatchesTheTrigItReplacesAtEverySegment() {
        UnitCircle circle = UnitCircle.of(SEGMENTS);
        float anglePerSegment = (float) (2 * Math.PI) / SEGMENTS;
        for (int cycles : new int[]{5, 6, 10}) {
            for (float phase : new float[]{0f, 1.3f, -2.9f}) {
                RingWave wave = new RingWave(cycles, phase, 7f);
                for (int segment = 0; segment < SEGMENTS; segment++) {
                    double angle = phase + segment * anglePerSegment * cycles;
                    assertEquals(Math.cos(angle) * 7f, wave.cosAt(circle, segment), 1e-3);
                    assertEquals(Math.sin(angle) * 7f, wave.sinAt(circle, segment), 1e-3);
                }
            }
        }
    }

    @Test
    void noWaveAddsNothing() {
        UnitCircle circle = UnitCircle.of(SEGMENTS);

        assertEquals(0f, RingWave.NONE.cosAt(circle, 17));
        assertEquals(0f, RingWave.NONE.sinAt(circle, 17));
    }
}
