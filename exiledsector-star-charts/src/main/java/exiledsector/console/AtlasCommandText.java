package exiledsector.console;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasState;

import java.util.ArrayList;
import java.util.List;

final class AtlasCommandText {

    private AtlasCommandText() {
    }

    static String cell(AtlasCell cell) {
        return "(" + cell.gridX() + " " + cell.gridY() + ")";
    }

    static String node(AtlasNode node, AtlasState state) {
        List<String> parts = new ArrayList<>();
        parts.add(node.regionId());
        if (node.factionId() != null) {
            parts.add(node.factionId());
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
        if (node.phenomenonId() != null) {
            parts.add("phenomenon " + node.phenomenonId());
        }
        if (node.expansionId() != null) {
            parts.add("expansion " + node.expansionId());
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
