package exiledsector.skills;

import exiledsector.ProjectFiles;
import exiledsector.data.CsvRows;
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
import java.util.HashSet;
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

    private static HullModConflicts loadConflicts() throws Exception {
        return HullModConflicts.fromRows(RealSkillData.conflictRows(ProjectFiles.projectRoot()));
    }

    private static Set<String> excludedHullMods(JSONObject type, HullModConflicts conflicts) throws Exception {
        Set<String> excluded = new LinkedHashSet<>(strings(type, "exclusiveHullMods"));
        Set<String> standIns = standInHullMods(type);
        excluded.addAll(conflicts.conflictsOfAny(standIns.isEmpty() ? excluded : standIns));
        return excluded;
    }

    private static Set<String> ownHullMods(JSONObject type) throws Exception {
        Set<String> own = new LinkedHashSet<>(strings(type, "phantomHullMods"));
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
        HullModConflicts conflicts = loadConflicts();
        List<String> gaps = new ArrayList<>();
        for (JSONObject type : types.values()) {
            Set<String> exclusiveHullMods = excludedHullMods(type, conflicts);
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

    @Test
    void noNodeListsItsOwnPhantomOrPassthroughHullModAsExclusive() throws Exception {
        List<String> redundant = new ArrayList<>();
        for (JSONObject type : loadTypes().values()) {
            for (String hullMod : standInHullMods(type)) {
                if (strings(type, "exclusiveHullMods").contains(hullMod)) {
                    redundant.add(type.getString("id") + " lists its own hull mod " + hullMod);
                }
            }
        }
        assertTrue(redundant.isEmpty(), String.join("\n", redundant));
    }

    @Test
    void noExplicitNodeExclusionIsAlreadyImpliedByTheHullModLists() throws Exception {
        Map<String, JSONObject> types = loadTypes();
        HullModConflicts conflicts = loadConflicts();
        List<String> redundant = new ArrayList<>();
        for (JSONObject type : types.values()) {
            for (String otherId : strings(type, "exclusiveSkillTypes")) {
                JSONObject other = types.get(otherId);
                if (other != null && (excludesAny(type, standInHullMods(other), conflicts) || excludesAny(other, standInHullMods(type), conflicts))) {
                    redundant.add(type.getString("id") + " lists node " + otherId + ", which its hull mod lists already exclude");
                }
            }
        }
        assertTrue(redundant.isEmpty(), String.join("\n", redundant));
    }

    private static boolean excludesAny(JSONObject type, Set<String> hullMods, HullModConflicts conflicts) throws Exception {
        Set<String> exclusive = excludedHullMods(type, conflicts);
        return hullMods.stream().anyMatch(exclusive::contains);
    }

    private static Set<String> standInHullMods(JSONObject type) throws Exception {
        Set<String> standIns = new LinkedHashSet<>(strings(type, "phantomHullMods"));
        if (type.has("vanillaHullMod")) {
            standIns.add(type.getString("vanillaHullMod"));
        }
        return standIns;
    }

    @Test
    void aNodeThatPlacesAHullModKeepsItsHullModConflictsInTheConflictTable() throws Exception {
        List<String> misplaced = new ArrayList<>();
        for (JSONObject type : loadTypes().values()) {
            if (!standInHullMods(type).isEmpty() && !strings(type, "exclusiveHullMods").isEmpty()) {
                misplaced.add(type.getString("id") + " lists " + strings(type, "exclusiveHullMods") + "; move them to " + HullModConflicts.DATA_PATH);
            }
        }
        assertTrue(misplaced.isEmpty(), String.join("\n", misplaced));
    }

    @Test
    void everyConflictRowNamesAHullModANodePlacesOrStandsInFor() throws Exception {
        Set<String> nodeHullMods = new HashSet<>();
        for (JSONObject type : loadTypes().values()) {
            Set<String> standIns = standInHullMods(type);
            nodeHullMods.addAll(standIns.isEmpty() ? strings(type, "exclusiveHullMods") : standIns);
        }
        List<String> dead = new ArrayList<>();
        JSONArray rows = RealSkillData.conflictRows(ProjectFiles.projectRoot());
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String hullMod = row.getString(HullModConflicts.HULL_MOD_COLUMN);
            for (String conflict : CsvRows.entries(row.getString(HullModConflicts.CONFLICTS_COLUMN))) {
                if (!nodeHullMods.contains(hullMod) && !nodeHullMods.contains(conflict)) {
                    dead.add(hullMod + " / " + conflict);
                }
            }
        }
        assertTrue(dead.isEmpty(), "No node places either hull mod of: " + dead);
    }

    @Test
    void everyConflictIsListedOnceAndEveryRowSaysWhichModItIsFor() throws Exception {
        Set<String> seen = new HashSet<>();
        Set<String> keys = new HashSet<>();
        List<String> problems = new ArrayList<>();
        JSONArray rows = RealSkillData.conflictRows(ProjectFiles.projectRoot());
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String hullMod = row.getString(HullModConflicts.HULL_MOD_COLUMN);
            if (!keys.add(hullMod)) {
                problems.add(hullMod + " has several rows; the game keeps only one");
            }
            List<String> conflicts = CsvRows.entries(row.getString(HullModConflicts.CONFLICTS_COLUMN));
            if (conflicts.isEmpty() || row.optString("notes").isBlank()) {
                problems.add(hullMod + " needs conflicts and notes");
            }
            for (String conflict : conflicts) {
                String pair = hullMod.compareTo(conflict) < 0 ? hullMod + " / " + conflict : conflict + " / " + hullMod;
                if (hullMod.equals(conflict) || !seen.add(pair)) {
                    problems.add("listed twice or with itself: " + pair);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
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
