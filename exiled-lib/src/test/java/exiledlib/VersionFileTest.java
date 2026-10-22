package exiledlib;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionFileTest {

    private static final String VERSION_FILE = "ExiledSector.version";
    private static final String LIBRARY_MOD = "exiled-lib/mod/";
    private static final String LIBRARY_VERSION_FILE = "ExiledLib.version";

    private static JSONObject readJson(String path) throws Exception {
        return new JSONObject(Files.readString(Path.of(path), StandardCharsets.UTF_8));
    }

    private static String versionString(JSONObject version) throws Exception {
        return version.getInt("major") + "." + version.getInt("minor") + "." + version.getInt("patch");
    }

    @Test
    void theVersionFileMatchesModInfo() throws Exception {
        JSONObject modInfo = readJson("mod_info.json");
        JSONObject versionFile = readJson(VERSION_FILE);

        assertEquals(versionString(modInfo.getJSONObject("version")), versionString(versionFile.getJSONObject("modVersion")));
        assertEquals(modInfo.getString("name"), versionFile.getString("modName"));
    }

    @Test
    void theMasterVersionFileAndDownloadPointAtThisRepositorysReleases() throws Exception {
        JSONObject versionFile = readJson(VERSION_FILE);

        assertTrue(versionFile.getString("masterVersionFile").endsWith("/main/" + VERSION_FILE));
        assertTrue(versionFile.getString("directDownloadURL").endsWith("/releases/latest/download/ExiledSector.zip"));
    }

    @Test
    void exiledLibsVersionFileMatchesItsModInfoAndIsListedForVersionChecker() throws Exception {
        JSONObject modInfo = readJson(LIBRARY_MOD + "mod_info.json");
        JSONObject versionFile = readJson(LIBRARY_MOD + LIBRARY_VERSION_FILE);

        assertEquals(versionString(modInfo.getJSONObject("version")), versionString(versionFile.getJSONObject("modVersion")));
        assertEquals(modInfo.getString("name"), versionFile.getString("modName"));
        assertTrue(versionFile.getString("masterVersionFile").endsWith("/main/" + LIBRARY_MOD + LIBRARY_VERSION_FILE));
        assertEquals(List.of("version file", LIBRARY_VERSION_FILE),
                Files.readAllLines(Path.of(LIBRARY_MOD + "data/config/version/version_files.csv"), StandardCharsets.UTF_8));
    }

    @Test
    void versionCheckerIsPointedAtTheVersionFile() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("data/config/version/version_files.csv"), StandardCharsets.UTF_8);

        assertEquals(List.of("version file", VERSION_FILE), lines);
        assertTrue(Files.exists(Path.of(VERSION_FILE)));
    }
}
