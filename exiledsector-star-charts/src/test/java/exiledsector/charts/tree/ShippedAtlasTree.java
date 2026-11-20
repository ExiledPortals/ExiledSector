package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import org.json.JSONObject;

public final class ShippedAtlasTree {

    private ShippedAtlasTree() {
    }

    public static AtlasTreeData load() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        JSONObject tree = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH));
        JSONObject bodies = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasBodyReader.BODIES_PATH));
        return AtlasTreeData.parse(types, tree, bodies);
    }
}
