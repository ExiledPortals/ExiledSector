package exiledsector.ui.atlas;

final class AtlasWheelRoute {

    static final int APPROACH_POINTS = 48;
    static final int SPIRAL_POINTS = 40;
    static final double SWITCH_MARGIN = Math.toRadians(30.0);
    static final double SPIRAL_SLACK = Math.toRadians(40.0);
    static final float BLEND_SECONDS = 0.9f;
    private static final int LEAD_POINTS = 8;
    private static final double FULL_TURN = Math.PI * 2.0;

    record Wheel(float highwayX, float highwayY, float centreX, float centreY, float outerX, float outerY, float innerX, float innerY) {
    }

    final float[] approachXs = new float[APPROACH_POINTS];
    final float[] approachYs = new float[APPROACH_POINTS];
    final float[] spiralXs = new float[SPIRAL_POINTS];
    final float[] spiralYs = new float[SPIRAL_POINTS];
    private final float[] blendXs = new float[Math.max(APPROACH_POINTS, SPIRAL_POINTS)];
    private final float[] blendYs = new float[Math.max(APPROACH_POINTS, SPIRAL_POINTS)];
    private Wheel wheel;
    private Approach approach;
    private int wrapSide = 1;
    private int fromWrapSide = 1;
    private float wrapBlend = 1f;
    private int spiralSide = 1;
    private int fromSpiralSide = 1;
    private double spiralSweep = Double.NaN;
    private double fromSpiralSweep;
    private double lastSpiralTurn;
    private float spiralBlend = 1f;

    void advance(float amount) {
        wrapBlend = Math.min(1f, wrapBlend + amount / BLEND_SECONDS);
        spiralBlend = Math.min(1f, spiralBlend + amount / BLEND_SECONDS);
    }

    int spiralSide() {
        return spiralSide;
    }

    float bendSide() {
        return fromSpiralSide + (spiralSide - fromSpiralSide) * eased(spiralBlend);
    }

    boolean blending() {
        return wrapBlend < 1f || spiralBlend < 1f;
    }

    void update(Wheel next) {
        boolean first = wheel == null;
        wheel = next;
        approach = Approach.of(next);
        boolean wrapping = approach.wraps();
        if (first) {
            wrapSide = approach.delta() >= 0.0 ? 1 : -1;
            fromWrapSide = wrapSide;
        }
        if (wrapping && wrapBlend >= 1f && approach.sweep(wrapSide) > approach.sweep(-wrapSide) + SWITCH_MARGIN) {
            fromWrapSide = wrapSide;
            wrapSide = -wrapSide;
            wrapBlend = 0f;
        } else if (!wrapping && wrapBlend >= 1f) {
            wrapSide = approach.delta() >= 0.0 ? 1 : -1;
            fromWrapSide = wrapSide;
        }
        updateSpiral(wrapping ? wrapSide : 0);
    }

    private void updateSpiral(int wantedSide) {
        double relative = innerTurn() - outerTurn();
        if (Double.isNaN(spiralSweep)) {
            spiralSide = wantedSide != 0 ? wantedSide : Math.IEEEremainder(relative, FULL_TURN) >= 0.0 ? 1 : -1;
            fromSpiralSide = spiralSide;
            spiralSweep = sweepToward(spiralSide, relative);
            fromSpiralSweep = spiralSweep;
        } else {
            double change = Math.IEEEremainder(relative - lastSpiralTurn, FULL_TURN);
            spiralSweep += change;
            fromSpiralSweep += change;
        }
        lastSpiralTurn = relative;
        int side = wantedSide != 0 ? wantedSide : spiralSide;
        boolean sideChanged = side != spiralSide;
        boolean overstretched = spiralSide * spiralSweep < -SPIRAL_SLACK || spiralSide * spiralSweep > FULL_TURN + SPIRAL_SLACK;
        if (spiralBlend >= 1f && (sideChanged || overstretched)) {
            fromSpiralSide = spiralSide;
            fromSpiralSweep = spiralSweep;
            spiralSide = side;
            spiralSweep = sweepToward(spiralSide, relative);
            spiralBlend = 0f;
        }
    }

    void trace() {
        if (wheel == null) {
            return;
        }
        approach.trace(wrapSide, approachXs, approachYs);
        if (wrapBlend < 1f) {
            approach.trace(fromWrapSide, blendXs, blendYs);
            mix(blendXs, blendYs, approachXs, approachYs, APPROACH_POINTS, eased(wrapBlend));
        }
        double outerTurn = outerTurn();
        float outerRadius = (float) Math.hypot(wheel.outerX() - wheel.centreX(), wheel.outerY() - wheel.centreY());
        float innerRadius = (float) Math.hypot(wheel.innerX() - wheel.centreX(), wheel.innerY() - wheel.centreY());
        spiral(wheel, outerTurn, outerRadius, innerRadius, spiralSweep, spiralXs, spiralYs);
        if (spiralBlend < 1f) {
            spiral(wheel, outerTurn, outerRadius, innerRadius, fromSpiralSweep, blendXs, blendYs);
            mix(blendXs, blendYs, spiralXs, spiralYs, SPIRAL_POINTS, eased(spiralBlend));
        }
    }

