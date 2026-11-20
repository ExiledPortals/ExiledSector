package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.ui.TreeViewport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntToDoubleFunction;

final class AtlasConvoyTraffic {

    private record Convoy(int portIndex, ConvoyRun run, ConvoyLooks looks) {
    }

    private final List<Convoy> convoys = new ArrayList<>();
    private final ConvoyLongHauls longHauls;

    AtlasConvoyTraffic(AtlasTreeBodies bodies) {
        for (Body port : bodies.bodies()) {
            List<List<String>> fleets = port.art().convoys();
            if (fleets.isEmpty() || port.wheel() == AtlasTreeBodies.NO_WHEEL) {
                continue;
            }
            List<List<String>> haulers = port.art().haulers();
            ConvoyRoute route = ConvoyRoute.fromPortToWheelCentre(bodies, port);
            for (int order = 0; order < fleets.size(); order++) {
                ConvoyRun run = new ConvoyRun(route, order, fleets.size());
                long seed = 31L * port.nodeId().hashCode() + order;
                List<String> large = order < haulers.size() ? haulers.get(order) : List.of();
                convoys.add(new Convoy(port.index(), run, new ConvoyLooks(fleets.get(order), large, seed)));
            }
        }
        this.longHauls = new ConvoyLongHauls(bodies, new Random());
    }

    void advance(float amount, float seconds) {
        for (Convoy convoy : convoys) {
            convoy.looks().advance(amount, convoy.run().at(seconds).headingDegrees());
        }
        longHauls.advance(amount);
    }

    void render(TreeViewport viewport, float seconds, IntToDoubleFunction portAlpha, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        for (Convoy convoy : convoys) {
            float portShare = (float) portAlpha.applyAsDouble(convoy.portIndex());
            convoy.looks().render(viewport, convoy.run().at(seconds), alpha * portShare);
        }
        longHauls.render(viewport, portAlpha, alpha);
    }
}
