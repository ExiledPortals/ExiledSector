package exiledsector.skills.npc;

import exiledsector.ProjectFiles;
import exiledsector.skills.HullModConflicts;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.loader.SkillTypeLoader;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RealSkillData {

    public static final Path TYPES_FILE = Path.of("data/skilltrees/skill_types.json");
    public static final Path TREE_FILE = Path.of("data/skilltrees/ship_skill_tree.json");
    public static final Path CONFLICTS_FILE = Path.of(HullModConflicts.DATA_PATH);

    private RealSkillData() {
    }

    public static void load() throws IOException, JSONException {
        load(ProjectFiles.projectRoot());
    }

    public static void load(Path projectRoot) throws IOException, JSONException {
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(ProjectFiles.readJson(projectRoot.resolve(TYPES_FILE)),
                HullModConflicts.fromRows(conflictRows(projectRoot)));
        List<SkillNode> nodes = SkillTreeLoader.parseNodes(ProjectFiles.readJson(projectRoot.resolve(TREE_FILE)), types);
        clear();
        types.values().forEach(SkillTree::registerType);
        for (SkillNode node : nodes) {
            SkillTree.register(node);
        }
    }

    public static JSONArray conflictRows(Path projectRoot) throws IOException, JSONException {
        String csv = Files.readString(projectRoot.resolve(CONFLICTS_FILE), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    public static void clear() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }
}
