package exiledsector.compat;

import exiledsector.charts.phasing.DisplacementTheorem;
import exiledsector.charts.phasing.PhaseDisplacementGate;
import exiledsector.items.starchart.WormholeTechItems;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.ProjectFiles.csvRows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultsOfKnowledgeTechRowsTest {

    private static final String DATABANK = "research_databank";
    private static final int VOK_LOWEST_TIER = 0;
    private static final int VOK_HIGHEST_TIER = 5;

    private static Map<String, Integer> itemCosts(String cell) {
        Map<String, Integer> costs = new HashMap<>();
        for (String pair : cell.split(",")) {
            String[] parts = pair.trim().split(":");
            assertEquals(2, parts.length, pair);
            costs.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
        return costs;
    }

    @Test
    void theGatewayResearchRowsCostDatabanksAndTheirOwnItems() throws Exception {
        Set<String> specialItems = new HashSet<>();
        JSONArray items = csvRows("data/campaign/special_items.csv");
        for (int index = 0; index < items.length(); index++) {
            specialItems.add(items.getJSONObject(index).optString("id"));
        }
        Map<String, JSONObject> techs = new HashMap<>();
        JSONArray techRows = csvRows("data/campaign/aotd_tech_options.csv");
        for (int index = 0; index < techRows.length(); index++) {
            JSONObject row = techRows.getJSONObject(index);
            techs.put(row.getString("id"), row);
        }
        List<List<Object>> expected = List.of(
                List.of(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID, WormholeTechItems.BLUEPRINT, 5),
                List.of(VaultsOfKnowledgeCompat.GATEWAY_ARRAY_RESEARCH_ID, WormholeTechItems.UPGRADE_ARRAY, 10),
                List.of(VaultsOfKnowledgeCompat.GATEWAY_NEXUS_RESEARCH_ID, WormholeTechItems.UPGRADE_NEXUS, 15),
                List.of(VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID, DisplacementTheorem.ITEM_ID, 20));
        for (List<Object> research : expected) {
            JSONObject row = techs.get((String) research.get(0));
            assertTrue(row != null, research.get(0) + " is missing");
            Map<String, Integer> costs = itemCosts(row.getString("itemReqToResearch"));
            assertEquals(Map.of(DATABANK, research.get(2), (String) research.get(1), 1), costs);
            assertTrue(specialItems.contains((String) research.get(1)), research.get(1) + " is not a special item");
            assertEquals(VaultsOfKnowledgeCompat.MOD_ID, row.getString("modId"));
        }
        assertEquals("none", techs.get(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID).getString("reqToResearchFirst"));
        assertTrue(techs.get(VaultsOfKnowledgeCompat.GATEWAY_ARRAY_RESEARCH_ID).getString("reqToResearchFirst")
                .contains(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID));
        assertTrue(techs.get(VaultsOfKnowledgeCompat.GATEWAY_NEXUS_RESEARCH_ID).getString("reqToResearchFirst")
                .contains(VaultsOfKnowledgeCompat.GATEWAY_ARRAY_RESEARCH_ID));
        JSONObject phaseGate = techs.get(VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID);
        assertEquals(VaultsOfKnowledgeCompat.GATEWAY_NEXUS_RESEARCH_ID, phaseGate.getString("reqToResearchFirst"));
        assertEquals(5, phaseGate.getInt("tier"));
        assertEquals(2, phaseGate.getInt("columnNumber"));
        assertEquals(9, phaseGate.getInt("rowNumber"));
        assertEquals(PhaseDisplacementGate.INDUSTRY_ID + ":industry", phaseGate.getString("rewards"));
    }

    @Test
    void everyResearchRowUsesATierVaultsOfKnowledgeHas() throws Exception {
        JSONArray techRows = csvRows("data/campaign/aotd_tech_options.csv");
        for (int index = 0; index < techRows.length(); index++) {
            JSONObject row = techRows.getJSONObject(index);
            int tier = row.getInt("tier");
            assertTrue(tier >= VOK_LOWEST_TIER && tier <= VOK_HIGHEST_TIER, row.getString("id") + " has tier " + tier);
        }
    }

    @Test
    void noTwoResearchRowsShareATierSlot() throws Exception {
        Set<String> slots = new HashSet<>();
        JSONArray techRows = csvRows("data/campaign/aotd_tech_options.csv");
        for (int index = 0; index < techRows.length(); index++) {
            JSONObject row = techRows.getJSONObject(index);
            String slot = row.getInt("tier") + "/" + row.getInt("columnNumber") + "/" + row.getInt("rowNumber");
            assertTrue(slots.add(slot), row.getString("id") + " shares slot " + slot);
        }
    }
}