    private double outerTurn() {
        return Math.atan2(wheel.outerY() - wheel.centreY(), wheel.outerX() - wheel.centreX());
    }

    private double innerTurn() {
        return Math.atan2(wheel.innerY() - wheel.centreY(), wheel.innerX() - wheel.centreX());
    }

    static double sweepToward(int side, double relative) {
        return side > 0 ? positive(relative) : -positive(-relative);
    }

    private static double positive(double angle) {
        double wrapped = angle % FULL_TURN;
        return wrapped < 0.0 ? wrapped + FULL_TURN : wrapped;
    }

    private static void spiral(Wheel wheel, double startTurn, float startRadius, float endRadius, double sweep, float[] xs, float[] ys) {
        for (int point = 0; point < SPIRAL_POINTS; point++) {
            float share = point / (float) (SPIRAL_POINTS - 1);
            double turn = startTurn + sweep * share;
            float radius = startRadius + (endRadius - startRadius) * share;
            xs[point] = wheel.centreX() + radius * (float) Math.cos(turn);
            ys[point] = wheel.centreY() + radius * (float) Math.sin(turn);
        }
        xs[SPIRAL_POINTS - 1] = wheel.innerX();
        ys[SPIRAL_POINTS - 1] = wheel.innerY();
    }

    private static void mix(float[] fromXs, float[] fromYs, float[] intoXs, float[] intoYs, int count, float share) {
        for (int point = 0; point < count; point++) {
            intoXs[point] = fromXs[point] + (intoXs[point] - fromXs[point]) * share;
            intoYs[point] = fromYs[point] + (intoYs[point] - fromYs[point]) * share;
        }
    }

    private static float eased(float share) {
        return share * share * (3f - 2f * share);
    }

    record Approach(Wheel wheel, double highwayTurn, double outerTurn, double tangent, double delta, float outerRadius) {

        static Approach of(Wheel wheel) {
            double highwayTurn = Math.atan2(wheel.highwayY() - wheel.centreY(), wheel.highwayX() - wheel.centreX());
            double outerTurn = Math.atan2(wheel.outerY() - wheel.centreY(), wheel.outerX() - wheel.centreX());
            float distance = (float) Math.hypot(wheel.highwayX() - wheel.centreX(), wheel.highwayY() - wheel.centreY());
            float outerRadius = (float) Math.hypot(wheel.outerX() - wheel.centreX(), wheel.outerY() - wheel.centreY());
            double tangent = distance > outerRadius ? Math.acos(outerRadius / distance) : -1.0;
            return new Approach(wheel, highwayTurn, outerTurn, tangent, Math.IEEEremainder(outerTurn - highwayTurn, FULL_TURN), outerRadius);
        }

        boolean wraps() {
            return tangent >= 0.0 && Math.abs(delta) > tangent;
        }

        double sweep(int side) {
            double touch = highwayTurn + side * tangent;
            return side > 0 ? positive(outerTurn - touch) : positive(touch - outerTurn);
        }

        void trace(int side, float[] xs, float[] ys) {
            if (!wraps()) {
                line(wheel.highwayX(), wheel.highwayY(), wheel.outerX(), wheel.outerY(), xs, ys, 0, APPROACH_POINTS, true);
                return;
            }
            double touch = highwayTurn + side * tangent;
            float touchX = wheel.centreX() + outerRadius * (float) Math.cos(touch);
            float touchY = wheel.centreY() + outerRadius * (float) Math.sin(touch);
            line(wheel.highwayX(), wheel.highwayY(), touchX, touchY, xs, ys, 0, LEAD_POINTS, false);
            double sweep = side * sweep(side);
            int arcPoints = APPROACH_POINTS - LEAD_POINTS;
            for (int point = 0; point < arcPoints; point++) {
                double turn = touch + sweep * point / (arcPoints - 1);
                xs[LEAD_POINTS + point] = wheel.centreX() + outerRadius * (float) Math.cos(turn);
                ys[LEAD_POINTS + point] = wheel.centreY() + outerRadius * (float) Math.sin(turn);
            }
            xs[APPROACH_POINTS - 1] = wheel.outerX();
            ys[APPROACH_POINTS - 1] = wheel.outerY();
        }

        private static void line(float fromX, float fromY, float toX, float toY, float[] xs, float[] ys, int first, int count, boolean reachEnd) {
            float steps = reachEnd ? count - 1f : count;
            for (int point = 0; point < count; point++) {
                xs[first + point] = fromX + (toX - fromX) * point / steps;
                ys[first + point] = fromY + (toY - fromY) * point / steps;
            }
        }
    }
}
