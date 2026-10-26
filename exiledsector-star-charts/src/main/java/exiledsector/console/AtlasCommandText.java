package exiledsector.console;

import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;

import java.util.ArrayList;
import java.util.List;

final class AtlasCommandText {

    private AtlasCommandText() {
    }

    static String cell(AtlasCell cell) {
        return "(" + cell.gridX() + " " + cell.gridY() + ")";
    }

    static String node(AtlasGenerator generator, AtlasCell cell, AtlasState state) {
        AtlasNode node = generator.node(cell);
        List<String> parts = new ArrayList<>();
        parts.add(node.regionId());
        String owner = AtlasOpening.territoryOwnerOf(generator, state, cell);
        if (owner != null) {
            parts.add(state.capturedBy(cell) == null ? owner : owner + " (captured from " + node.factionId() + ")");
        }
        if (node.layoutId() != null) {
            parts.add(node.layoutId());
        }
        if (node.specialId() != null) {
            parts.add("site " + node.specialId());
        }
        if (node.powerfulBoss()) {
            parts.add("powerful boss");
        }
        List<String> anomalyIds = AtlasAnomalies.effective(generator, state, cell);
        if (!anomalyIds.isEmpty()) {
            parts.add("anomalies " + String.join(" ", anomalyIds));
        }
        if (node.infestationId() != null) {
            parts.add(state.has(node.cell(), AtlasState.CLEANSED) ? "cleansed" : "infested from " + node.infestationId());
        }
        parts.add(stateOf(node.cell(), state));
        return cell(node.cell()) + " " + String.join(", ", parts);
    }

    static String stateOf(AtlasCell cell, AtlasState state) {
        if (state.isCompleted(cell)) {
            return "completed";
        }
        if (state.has(cell, AtlasState.ATTEMPTED)) {
            return "attempted";
        }
        if (state.has(cell, AtlasState.MARKERS_SPENT)) {
            return "markers spent";
        }
        return "open";
    }
}
