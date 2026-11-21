package exiledsector.charts.tree;

public final class AtlasLinkPath {

    private static final int ARC_POINTS = 24;
    private static final int SPIRAL_POINTS = 24;
    private static final double SPIRAL_HOOK_RADIANS = Math.toRadians(150.0);
    private static final double FULL_TURN = Math.PI * 2.0;
    private static final float CENTRE_RADIUS = 1f;
    private static final int BOW_POINTS = 16;
    public static final int MAX_WHEEL_SPIRAL_POINTS = 128;
    private static final double WHEEL_SPIRAL_STEP_RADIANS = Math.toRadians(8.0);
    private static final float WHEEL_SPIRAL_STEP_LENGTH = 40f;
    static final float BOW_SHARE = 0.12f;

    private final float[] xs;
    private final float[] ys;
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;

    private AtlasLinkPath(float[] xs, float[] ys) {
        this.xs = xs;
        this.ys = ys;
        float lowX = Float.MAX_VALUE;
        float lowY = Float.MAX_VALUE;
        float highX = -Float.MAX_VALUE;
        float highY = -Float.MAX_VALUE;
        for (int point = 0; point < xs.length; point++) {
            lowX = Math.min(lowX, xs[point]);
            lowY = Math.min(lowY, ys[point]);
            highX = Math.max(highX, xs[point]);
            highY = Math.max(highY, ys[point]);
        }
        this.minX = lowX;
        this.minY = lowY;
        this.maxX = highX;
        this.maxY = highY;
    }

    static AtlasLinkPath straight(float fromX, float fromY, float toX, float toY) {
        return new AtlasLinkPath(new float[]{fromX, toX}, new float[]{fromY, toY});
    }

    static AtlasLinkPath bowed(float fromX, float fromY, float toX, float toY, float centreX, float centreY) {
        float deltaX = toX - fromX;
        float deltaY = toY - fromY;
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length <= 0f) {
            return straight(fromX, fromY, toX, toY);
        }
        boolean outward = Math.hypot(toX - centreX, toY - centreY) > Math.hypot(fromX - centreX, fromY - centreY);
        float sense = outward ? -1f : 1f;
        float normalX = -deltaY / length * sense;
        float normalY = deltaX / length * sense;
        float bow = length * BOW_SHARE;
        float[] xs = new float[BOW_POINTS];
        float[] ys = new float[BOW_POINTS];
        for (int point = 0; point < BOW_POINTS; point++) {
            float share = (float) point / (BOW_POINTS - 1);
            float bulge = bow * (float) Math.sin(Math.PI * share);
            xs[point] = fromX + deltaX * share + normalX * bulge;
            ys[point] = fromY + deltaY * share + normalY * bulge;
        }
        return new AtlasLinkPath(xs, ys);
    }

    static AtlasLinkPath arc(float centreX, float centreY, float radius, float fromX, float fromY, float toX, float toY) {
        double fromTurn = Math.atan2(fromY - centreY, fromX - centreX);
        double sweep = Math.IEEEremainder(Math.atan2(toY - centreY, toX - centreX) - fromTurn, FULL_TURN);
        float[] xs = new float[ARC_POINTS];
        float[] ys = new float[ARC_POINTS];
        for (int point = 0; point < ARC_POINTS; point++) {
            double turn = fromTurn + sweep * point / (ARC_POINTS - 1);
            xs[point] = centreX + radius * (float) Math.cos(turn);
            ys[point] = centreY + radius * (float) Math.sin(turn);
        }
        return new AtlasLinkPath(xs, ys);
    }

    static AtlasLinkPath spiral(float centreX, float centreY, int turn, float fromX, float fromY, float toX, float toY) {
        double fromRadius = Math.hypot(fromX - centreX, fromY - centreY);
        double toRadius = Math.hypot(toX - centreX, toY - centreY);
        boolean inward = fromRadius >= toRadius;
        double outerRadius = inward ? fromRadius : toRadius;
        double innerRadius = inward ? toRadius : fromRadius;
        double outerAngle = inward ? Math.atan2(fromY - centreY, fromX - centreX) : Math.atan2(toY - centreY, toX - centreX);
        double innerAngle = innerRadius < CENTRE_RADIUS ? outerAngle + turn * SPIRAL_HOOK_RADIANS
                : inward ? Math.atan2(toY - centreY, toX - centreX) : Math.atan2(fromY - centreY, fromX - centreX);
        double sweep = turn * floorMod((innerAngle - outerAngle) * turn, FULL_TURN);
        float[] xs = new float[SPIRAL_POINTS];
        float[] ys = new float[SPIRAL_POINTS];
        for (int point = 0; point < SPIRAL_POINTS; point++) {
            double share = (double) point / (SPIRAL_POINTS - 1);
            double radius = outerRadius + (innerRadius - outerRadius) * share;
            double angle = outerAngle + sweep * share;
            int slot = inward ? point : SPIRAL_POINTS - 1 - point;
            xs[slot] = centreX + (float) (radius * Math.cos(angle));
            ys[slot] = centreY + (float) (radius * Math.sin(angle));
        }
        return new AtlasLinkPath(xs, ys);
    }

    static AtlasLinkPath alongSpiral(AtlasWheelSpiral spiral, double fromSweep, double toSweep) {
        double largestStep = WHEEL_SPIRAL_STEP_RADIANS;
        double longestStep = WHEEL_SPIRAL_STEP_LENGTH;
        double[] sweeps = spiral.sampleSweeps(fromSweep, toSweep, largestStep, longestStep);
        while (sweeps.length > MAX_WHEEL_SPIRAL_POINTS) {
            largestStep *= 1.5;
            longestStep *= 1.5;
            sweeps = spiral.sampleSweeps(fromSweep, toSweep, largestStep, longestStep);
        }
        float[] xs = new float[sweeps.length];
        float[] ys = new float[sweeps.length];
        for (int point = 0; point < sweeps.length; point++) {
            xs[point] = spiral.xAt(sweeps[point]);
            ys[point] = spiral.yAt(sweeps[point]);
        }
        return new AtlasLinkPath(xs, ys);
    }

    private static double floorMod(double value, double modulus) {
        return ((value % modulus) + modulus) % modulus;
    }

    public int size() {
        return xs.length;
    }

    public float x(int point) {
        return xs[point];
    }

    public float y(int point) {
        return ys[point];
    }

    public float minX() {
        return minX;
    }

    public float minY() {
        return minY;
    }

    public float maxX() {
        return maxX;
    }

    public float maxY() {
        return maxY;
    }
}
