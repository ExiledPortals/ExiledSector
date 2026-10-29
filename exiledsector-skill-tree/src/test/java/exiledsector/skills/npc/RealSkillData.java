package exiledsector.skills.npc;

import exiledsector.ProjectFiles;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.loader.SkillTypeLoader;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RealSkillData {

    public static final Path TYPES_FILE = Path.of("data/skilltrees/skill_types.json");
    public static final Path TREE_FILE = Path.of("data/skilltrees/ship_skill_tree.json");

    private RealSkillData() {
    }

    public static void load() throws IOException, JSONException {
        load(ProjectFiles.projectRoot());
    }

    public static void load(Path projectRoot) throws IOException, JSONException {
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(ProjectFiles.readJson(projectRoot.resolve(TYPES_FILE)));
        List<SkillNode> nodes = SkillTreeLoader.parseNodes(ProjectFiles.readJson(projectRoot.resolve(TREE_FILE)), types);
        clear();
        types.values().forEach(SkillTree::registerType);
        for (SkillNode node : nodes) {
            SkillTree.register(node);
        }
    }

    public static void clear() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }
}
