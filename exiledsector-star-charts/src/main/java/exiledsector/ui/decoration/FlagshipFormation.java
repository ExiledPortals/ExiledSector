package exiledsector.ui.decoration;

import java.util.ArrayList;
import java.util.List;

final class FlagshipFormation {

    static final float LEAD_FORWARD_SHARE = 0.8f;
    static final float FLANK_LATERAL_SHARE = 0.5f;
    static final float FLANK_WIDENING_SHARE = 0.08f;
    static final float FLANK_FORWARD_SHARE = 0.25f;
    static final float FLANK_FALL_BACK_SHARE = 0.3f;

    private FlagshipFormation() {
    }

    record Slot(float forward, float lateral) implements EscortPath {

        float reach() {
            return (float) Math.hypot(forward, lateral);
        }

        @Override
        public float xAt(float seconds, float headingDeg) {
            double heading = Math.toRadians(headingDeg);
            return forward * (float) Math.cos(heading) - lateral * (float) Math.sin(heading);
        }

        @Override
        public float yAt(float seconds, float headingDeg) {
            double heading = Math.toRadians(headingDeg);
            return forward * (float) Math.sin(heading) + lateral * (float) Math.cos(heading);
        }

        @Override
        public float facingAt(float seconds, float headingDeg) {
            return headingDeg;
        }
    }

    static List<Slot> slots(int escorts, float radius) {
        List<Slot> slots = new ArrayList<>(Math.max(0, escorts));
        if (escorts >= 3 && escorts % 2 == 1) {
            slots.add(new Slot(radius * LEAD_FORWARD_SHARE, 0f));
        }
        for (int flank = 0; slots.size() < escorts; flank++) {
            int rank = flank / 2;
            float side = flank % 2 == 0 ? 1f : -1f;
            float lateral = radius * (FLANK_LATERAL_SHARE + FLANK_WIDENING_SHARE * rank);
            float forward = radius * (FLANK_FORWARD_SHARE - FLANK_FALL_BACK_SHARE * rank);
            slots.add(new Slot(forward, side * lateral));
        }
        return slots;
    }
}
