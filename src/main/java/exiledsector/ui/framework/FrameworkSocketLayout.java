package exiledsector.ui.framework;

import exiledsector.ui.decoration.ShipAnchors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FrameworkSocketLayout {

    public record Placement(boolean leftSide, int row, int rowsOnSide) {
    }

    public static final float SOCKET_RADIUS = 88f;
    static final float COLUMN_GAP = 110f;
    static final float ELBOW_RUN = 50f;
    static final float ROW_SPACING = 270f;
    private static final float CENTRELINE_TOLERANCE = 1f;

    private FrameworkSocketLayout() {
    }

    public static List<Placement> place(List<ShipAnchors.Anchor> anchors) {
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
            List<Integer> topToBottom = new ArrayList<>(sideSlots);
            topToBottom.sort(Comparator.comparingDouble(slot -> -anchors.get(slot).forward()));
            for (int row = 0; row < topToBottom.size(); row++) {
                int slot = topToBottom.get(row);
                placements[slot] = new Placement(leftSide[slot], row, topToBottom.size());
            }
        }
        return List.of(placements);
    }

    public static float socketX(Placement placement, float shipX, float longestSidePixels) {
        float offset = longestSidePixels / 2f + COLUMN_GAP + SOCKET_RADIUS;
        return placement.leftSide() ? shipX - offset : shipX + offset;
    }

    public static float socketY(Placement placement, float shipY) {
        return shipY + ((placement.rowsOnSide() - 1) / 2f - placement.row()) * ROW_SPACING;
    }

    public static float edgeX(Placement placement, float socketX) {
        return placement.leftSide() ? socketX + SOCKET_RADIUS : socketX - SOCKET_RADIUS;
    }

    public static float elbowX(Placement placement, float socketX) {
        return placement.leftSide() ? socketX + SOCKET_RADIUS + ELBOW_RUN : socketX - SOCKET_RADIUS - ELBOW_RUN;
    }
}
