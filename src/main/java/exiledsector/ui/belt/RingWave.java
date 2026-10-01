package exiledsector.ui.belt;

import exiledsector.ui.util.UnitCircle;

final class RingWave {

    static final RingWave NONE = new RingWave(0, 0f, 0f);

    private final int cyclesPerTurn;
    private final float amplitude;
    private final float cosPhase;
    private final float sinPhase;

    RingWave(int cyclesPerTurn, float phaseRad, float amplitude) {
        this.cyclesPerTurn = cyclesPerTurn;
        this.amplitude = amplitude;
        this.cosPhase = (float) Math.cos(phaseRad);
        this.sinPhase = (float) Math.sin(phaseRad);
    }

    float cosAt(UnitCircle circle, int segment) {
        if (amplitude == 0f) return 0f;
        int index = cyclesPerTurn * segment % circle.segments();
        return amplitude * (cosPhase * circle.cos(index) - sinPhase * circle.sin(index));
    }

    float sinAt(UnitCircle circle, int segment) {
        if (amplitude == 0f) return 0f;
        int index = cyclesPerTurn * segment % circle.segments();
        return amplitude * (sinPhase * circle.cos(index) + cosPhase * circle.sin(index));
    }
}
