package exiledsector.charts.tree;

import java.util.Arrays;

final class AtlasWheelSpiral {

    private static final double ENTRY_TURN_RATE = 0.25;
    private static final double STEEPEST_DEPARTURE = Math.toRadians(80);
    private static final double MAX_ENTRY_SHARE_OF_FIRST_GAP = 0.85;
    private static final int PITCH_SETTLING_ROUNDS = 4;
    private static final double LEVEL_PITCH = 1e-6;
    private static final double FINEST_SAMPLE_STEP = 1e-4;

    private final float centreX;
    private final float centreY;
    private final double entryAngle;
    private final int turn;
    private final double[] sweeps;
    private final double[] radii;
    private final double[] logRadii;
    private final double[] logSlopes;
    private final double departurePitch;
    private final double pitchFall;
    private final double entryDropScale;
    private final double plungeStartSlope;

    private AtlasWheelSpiral(float centreX, float centreY, double entryAngle, int turn, double[] sweeps, double[] radii,
                             double departurePitch, double hugPitch) {
        this.centreX = centreX;
        this.centreY = centreY;
        this.entryAngle = entryAngle;
        this.turn = turn;
        this.sweeps = sweeps;
        this.radii = radii;
        int orbits = sweeps.length - 1;
        this.logRadii = new double[orbits];
        for (int knot = 0; knot < orbits; knot++) {
            logRadii[knot] = Math.log(radii[knot]);
        }
        this.logSlopes = monotoneLogSlopes(sweeps, logRadii, -Math.tan(hugPitch));
        this.departurePitch = departurePitch;
        this.pitchFall = (departurePitch - hugPitch) / sweeps[1];
        this.entryDropScale = (logRadii[0] - logRadii[1]) / entryDrop(sweeps[1]);
        this.plungeStartSlope = radii[orbits - 1] * logSlopes[orbits - 1];
    }

    static AtlasWheelSpiral wound(float centreX, float centreY, double entryAngle, int turn, double departurePitch, double hugRadius,
                                  double[] orbitSweeps, double[] orbitRadii) {
        double drop = Math.log(orbitRadii[0] / hugRadius);
        double firstOrbitSweep = orbitSweeps[1];
        double hugPitch = 0;
        double entry = entrySweep(drop, departurePitch, hugPitch);
        for (int round = 0; round < PITCH_SETTLING_ROUNDS; round++) {
            entry = Math.min(entrySweep(drop, departurePitch, hugPitch), firstOrbitSweep * MAX_ENTRY_SHARE_OF_FIRST_GAP);
            hugPitch = Math.min(departurePitch, Math.atan(Math.log(hugRadius / orbitRadii[1]) / (firstOrbitSweep - entry)));
        }
        double[] sweeps = new double[orbitSweeps.length + 1];
        double[] radii = new double[orbitRadii.length + 1];
        sweeps[1] = entry;
        radii[0] = orbitRadii[0];
        radii[1] = hugRadius;
        for (int knot = 1; knot < orbitSweeps.length; knot++) {
            sweeps[knot + 1] = orbitSweeps[knot];
            radii[knot + 1] = orbitRadii[knot];
        }
        return new AtlasWheelSpiral(centreX, centreY, entryAngle, turn, sweeps, radii, departurePitch, hugPitch);
    }

    static double longestEntrySweep(double entryRadius, double hugRadius, double departurePitch) {
        return entrySweep(Math.log(entryRadius / hugRadius), departurePitch, 0);
    }

    static double steepestDeparture(double entryRadius, double hugRadius) {
        return Math.min(STEEPEST_DEPARTURE, Math.acos(Math.exp(-(1 - ENTRY_TURN_RATE) * Math.log(entryRadius / hugRadius))));
    }

    private static double entrySweep(double drop, double departurePitch, double hugPitch) {
        double fall = departurePitch - hugPitch;
        if (fall < LEVEL_PITCH) {
            return drop / Math.tan(departurePitch);
        }
        return drop * fall / Math.log(Math.cos(hugPitch) / Math.cos(departurePitch));
    }

