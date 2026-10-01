package exiledsector.skills.npc;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.loader.SkillTypeLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;

public final class RealSkillData {

    public static final String ROOT_PROPERTY = "exiledsector.root";
    public static final Path TYPES_FILE = Path.of("data/skilltrees/skill_types.json");
    public static final Path TREE_FILE = Path.of("data/skilltrees/ship_skill_tree.json");
    public static final Path LAYOUTS_FILE = Path.of(NpcLayoutLoader.DATA_PATH);

    private RealSkillData() {
    }

    public static Path projectRoot() {
        String override = System.getProperty(ROOT_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath();
        }
        Path workingDirectory = Path.of("").toAbsolutePath();
        if (Files.isRegularFile(workingDirectory.resolve(TYPES_FILE))) {
            return workingDirectory;
        }
        Path fromClasses = rootAboveCompiledClasses();
        return fromClasses != null ? fromClasses : workingDirectory;
    }

    private static Path rootAboveCompiledClasses() {
        CodeSource codeSource = RealSkillData.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return null;
        }
        try {
            for (Path candidate = Path.of(codeSource.getLocation().toURI()); candidate != null; candidate = candidate.getParent()) {
                if (Files.isRegularFile(candidate.resolve(TYPES_FILE))) {
                    return candidate;
                }
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    public static void load() throws IOException, JSONException {
        load(projectRoot());
    }

    public static void load(Path projectRoot) throws IOException, JSONException {
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(readJson(projectRoot.resolve(TYPES_FILE)));
        List<SkillNode> nodes = SkillTreeLoader.parseNodes(readJson(projectRoot.resolve(TREE_FILE)), types);
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

    public static JSONObject readJson(Path path) throws IOException, JSONException {
        return new JSONObject(Files.readString(path, StandardCharsets.UTF_8));
    }
}
