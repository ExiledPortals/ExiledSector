package exiledsector.ui.framework;

import exiledsector.ui.decoration.ShipAnchors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FrameworkSocketLayout {

    public enum Side {LEFT, RIGHT, ABOVE, BELOW}

    public enum Arrangement {BOTH_SIDES, LEFT_ABOVE_BELOW}

    public record Placement(Side side, int row, int rowsOnSide) {

        public boolean leftSide() {
            return side == Side.LEFT;
        }

        boolean vertical() {
            return side == Side.ABOVE || side == Side.BELOW;
        }
    }

    public record ShipExtent(float halfWidth, float halfLength) {

        public static ShipExtent square(float longestSidePixels) {
            return new ShipExtent(longestSidePixels / 2f, longestSidePixels / 2f);
        }
    }

    public static final float SOCKET_RADIUS = 88f;
    public static final float COLUMN_GAP = 110f;
    static final float VERTICAL_GAP = 70f;
    static final float END_SOCKET_SHIFT = 120f;
    static final float ELBOW_RUN = 50f;
    static final float ROW_SPACING = 270f;
    private static final float CENTRELINE_TOLERANCE = 1f;
    private static final int MIN_SOCKETS_FOR_ENDS = 3;

    private FrameworkSocketLayout() {
    }

    public static List<Placement> place(List<ShipAnchors.Anchor> anchors, Arrangement arrangement) {
        return arrangement == Arrangement.LEFT_ABOVE_BELOW ? placeLeftAboveBelow(anchors) : placeBothSides(anchors);
    }

    static List<Placement> placeBothSides(List<ShipAnchors.Anchor> anchors) {
        int socketCount = anchors.size();
        boolean[] leftSide = new boolean[socketCount];
        List<Integer> leftSlots = new ArrayList<>();
        List<Integer> rightSlots = new ArrayList<>();
        List<Integer> centreSlots = new ArrayList<>();
        for (int slot = 0; slot < socketCount; slot++) {
            float left = anchors.get(slot).left();
            if (left > CENTRELINE_TOLERANCE) {
                leftSlots.add(slot);
            } else if (left < -CENTRELINE_TOLERANCE) {
                rightSlots.add(slot);
            } else {
                centreSlots.add(slot);
            }
        }
        for (int slot : centreSlots) {
            (leftSlots.size() <= rightSlots.size() ? leftSlots : rightSlots).add(slot);
        }
        Comparator<Integer> nearestCentreline = Comparator.comparingDouble(slot -> Math.abs(anchors.get(slot).left()));
        while (Math.abs(leftSlots.size() - rightSlots.size()) > 1) {
            List<Integer> larger = leftSlots.size() > rightSlots.size() ? leftSlots : rightSlots;
            List<Integer> smaller = larger == leftSlots ? rightSlots : leftSlots;
            Integer moved = larger.stream().min(nearestCentreline).orElseThrow();
            larger.remove(moved);
            smaller.add(moved);
        }
        leftSlots.forEach(slot -> leftSide[slot] = true);
        Placement[] placements = new Placement[socketCount];
        for (List<Integer> sideSlots : List.of(leftSlots, rightSlots)) {
            List<Integer> topToBottom = frontToBack(anchors, sideSlots);
            for (int row = 0; row < topToBottom.size(); row++) {
                int slot = topToBottom.get(row);
                placements[slot] = new Placement(leftSide[slot] ? Side.LEFT : Side.RIGHT, row, topToBottom.size());
            }
        }
        return List.of(placements);
    }

    static List<Placement> placeLeftAboveBelow(List<ShipAnchors.Anchor> anchors) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < anchors.size(); slot++) {
            slots.add(slot);
        }
        List<Integer> frontToBack = frontToBack(anchors, slots);
        Placement[] placements = new Placement[anchors.size()];
        if (frontToBack.size() >= MIN_SOCKETS_FOR_ENDS) {
            placements[frontToBack.remove(0)] = new Placement(Side.ABOVE, 0, 1);
            placements[frontToBack.remove(frontToBack.size() - 1)] = new Placement(Side.BELOW, 0, 1);
        }
        for (int row = 0; row < frontToBack.size(); row++) {
            placements[frontToBack.get(row)] = new Placement(Side.LEFT, row, frontToBack.size());
        }
        return List.of(placements);
    }

    private static List<Integer> frontToBack(List<ShipAnchors.Anchor> anchors, List<Integer> slots) {
        List<Integer> ordered = new ArrayList<>(slots);
        ordered.sort(Comparator.comparingDouble(slot -> -anchors.get(slot).forward()));
        return ordered;
    }

    public static float socketX(Placement placement, float shipX, ShipExtent extent) {
        float offset = extent.halfWidth() + COLUMN_GAP + SOCKET_RADIUS;
        return switch (placement.side()) {
            case LEFT -> shipX - offset;
            case RIGHT -> shipX + offset;
            case ABOVE, BELOW -> shipX - Math.min(END_SOCKET_SHIFT, extent.halfWidth());
        };
    }

    public static float socketY(Placement placement, float shipY, ShipExtent extent) {
        float endOffset = extent.halfLength() + VERTICAL_GAP + SOCKET_RADIUS;
        return switch (placement.side()) {
            case LEFT, RIGHT -> shipY + ((placement.rowsOnSide() - 1) / 2f - placement.row()) * ROW_SPACING;
            case ABOVE -> shipY + endOffset;
            case BELOW -> shipY - endOffset;
        };
    }

    public static float edgeX(Placement placement, float socketX) {
        return switch (placement.side()) {
            case LEFT -> socketX + SOCKET_RADIUS;
            case RIGHT -> socketX - SOCKET_RADIUS;
            case ABOVE, BELOW -> socketX;
        };
    }

    public static float edgeY(Placement placement, float socketY) {
        return switch (placement.side()) {
            case LEFT, RIGHT -> socketY;
            case ABOVE -> socketY - SOCKET_RADIUS;
            case BELOW -> socketY + SOCKET_RADIUS;
        };
    }

    public static float elbowX(Placement placement, float socketX) {
        float edgeX = edgeX(placement, socketX);
        return placement.vertical() ? edgeX : edgeX + Math.signum(edgeX - socketX) * ELBOW_RUN;
    }

    public static float elbowY(Placement placement, float socketY) {
        float edgeY = edgeY(placement, socketY);
        return placement.vertical() ? edgeY + Math.signum(edgeY - socketY) * ELBOW_RUN : edgeY;
    }
}
