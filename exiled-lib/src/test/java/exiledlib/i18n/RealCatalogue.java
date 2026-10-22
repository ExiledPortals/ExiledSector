package exiledlib.i18n;

import exiledlib.ProjectFiles;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RealCatalogue {

    private static final Map<String, Catalogue> CACHE = new ConcurrentHashMap<>();

    private RealCatalogue() {
    }

    public static Catalogue english() {
        return of(LocaleChain.ENGLISH);
    }

    public static Catalogue of(String locale) {
        return CACHE.computeIfAbsent(locale, RealCatalogue::load);
    }

    public static List<Path> files(String locale) {
        return I18n.paths(locale).stream().map(ProjectFiles::gameFile).filter(Files::isRegularFile).toList();
    }

    public static Map<String, String> entries(String locale) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (Path path : files(locale)) {
            entries.putAll(readEntries(path));
        }
        return entries;
    }

    private static Map<String, String> readEntries(Path path) {
        try {
            return I18n.flatten(new JSONObject(Files.readString(path, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JSONException e) {
            throw new IllegalStateException("Invalid JSON in " + path, e);
        }
    }

    private static Catalogue load(String locale) {
        return Catalogue.compose(locale, RealCatalogue::entries);
    }
}
