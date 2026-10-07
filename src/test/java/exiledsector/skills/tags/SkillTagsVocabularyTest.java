package exiledsector.skills.tags;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTagsVocabularyTest {

    private static JSONArray loadArray(String path, String key) throws Exception {
        return new JSONObject(Files.readString(Path.of(path), StandardCharsets.UTF_8)).getJSONArray(key);
    }

    private static JSONArray types() throws Exception {
        return loadArray("data/skilltrees/skill_types.json", "skillTypes");
    }

    private static JSONArray nodes() throws Exception {
        return loadArray("data/skilltrees/ship_skill_tree.json", "nodes");
    }

    private static List<String> tags(JSONObject entry) throws Exception {
        List<String> tags = new ArrayList<>();
        JSONArray array = entry.optJSONArray("tags");
        for (int i = 0; array != null && i < array.length(); i++) {
            tags.add(array.getString(i));
        }
        return tags;
    }

    private static void collectUnknownTags(JSONArray entries, String kind, List<String> problems) throws Exception {
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            for (String tag : tags(entry)) {
                if (!SkillTags.ALL.contains(tag)) {
                    problems.add(kind + " " + entry.optString("id") + " has unknown tag " + tag);
                }
            }
        }
    }

    @Test
    void everyTagOnEveryTypeAndNodeIsInTheVocabulary() throws Exception {
        List<String> problems = new ArrayList<>();
        collectUnknownTags(types(), "type", problems);
        collectUnknownTags(nodes(), "node", problems);

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyNodeCarriesExactlyOneRegionTag() throws Exception {
        JSONArray nodes = nodes();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            List<String> regions = tags(node).stream().filter(SkillTags::isRegion).toList();
            if (regions.size() != 1) {
                problems.add("node " + node.optString("id") + " has region tags " + regions);
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void noTypeCarriesARegionTag() throws Exception {
        JSONArray types = types();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < types.length(); i++) {
            JSONObject type = types.getJSONObject(i);
            List<String> regions = tags(type).stream().filter(SkillTags::isRegion).toList();
            if (!regions.isEmpty()) {
                problems.add("type " + type.optString("id") + " has region tags " + regions);
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
