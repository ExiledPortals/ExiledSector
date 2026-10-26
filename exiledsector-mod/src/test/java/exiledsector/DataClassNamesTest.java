package exiledsector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataClassNamesTest {

    private static final List<Path> DATA = List.of(Path.of("data"), Path.of("exiled-lib/mod/data"), Path.of("mod_info.json"),
            Path.of("exiled-lib/mod/mod_info.json"));
    private static final Pattern CLASS_NAME = Pattern.compile("\\b(?:exiledsector|exiledlib)(?:\\.[a-z]\\w*)*\\.[A-Z][\\w$]*");

    @Test
    void everyOfOurClassesNamedInDataFilesResolves() throws IOException {
        Map<String, TreeSet<String>> named = namedClasses();
        assertTrue(named.containsKey(ExiledSectorModPlugin.class.getName()), named.keySet().toString());
        assertTrue(named.keySet().stream().anyMatch(className -> className.endsWith(".ChartBattlePlugin")), named.keySet().toString());

        List<String> missing = new ArrayList<>();
        named.forEach((className, files) -> {
            if (!resolves(className)) {
                missing.add(className + " in " + files);
            }
        });
        assertEquals(List.of(), missing);
    }

    private static Map<String, TreeSet<String>> namedClasses() throws IOException {
        Map<String, TreeSet<String>> named = new TreeMap<>();
        for (Path root : DATA) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Matcher matcher = CLASS_NAME.matcher(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1));
                    while (matcher.find()) {
                        named.computeIfAbsent(matcher.group(), className -> new TreeSet<>()).add(file.toString().replace('\\', '/'));
                    }
                }
            }
        }
        return named;
    }

    private static boolean resolves(String className) {
        try {
            Class.forName(className, false, DataClassNamesTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
