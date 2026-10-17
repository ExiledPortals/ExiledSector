package exiledsector.i18n;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationKeyReferencesTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Path ENGLISH_CATALOGUE = Path.of("data/strings/exiledSector/en.json");
    private static final Pattern LITERAL_KEY = Pattern.compile("Translation\\.(?:text|msg|styled|gameText)\\(\\s*\"([^\"]+)\"");
    private static final List<String> PLURAL_FORMS = List.of(".one", ".other");
    private static final String METADATA_PREFIX = "meta.";

    @Test
    void everyLiteralKeyTheCodeLooksUpExistsInTheEnglishCatalogue() throws IOException, JSONException {
        JSONObject english = new JSONObject(Files.readString(ENGLISH_CATALOGUE, StandardCharsets.UTF_8));
        List<String> missing = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = LITERAL_KEY.matcher(Files.readString(source, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!key.endsWith(".") && !key.startsWith(METADATA_PREFIX) && !isDefined(english, key)) {
                        missing.add(MAIN_SOURCES.relativize(source) + ": " + key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Keys looked up in code but missing from en.json: " + missing);
    }

    private static boolean isDefined(JSONObject english, String key) {
        return english.has(key) || PLURAL_FORMS.stream().anyMatch(form -> english.has(key + form));
    }
}
