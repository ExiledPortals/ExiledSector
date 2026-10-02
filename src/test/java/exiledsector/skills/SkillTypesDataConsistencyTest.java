package exiledsector.skills;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTypesDataConsistencyTest {

    private static Map<String, JSONObject> loadTypes() throws Exception {
        String json = Files.readString(Path.of("data/skilltrees/skill_types.json"), StandardCharsets.UTF_8);
        JSONArray array = new JSONObject(json).getJSONArray("skillTypes");
        Map<String, JSONObject> types = new HashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject type = array.getJSONObject(i);
            types.put(type.getString("id"), type);
        }
        return types;
    }

    private static Set<String> strings(JSONObject type, String key) throws Exception {
        Set<String> values = new LinkedHashSet<>();
        JSONArray array = type.optJSONArray(key);
        for (int i = 0; array != null && i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }

    private static Set<String> ownHullMods(JSONObject type) throws Exception {
        Set<String> own = new LinkedHashSet<>(strings(type, "installedHullMods"));
        own.addAll(strings(type, "phantomHullMods"));
        if (type.has("vanillaHullMod")) {
            own.add(type.getString("vanillaHullMod"));
        }
        JSONArray unlocks = type.optJSONArray("unlockConditions");
        for (int i = 0; unlocks != null && i < unlocks.length(); i++) {
            JSONObject unlock = unlocks.getJSONObject(i);
            if ("blueprint".equals(unlock.optString("type")) && "hullmod".equals(unlock.optString("category"))) {
                own.add(unlock.getString("id"));
            }
        }
        if (strings(type, "exclusiveHullMods").contains(type.getString("id"))) {
            own.add(type.getString("id"));
        }
        return own;
    }

    @Test
    void aNodeExclusiveWithAnotherNodeIsAlsoExclusiveWithThatNodesOwnHullMod() throws Exception {
        Map<String, JSONObject> types = loadTypes();
        List<String> gaps = new ArrayList<>();
        for (JSONObject type : types.values()) {
            Set<String> exclusiveHullMods = strings(type, "exclusiveHullMods");
            for (String otherId : strings(type, "exclusiveSkillTypes")) {
                JSONObject other = types.get(otherId);
                if (other == null) {
                    gaps.add(type.getString("id") + " excludes unknown node " + otherId);
                    continue;
                }
                for (String hullMod : ownHullMods(other)) {
                    if (!exclusiveHullMods.contains(hullMod)) {
                        gaps.add(type.getString("id") + " excludes node " + otherId + " but not its hull mod " + hullMod);
                    }
                }
            }
        }
        assertTrue(gaps.isEmpty(), String.join("\n", gaps));
    }

    private static Set<String> standInHullMods(JSONObject type) throws Exception {
        Set<String> standIns = new LinkedHashSet<>(strings(type, "installedHullMods"));
        standIns.addAll(strings(type, "phantomHullMods"));
        if (type.has("vanillaHullMod")) {
            standIns.add(type.getString("vanillaHullMod"));
        }
        return standIns;
    }

    @Test
    void everyVanillaHullModHasExactlyOneNodeAndEveryNodeStandsInForAtMostOneHullMod() throws Exception {
        Map<String, List<String>> nodesByHullMod = new HashMap<>();
        List<String> violations = new ArrayList<>();
        for (JSONObject type : loadTypes().values()) {
            Set<String> standIns = standInHullMods(type);
            if (standIns.size() > 1) {
                violations.add("node " + type.getString("id") + " stands in for several hull mods: " + standIns);
            }
            for (String hullMod : standIns) {
                nodesByHullMod.computeIfAbsent(hullMod, id -> new ArrayList<>()).add(type.getString("id"));
            }
        }
        nodesByHullMod.forEach((hullMod, nodes) -> {
            if (nodes.size() > 1) {
                violations.add("hull mod " + hullMod + " has several nodes: " + nodes.stream().sorted().toList());
            }
        });
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void everySkillTypeAndNodeTagIsInTheTagVocabulary() throws Exception {
        List<String> unknown = new ArrayList<>();
        for (JSONObject type : loadTypes().values()) {
            for (String tag : strings(type, "tags")) {
                if (!SkillTags.ALL.contains(tag)) {
                    unknown.add("type " + type.getString("id") + ": " + tag);
                }
            }
        }
        String tree = Files.readString(Path.of("data/skilltrees/ship_skill_tree.json"), StandardCharsets.UTF_8);
        JSONArray nodes = new JSONObject(tree).getJSONArray("nodes");
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            for (String tag : strings(node, "tags")) {
                if (!SkillTags.ALL.contains(tag)) {
                    unknown.add("node " + node.getString("id") + ": " + tag);
                }
            }
        }
        assertTrue(unknown.isEmpty(), "Tags outside the vocabulary: " + unknown);
    }

    @Test
    void npcBuildsConvertTheHeavyArmorHullModOnlyIntoTheHeavyArmorNode() throws Exception {
        RealSkillData.load();
        try {
            List<String> standIns = SkillTree.getAllTypes().values().stream()
                    .filter(type -> "heavyarmor".equals(type.getEquivalentHullModId()))
                    .map(SkillType::getId)
                    .toList();

            assertEquals(List.of("heavyarmor"), standIns);
        } finally {
            RealSkillData.clear();
        }
    }
}
