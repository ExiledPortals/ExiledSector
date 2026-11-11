package exiledsector.skills.tags;

import exiledsector.ProjectFiles;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class RealThemeTags {

    private RealThemeTags() {
    }

    public static JSONArray rows() throws IOException, JSONException {
        String csv = Files.readString(ProjectFiles.gameFile(ThemeTags.PATH), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    public static void load() throws IOException, JSONException {
        ThemeTags.register(rows());
    }
}
