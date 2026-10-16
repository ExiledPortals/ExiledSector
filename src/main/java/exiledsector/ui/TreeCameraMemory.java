package exiledsector.ui;

import exiledsector.ui.hyperspace.HyperspaceCamera;

import java.util.HashMap;
import java.util.Map;

public final class TreeCameraMemory {

    private static final Map<String, View> VIEWS_BY_SHIP_ID = new HashMap<>();

    private TreeCameraMemory() {
    }

    static View forShip(String shipId) {
        return shipId == null ? new View() : VIEWS_BY_SHIP_ID.computeIfAbsent(shipId, id -> new View());
    }

    public static void forgetAll() {
        VIEWS_BY_SHIP_ID.clear();
    }

    static final class View {

        private float treeX;
        private float treeY;
        private float zoom;
        private boolean saved;

        boolean isSaved() {
            return saved;
        }

        void save(float viewTreeX, float viewTreeY, float viewZoom) {
            treeX = viewTreeX;
            treeY = viewTreeY;
            zoom = viewZoom;
            saved = true;
        }

        HyperspaceCamera camera() {
            return new HyperspaceCamera(treeX, treeY, zoom);
        }
    }
}
