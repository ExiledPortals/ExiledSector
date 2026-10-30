package exiledsector.compat;

import exiledsector.ProjectFiles;
import exiledsector.items.WormholeTechItems;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultsOfKnowledgeTechRowsTest {

    private static final String DATABANK = "research_databank";

    private static JSONArray rows(String path) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(path), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

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
        JSONArray items = rows("data/campaign/special_items.csv");
        for (int index = 0; index < items.length(); index++) {
            specialItems.add(items.getJSONObject(index).optString("id"));
        }
        Map<String, JSONObject> techs = new HashMap<>();
        JSONArray techRows = rows("data/campaign/aotd_tech_options.csv");
        for (int index = 0; index < techRows.length(); index++) {
            JSONObject row = techRows.getJSONObject(index);
            techs.put(row.getString("id"), row);
        }
        List<List<Object>> expected = List.of(
                List.of(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID, WormholeTechItems.BLUEPRINT, 5),
                List.of(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID, WormholeTechItems.UPGRADE_ADVANCED, 10),
                List.of(VaultsOfKnowledgeCompat.GATEWAY_PERFECTED_RESEARCH_ID, WormholeTechItems.UPGRADE_PERFECTED, 15));
        for (List<Object> research : expected) {
            JSONObject row = techs.get((String) research.get(0));
            assertTrue(row != null, research.get(0) + " is missing");
            Map<String, Integer> costs = itemCosts(row.getString("itemReqToResearch"));
            assertEquals(Map.of(DATABANK, research.get(2), (String) research.get(1), 1), costs);
            assertTrue(specialItems.contains((String) research.get(1)), research.get(1) + " is not a special item");
            assertEquals(VaultsOfKnowledgeCompat.MOD_ID, row.getString("modId"));
        }
        assertEquals("none", techs.get(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID).getString("reqToResearchFirst"));
        assertTrue(techs.get(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID).getString("reqToResearchFirst")
                .contains(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID));
        assertTrue(techs.get(VaultsOfKnowledgeCompat.GATEWAY_PERFECTED_RESEARCH_ID).getString("reqToResearchFirst")
                .contains(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID));
    }
}
