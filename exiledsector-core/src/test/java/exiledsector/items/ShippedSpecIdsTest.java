package exiledsector.items;

import exiledsector.ProjectFiles;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippedSpecIdsTest {

    private static final String BASELINE = "exiledsector-core/src/test/resources/shipped_spec_ids.csv";
    private static final String SPECIAL_ITEMS = "data/campaign/special_items.csv";
    private static final List<String> SPEC_FILES = List.of(SPECIAL_ITEMS, "data/campaign/commodities.csv",
            "data/campaign/abilities.csv", "data/campaign/industries.csv", "data/hullmods/hull_mods.csv",
            "data/hulls/ship_data.csv", "data/weapons/weapon_data.csv");
    private static final Set<String> SWEPT_FILES = Set.of(SPECIAL_ITEMS, "data/campaign/commodities.csv",
            "data/weapons/weapon_data.csv");

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    private static Set<String> shippedIds(String dataPath) throws Exception {
        Set<String> ids = new TreeSet<>();
        JSONArray rows = csvRows(dataPath);
        for (int i = 0; i < rows.length(); i++) {
            String id = rows.getJSONObject(i).optString("id", "").trim();
            if (!id.isEmpty() && !id.startsWith("#")) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static Set<String> baselineLines() throws Exception {
        List<String> lines = Files.readAllLines(ProjectFiles.projectRoot().resolve(BASELINE), StandardCharsets.UTF_8);
        return lines.stream().skip(1).map(String::trim).filter(line -> !line.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> tags(JSONObject row) {
        return Arrays.stream(row.optString("tags", "").split(",")).map(String::trim)
                .filter(tag -> !tag.isEmpty()).collect(Collectors.toSet());
    }

    @Test
    void everyShippedIdKeepsARowSoOldSavesLoad() throws Exception {
        Set<String> currentLines = new TreeSet<>();
        for (String file : SPEC_FILES) {
            shippedIds(file).forEach(id -> currentLines.add(file + "," + id));
        }
        List<String> missing = new ArrayList<>(baselineLines());
        missing.removeAll(currentLines);

        assertTrue(missing.isEmpty(), "Saves may still hold these ids, so removing their rows stops those saves loading. "
                + "Restore each row and tag it " + RetiredItems.RETIRED_ID_TAG + " (and hide_in_codex) instead: " + missing);
    }

    @Test
    void everyCurrentIdIsInTheBaseline() throws Exception {
        Set<String> baseline = baselineLines();
        List<String> unlisted = new ArrayList<>();
        for (String file : SPEC_FILES) {
            for (String id : shippedIds(file)) {
                if (!baseline.contains(file + "," + id)) {
                    unlisted.add(file + "," + id);
                }
            }
        }

        assertTrue(unlisted.isEmpty(), "Add these lines to " + BASELINE + ": " + unlisted);
    }

    @Test
    void retiredRowsAreHiddenAndOnlyWhereTheLoadSweepRemovesThem() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String file : SPEC_FILES) {
            JSONArray rows = csvRows(file);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Set<String> rowTags = tags(row);
                if (!rowTags.contains(RetiredItems.RETIRED_ID_TAG)) {
                    continue;
                }
                String id = row.optString("id");
                if (!SWEPT_FILES.contains(file)) {
                    problems.add(file + "," + id + " is retired where no load sweep removes it");
                }
                if (SPECIAL_ITEMS.equals(file) && !rowTags.contains("hide_in_codex")) {
                    problems.add(file + "," + id + " is retired but shows in the codex");
                }
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
