package exiledsector.charts.tree;

import org.json.JSONException;
import org.json.JSONObject;

final class AtlasTreeFixtures {

    static final String TYPES = """
            { "skillTypes": [
              { "id": "gate", "name": "Gate", "icon": "graphics/icons/gate.png", "tier": "ROOT" },
              { "id": "salvage", "name": "Salvage", "icon": "graphics/icons/salvage.png",
                "effects": [ { "effect": "CHART_ITEM_PERCENT", "magnitude": 10 }, { "effect": "NOT_A_CHART_EFFECT", "magnitude": 5 } ] },
              { "id": "hoard", "name": "Hoard", "tier": "NOTABLE", "flavour": "Crates everywhere.", "tags": ["loot"] },
              { "id": "jump", "name": "Jump", "tier": "WORMHOLE" }
            ] }
            """;

    static final String TREE = """
            { "volumes": ["salvors", "far_reach"],
              "nodes": [
                { "id": "root", "type": "gate", "connectedTo": ["a"], "x": 0, "y": 0 },
                { "id": "a", "type": "salvage", "connectedTo": ["root", "b", "d"], "x": 100, "y": 0, "tags": ["salvors"] },
                { "id": "b", "type": "salvage", "connectedTo": ["a", "c", "w1"], "x": 200, "y": 0, "tags": ["salvors"] },
                { "id": "c", "type": "hoard", "connectedTo": ["b"], "x": 300, "y": 0, "tags": ["salvors"] },
                { "id": "d", "type": "salvage", "connectedTo": ["a"], "x": 100, "y": 100, "tags": ["salvors"] },
                { "id": "w1", "type": "jump", "connectedTo": ["b", "w2"], "x": 200, "y": 100, "pairedWith": "w2", "tags": ["salvors"] },
                { "id": "w2", "type": "jump", "connectedTo": ["w1", "e"], "x": 5000, "y": 5000, "pairedWith": "w1", "tags": ["far_reach"] },
                { "id": "e", "type": "hoard", "connectedTo": ["w2"], "x": 5100, "y": 5000, "tags": ["far_reach", "loot"] }
              ],
              "connectorCurves": [ { "a": "a", "b": "b", "controlX": 150, "controlY": 40 } ],
              "hiddenConnectors": [ { "a": "w1", "b": "w2" } ],
              "decorations": [ { "kind": "star", "id": "salvors_star", "x": 150, "y": 50, "radius": 300, "tags": ["salvors"] } ]
            }
            """;

    private AtlasTreeFixtures() {
    }

    static AtlasTreeData data() {
        return parse(TYPES, TREE);
    }

    static AtlasTreeData parse(String typesJson, String treeJson) {
        try {
            return AtlasTreeData.parse(new JSONObject(typesJson), new JSONObject(treeJson));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
