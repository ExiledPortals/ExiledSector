package exiledsector.skills.npc;

import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFocusTest {

    @AfterEach
    void tearDown() {
        NpcFocus.clear();
    }

    @Test
    void focusEntriesNeedAThemeTagAndAPositiveWeight() {
        Map<String, Float> focus = NpcFocus.parseFocus("hegemony", "armour:2; region_core:3; ballistic:-1; hull; flux:x; speed:1.5",
                NpcFocus.FACTION_PATH);

        assertEquals(Map.of("armour", 2f, "speed", 1.5f), focus);
    }

    @Test
    void factionAndShipSystemFocusMultiply() throws JSONException {
        NpcFocus.registerFactions(new JSONArray("[{\"faction\": \"hegemony\", \"focus\": \"armour:2; ballistic:2\"}]"));
        NpcFocus.registerShipSystems(new JSONArray("[{\"system\": \"damper\", \"focus\": \"armour:2\"}]"));

        assertEquals(Map.of("armour", 4f, "ballistic", 2f), NpcFocus.of("hegemony", "damper"));
        assertEquals(Map.of("armour", 2f), NpcFocus.of("independent", null, "damper"));
        assertTrue(NpcFocus.of(null, null, null).isEmpty());
    }

    @Test
    void everyShippedFocusEntryIsValid() throws Exception {
        assertShippedRowsValid(NpcFocus.FACTION_PATH, "faction");
        assertShippedRowsValid(NpcFocus.SHIP_SYSTEM_PATH, "system");
    }

    private static void assertShippedRowsValid(String path, String idColumn) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(path), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        assertTrue(rows.length() > 0, path);
        for (int i = 0; i < rows.length(); i++) {
            String id = rows.getJSONObject(i).optString(idColumn, "");
            String focusText = rows.getJSONObject(i).optString("focus", "");
            long entryCount = Arrays.stream(focusText.split(";")).filter(entry -> !entry.isBlank()).count();
            assertEquals(entryCount, NpcFocus.parseFocus(id, focusText, path).size(), path + " row " + id);
        }
    }
}
