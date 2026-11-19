package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.AtlasShipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

final class AtlasConvoyTraffic {

    static final float SHIP_SCALE_SHARE = 0.5f;
    private static final float CONVOY_RADIUS = 24f;
    private static final float CULL_RADIUS = 60f;

    private record Convoy(int portIndex, ConvoyRun run, AtlasShipGroup ships) {
    }

    private final List<Convoy> convoys = new ArrayList<>();

    AtlasConvoyTraffic(AtlasTreeBodies bodies) {
        for (Body port : bodies.bodies()) {
            List<List<String>> fleets = port.art().convoys();
            if (fleets.isEmpty() || port.wheel() == AtlasTreeBodies.NO_WHEEL) {
                continue;
            }
            Body world = bodies.bodies().get(port.wheel());
            for (int order = 0; order < fleets.size(); order++) {
                ConvoyRun run = new ConvoyRun(port.x(), port.y(), world.x(), world.y(), order, fleets.size());
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
            float shipAlpha = alpha * position.visibility() * (float) portAlpha.applyAsDouble(convoy.portIndex());
            convoy.ships().render(screenX, screenY, shipZoom, position.headingDegrees(), shipAlpha);
        }
    }
}
