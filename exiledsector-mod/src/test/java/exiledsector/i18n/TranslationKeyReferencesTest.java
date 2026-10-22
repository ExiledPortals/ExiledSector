package exiledsector.i18n;

import exiledsector.ProjectFiles;
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

    private static final Path CLASS_PARTS = Path.of("docs/plans/domain_split_classes.csv");
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

    @Test
    void everyLiteralKeyTheCodeLooksUpExistsInTheEnglishCatalogue() throws IOException, JSONException {
        JSONObject english = new JSONObject(RealCatalogue.entries(LocaleChain.ENGLISH));
        List<String> missing = new ArrayList<>();
        forEachLiteralKey((source, key) -> {
            if (!isDefined(english, key)) {
                missing.add(source + ": " + key);
            }
        });
        assertTrue(missing.isEmpty(), "Keys looked up in code but missing from the English catalogues: " + missing);
    }

    @Test
    void everyLiteralKeyLivesInTheCatalogueOfItsClassesPartOrBelow() throws IOException, JSONException {
        Map<String, String> partByClass = classParts();
        Map<String, JSONObject> catalogueByPart = new HashMap<>();
        for (Map.Entry<String, String> catalogue : CATALOGUE_BY_PART.entrySet()) {
            Path file = ProjectFiles.projectRoot().resolve(catalogue.getValue());
            catalogueByPart.put(catalogue.getKey(), new JSONObject(Files.readString(file, StandardCharsets.UTF_8)));
        }
        List<String> misplaced = new ArrayList<>();
        forEachLiteralKey((source, key) -> {
            String part = partByClass.get(className(source));
            if (part != null && reachableParts(part).stream().noneMatch(reachable -> isDefined(catalogueByPart.get(reachable), key))) {
                misplaced.add(source + " (" + part + "): " + key);
            }
        });
        assertTrue(misplaced.isEmpty(), "Keys looked up outside the catalogue of the class's part, core or lib: " + misplaced);
    }

    private interface KeyVisitor {
        void visit(String source, String key);
    }

    private static void forEachLiteralKey(KeyVisitor visitor) throws IOException {
        for (Map.Entry<String, Path> source : ProjectFiles.mainSources().entrySet()) {
            Matcher matcher = LITERAL_KEY.matcher(Files.readString(source.getValue(), StandardCharsets.UTF_8));
            while (matcher.find()) {
                String key = matcher.group(1);
                if (!key.endsWith(".") && !key.startsWith(METADATA_PREFIX)) {
                    visitor.visit(source.getKey(), key);
                }
            }
        }
    }

    private static Map<String, String> classParts() throws IOException {
        Map<String, String> partByClass = new HashMap<>();
        List<String> rows = Files.readAllLines(CLASS_PARTS);
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split(",", -1);
            partByClass.put(cells[0], cells[1]);
        }
        return partByClass;
    }

    private static String className(String relative) {
        return relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
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
