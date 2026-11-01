package exiledsector.ui.section;

import exiledsector.ui.decoration.HullPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SectionLayout {

    public enum Side {LEFT, ABOVE, BELOW}

    public record Placement(Side side, int row, int rowsOnSide) {

        boolean vertical() {
            return side == Side.ABOVE || side == Side.BELOW;
        }
    }

    public record ShipExtent(float halfWidth, float halfLength) {

        public static ShipExtent square(float longestSidePixels) {
            return new ShipExtent(longestSidePixels / 2f, longestSidePixels / 2f);
        }
    }

    public static final float SECTION_RADIUS = 88f;
    public static final float COLUMN_GAP = 110f;
    static final float VERTICAL_GAP = 70f;
    static final float END_SECTION_SHIFT = 120f;
    static final float ELBOW_RUN = 50f;
    static final float ROW_SPACING = 270f;
    private static final int MIN_SECTIONS_FOR_ENDS = 3;

    private SectionLayout() {
    }

    public static List<Placement> place(List<HullPoint> anchors) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < anchors.size(); slot++) {
            slots.add(slot);
        }
        List<Integer> frontToBack = frontToBack(anchors, slots);
        Placement[] placements = new Placement[anchors.size()];
        if (frontToBack.size() >= MIN_SECTIONS_FOR_ENDS) {
            placements[frontToBack.remove(0)] = new Placement(Side.ABOVE, 0, 1);
            placements[frontToBack.remove(frontToBack.size() - 1)] = new Placement(Side.BELOW, 0, 1);
        }
        for (int row = 0; row < frontToBack.size(); row++) {
            placements[frontToBack.get(row)] = new Placement(Side.LEFT, row, frontToBack.size());
        }
        return List.of(placements);
    }

    private static List<Integer> frontToBack(List<HullPoint> anchors, List<Integer> slots) {
        List<Integer> ordered = new ArrayList<>(slots);
        ordered.sort(Comparator.comparingDouble(slot -> -anchors.get(slot).forward()));
        return ordered;
    }

    public static float sectionX(Placement placement, float shipX, ShipExtent extent) {
        float offset = extent.halfWidth() + COLUMN_GAP + SECTION_RADIUS;
        return switch (placement.side()) {
            case LEFT -> shipX - offset;
            case ABOVE, BELOW -> shipX - Math.min(END_SECTION_SHIFT, extent.halfWidth());
        };
    }

    public static float sectionY(Placement placement, float shipY, ShipExtent extent) {
        float endOffset = extent.halfLength() + VERTICAL_GAP + SECTION_RADIUS;
        return switch (placement.side()) {
            case LEFT -> shipY + ((placement.rowsOnSide() - 1) / 2f - placement.row()) * ROW_SPACING;
            case ABOVE -> shipY + endOffset;
            case BELOW -> shipY - endOffset;
        };
    }

    public static float edgeX(Placement placement, float sectionX) {
        return switch (placement.side()) {
            case LEFT -> sectionX + SECTION_RADIUS;
            case ABOVE, BELOW -> sectionX;
        };
    }

    public static float edgeY(Placement placement, float sectionY) {
        return switch (placement.side()) {
            case LEFT -> sectionY;
            case ABOVE -> sectionY - SECTION_RADIUS;
            case BELOW -> sectionY + SECTION_RADIUS;
        };
    }

    public static float elbowX(Placement placement, float sectionX) {
        float edgeX = edgeX(placement, sectionX);
        return placement.vertical() ? edgeX : edgeX + Math.signum(edgeX - sectionX) * ELBOW_RUN;
    }

    public static float elbowY(Placement placement, float sectionY) {
        float edgeY = edgeY(placement, sectionY);
        return placement.vertical() ? edgeY + Math.signum(edgeY - sectionY) * ELBOW_RUN : edgeY;
    }
}
