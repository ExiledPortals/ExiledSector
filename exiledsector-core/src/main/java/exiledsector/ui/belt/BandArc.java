package exiledsector.ui.belt;

public record BandArc(int index, int count) {

    public static final BandArc WHOLE = new BandArc(0, 1);
    private static final int[] NO_VERTICES = new int[0];

    public static BandArc[] split(int count) {
        BandArc[] arcs = new BandArc[count];
        for (int arcIndex = 0; arcIndex < count; arcIndex++) {
            arcs[arcIndex] = new BandArc(arcIndex, count);
        }
        return arcs;
    }

    public boolean whole() {
        return count <= 1;
    }

    public float middleDegrees() {
        return (index + 0.5f) * 360f / count;
    }

    int[] vertexRange(float rotationDeg, int segments) {
        if (whole()) {
            return new int[]{0, segments};
        }
        int rotationShift = Math.round(-rotationDeg * segments / 360f);
        int first = rotationShift + Math.round(index * segments / (float) count);
        int last = rotationShift + Math.round((index + 1) * segments / (float) count);
        if (last <= first) {
            return NO_VERTICES;
        }
        int shift = Math.floorDiv(first, segments) * segments;
        return new int[]{first - shift, last - shift};
    }
}
