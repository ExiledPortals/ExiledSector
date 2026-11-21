package exiledsector.skills.tags;

import exiledsector.ProjectFiles;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public final class RealThemeTags {

    private RealThemeTags() {
    }

    public static JSONArray rows() throws IOException, JSONException {
        return ProjectFiles.csvRows(ThemeTags.PATH);
    }

    public static void load() throws IOException, JSONException {
        ThemeTags.register(rows());
    }
}