    private double entryDrop(double sweep) {
        if (Math.abs(pitchFall) < LEVEL_PITCH) {
            return sweep * Math.tan(departurePitch);
        }
        return Math.log(Math.cos(departurePitch - pitchFall * sweep) / Math.cos(departurePitch)) / pitchFall;
    }

    private static double[] monotoneLogSlopes(double[] knots, double[] logValues, double hugSlope) {
        int orbits = logValues.length;
        double[] secants = new double[orbits];
        for (int knot = 0; knot + 1 < orbits; knot++) {
            secants[knot] = (logValues[knot + 1] - logValues[knot]) / (knots[knot + 1] - knots[knot]);
        }
        secants[orbits - 1] = -1 / (knots[orbits] - knots[orbits - 1]);
        double[] tangents = new double[orbits];
        tangents[1] = hugSlope;
        for (int knot = 2; knot < orbits; knot++) {
            double spanBefore = knots[knot] - knots[knot - 1];
            double spanAfter = knots[knot + 1] - knots[knot];
            double weightBefore = 2 * spanAfter + spanBefore;
            double weightAfter = spanAfter + 2 * spanBefore;
            tangents[knot] = (weightBefore + weightAfter) / (weightBefore / secants[knot - 1] + weightAfter / secants[knot]);
        }
        return tangents;
    }

    double totalSweep() {
        return sweeps[sweeps.length - 1];
    }

    double radiusAt(double sweep) {
        if (sweep <= sweeps[1]) {
            return radii[0] * Math.exp(-entryDrop(Math.max(0, sweep)) * entryDropScale);
        }
        int segment = 1;
        while (segment < sweeps.length - 2 && sweep > sweeps[segment + 1]) {
            segment++;
        }
        double span = sweeps[segment + 1] - sweeps[segment];
        double share = Math.min(1, (sweep - sweeps[segment]) / span);
        if (segment == sweeps.length - 2) {
            return Math.max(0, hermite(share, span, radii[segment], plungeStartSlope, 0, -radii[segment] / span));
        }
        return Math.exp(hermite(share, span, logRadii[segment], logSlopes[segment], logRadii[segment + 1], logSlopes[segment + 1]));
    }

    private static double hermite(double share, double span, double fromValue, double fromSlope, double toValue, double toSlope) {
        double shareSquared = share * share;
        double shareCubed = shareSquared * share;
        return (2 * shareCubed - 3 * shareSquared + 1) * fromValue
                + (shareCubed - 2 * shareSquared + share) * span * fromSlope
                + (-2 * shareCubed + 3 * shareSquared) * toValue
                + (shareCubed - shareSquared) * span * toSlope;
    }

    double[] sampleSweeps(double fromSweep, double toSweep, double largestStep, double longestStep) {
        double direction = Math.signum(toSweep - fromSweep);
        double[] samples = new double[32];
        int count = 0;
        samples[count++] = fromSweep;
        double at = fromSweep;
        while ((toSweep - at) * direction > 0) {
            double remaining = Math.abs(toSweep - at);
            double step = Math.min(largestStep, remaining);
            while (step > FINEST_SAMPLE_STEP && distanceBetween(at, at + direction * step) > longestStep) {
                step /= 2;
            }
            at = step >= remaining ? toSweep : at + direction * step;
            if (count == samples.length) {
                samples = Arrays.copyOf(samples, count * 2);
            }
            samples[count++] = at;
        }
        return Arrays.copyOf(samples, count);
    }

    private double distanceBetween(double fromSweep, double toSweep) {
        return Math.hypot(xAt(toSweep) - xAt(fromSweep), yAt(toSweep) - yAt(fromSweep));
    }

    float xAt(double sweep) {
        return centreX + (float) (radiusAt(sweep) * Math.cos(entryAngle + turn * sweep));
    }

    float yAt(double sweep) {
        return centreY + (float) (radiusAt(sweep) * Math.sin(entryAngle + turn * sweep));
    }
}
