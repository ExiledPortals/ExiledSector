package exiledsector.skills;

import exiledsector.skills.loader.WormholePairValidator;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillTreeConnectorDataTest {

    private static JSONObject tree;
    private static final Map<String, JSONObject> NODES = new HashMap<>();
    private static final Set<String> LINKS = new HashSet<>();

    @BeforeAll
    static void loadTree() throws Exception {
        tree = RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TREE_FILE));
        JSONArray nodes = tree.getJSONArray("nodes");
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            NODES.put(node.getString("id"), node);
        }
        for (JSONObject node : NODES.values()) {
            JSONArray connected = node.getJSONArray("connectedTo");
            for (int i = 0; i < connected.length(); i++) {
                LINKS.add(SkillTree.curveKey(node.getString("id"), connected.getString(i)));
            }
        }
    }

    private static List<String> deadEntries(String key) throws Exception {
        List<String> dead = new ArrayList<>();
        JSONArray entries = tree.optJSONArray(key);
        for (int i = 0; entries != null && i < entries.length(); i++) {
            String a = entries.getJSONObject(i).getString("a");
            String b = entries.getJSONObject(i).getString("b");
            if (!NODES.containsKey(a) || !NODES.containsKey(b)) {
                dead.add(a + " | " + b + " (missing node)");
            } else if (!LINKS.contains(SkillTree.curveKey(a, b))) {
                dead.add(a + " | " + b + " (not linked)");
            }
        }
        return dead;
    }

    @Test
    void everyConnectorCurveSitsOnALinkedPairOfExistingNodes() throws Exception {
        assertEquals(List.of(), deadEntries("connectorCurves"),
                "These curves in " + RealSkillData.TREE_FILE + " point at nodes that are gone or no longer linked");
    }

    @Test
    void everyHiddenConnectorSitsOnALinkedPairOfExistingNodes() throws Exception {
        assertEquals(List.of(), deadEntries("hiddenConnectors"),
                "These hidden connectors in " + RealSkillData.TREE_FILE + " point at nodes that are gone or no longer linked");
    }

    private static boolean lists(JSONObject node, String otherId) throws Exception {
        JSONArray connected = node.getJSONArray("connectedTo");
        for (int i = 0; i < connected.length(); i++) {
            if (connected.getString(i).equals(otherId)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void everyLinkIsListedOnBothNodes() throws Exception {
        List<String> oneWay = new ArrayList<>();
        for (JSONObject node : NODES.values()) {
            JSONArray connected = node.getJSONArray("connectedTo");
            for (int i = 0; i < connected.length(); i++) {
                JSONObject other = NODES.get(connected.getString(i));
                if (other == null) {
                    oneWay.add(node.getString("id") + " -> " + connected.getString(i) + " (missing node)");
                } else if (!lists(other, node.getString("id"))) {
                    oneWay.add(node.getString("id") + " -> " + connected.getString(i));
                }
            }
        }
        oneWay.sort(null);

        assertEquals(List.of(), oneWay, "These links in " + RealSkillData.TREE_FILE + " are only listed on one of their two nodes");
    }

    @Test
    void everyWormholeIsPairedWithAnExistingWormholeThatPairsBack() throws Exception {
        RealSkillData.load();
        try {
            assertEquals(List.of(), WormholePairValidator.findIssues(SkillTree.getAllNodes().values()));
        } finally {
            RealSkillData.clear();
        }
    }
}
