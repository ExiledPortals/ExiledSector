package exiledsector.i18n;

import exiledlib.ProjectFiles;
import exiledlib.i18n.I18n;
import exiledlib.i18n.LocaleChain;
import exiledlib.i18n.RealCatalogue;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationKeyReferencesTest {

    private static final Pattern LITERAL_KEY = Pattern.compile("Translation\\.(?:text|msg|styled|gameText)\\(\\s*\"([^\"]+)\"");
    private static final List<String> PLURAL_FORMS = List.of(".one", ".other");
    private static final String METADATA_PREFIX = "meta.";
    private static final String LIB = "lib";
    private static final String CORE = "core";
    private static final Map<String, String> CATALOGUE_BY_PART = Map.of(
            LIB, I18n.LIBRARY_STRINGS_DIRECTORY + "en.json",
            CORE, "data/strings/exiledSector/core/en.json",
            "skill-tree", "data/strings/exiledSector/skillTree/en.json",
            "hull-upgrades", "data/strings/exiledSector/hullUpgrades/en.json",
            "star-charts", "data/strings/exiledSector/starCharts/en.json");
    private static final Map<String, String> PART_BY_MODULE = Map.of(
            "exiled-lib", LIB,
            "exiledsector-core", CORE,
            "exiledsector-skill-tree", "skill-tree",
            "exiledsector-hull-upgrades", "hull-upgrades",
            "exiledsector-star-charts", "star-charts",
            "exiledsector-mod", CORE);

    @Test
    void everyLiteralKeyTheCodeLooksUpExistsInTheEnglishCatalogue() throws IOException, JSONException {
        JSONObject english = new JSONObject(RealCatalogue.entries(LocaleChain.ENGLISH));
        List<String> missing = new ArrayList<>();
        forEachLiteralKey((source, file, key) -> {
            if (!isDefined(english, key)) {
                missing.add(source + ": " + key);
            }
        });
        assertTrue(missing.isEmpty(), "Keys looked up in code but missing from the English catalogues: " + missing);
    }

    @Test
    void everyLiteralKeyLivesInTheCatalogueOfItsClassesPartOrBelow() throws IOException, JSONException {
        Map<String, JSONObject> catalogueByPart = new HashMap<>();
        for (Map.Entry<String, String> catalogue : CATALOGUE_BY_PART.entrySet()) {
            Path file = ProjectFiles.gameFile(catalogue.getValue());
            catalogueByPart.put(catalogue.getKey(), new JSONObject(Files.readString(file, StandardCharsets.UTF_8)));
        }
        List<String> misplaced = new ArrayList<>();
        forEachLiteralKey((source, file, key) -> {
            String part = partOf(file);
            if (part != null && reachableParts(part).stream().noneMatch(reachable -> isDefined(catalogueByPart.get(reachable), key))) {
                misplaced.add(source + " (" + part + "): " + key);
            }
        });
        assertTrue(misplaced.isEmpty(), "Keys looked up outside the catalogue of the class's part, core or lib: " + misplaced);
    }

    private interface KeyVisitor {
        void visit(String source, Path file, String key);
    }

    private static void forEachLiteralKey(KeyVisitor visitor) throws IOException {
        for (Map.Entry<String, Path> source : ProjectFiles.mainSources().entrySet()) {
            Matcher matcher = LITERAL_KEY.matcher(Files.readString(source.getValue(), StandardCharsets.UTF_8));
            while (matcher.find()) {
                String key = matcher.group(1);
                if (!key.endsWith(".") && !key.startsWith(METADATA_PREFIX)) {
                    visitor.visit(source.getKey(), source.getValue(), key);
                }
            }
        }
    }

    private static String partOf(Path source) {
        return PART_BY_MODULE.get(ProjectFiles.projectRoot().relativize(source.toAbsolutePath()).getName(0).toString());
    }

    private static List<String> reachableParts(String part) {
        if (part.equals(LIB)) {
            return List.of(LIB);
        }
        return part.equals(CORE) ? List.of(CORE, LIB) : List.of(part, CORE, LIB);
    }

    private static boolean isDefined(JSONObject english, String key) {
        return english.has(key) || PLURAL_FORMS.stream().anyMatch(form -> english.has(key + form));
    }
}
