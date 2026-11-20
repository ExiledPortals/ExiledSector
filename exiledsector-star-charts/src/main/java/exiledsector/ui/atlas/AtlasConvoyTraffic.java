package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.AtlasShipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

final class AtlasConvoyTraffic {

    static final float SHIP_SCALE_SHARE = 0.15f;
    private static final float CONVOY_RADIUS = 60f;
    private static final float CULL_RADIUS = 30f;
    static final float HIDDEN_BELOW_ZOOM = 0.3f;
    static final float SHOWN_FROM_ZOOM = 0.45f;

    private record Convoy(int portIndex, ConvoyRun run, AtlasShipGroup ships) {
    }

    private final List<Convoy> convoys = new ArrayList<>();

    AtlasConvoyTraffic(AtlasTreeBodies bodies) {
        for (Body port : bodies.bodies()) {
            List<List<String>> fleets = port.art().convoys();
            if (fleets.isEmpty() || port.wheel() == AtlasTreeBodies.NO_WHEEL) {
                continue;
            }
            ConvoyRoute route = ConvoyRoute.fromPortToWheelCentre(bodies, port);
            for (int order = 0; order < fleets.size(); order++) {
                ConvoyRun run = new ConvoyRun(route, order, fleets.size());
                long seed = 31L * port.nodeId().hashCode() + order;
                convoys.add(new Convoy(port.index(), run, new AtlasShipGroup(AtlasShipGroup.Mode.UNDER_WAY, fleets.get(order), CONVOY_RADIUS, seed)));
            }
        }
    }

    boolean isEmpty() {
        return convoys.isEmpty();
    }

    void advance(float amount, float seconds) {
        for (Convoy convoy : convoys) {
            convoy.ships().advance(amount, convoy.run().at(seconds).headingDegrees());
        }
    }

    void render(TreeViewport viewport, float seconds, IntToDoubleFunction portAlpha, float alpha) {
        float zoomShare = zoomVisibility(viewport.zoom());
        if (zoomShare <= 0f) {
            return;
        }
        float shipZoom = viewport.zoom() * SHIP_SCALE_SHARE;
        for (Convoy convoy : convoys) {
            ConvoyRun.Position position = convoy.run().at(seconds);
            if (position.visibility() <= 0f) {
                continue;
            }
            float screenX = viewport.screenX(position.x());
            float screenY = viewport.screenY(position.y());
            if (!viewport.isVisible(screenX, screenY, CULL_RADIUS * viewport.zoom())) {
                continue;
            }
            float shipAlpha = alpha * zoomShare * position.visibility() * (float) portAlpha.applyAsDouble(convoy.portIndex());
            convoy.ships().render(screenX, screenY, shipZoom, position.headingDegrees(), shipAlpha);
        }
    }

    static float zoomVisibility(float zoom) {
        return Math.max(0f, Math.min(1f, (zoom - HIDDEN_BELOW_ZOOM) / (SHOWN_FROM_ZOOM - HIDDEN_BELOW_ZOOM)));
    }
}
