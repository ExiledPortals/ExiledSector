package exiledsector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptSandboxRestrictionsTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Pattern FORBIDDEN = Pattern.compile(
            "java\\.lang\\.reflect\\.|\\bjava\\.io\\.File\\b|java\\.nio\\.file\\.|\\.getDeclaredMethod\\(|\\.getDeclaredField\\(|\\.getMethod\\(|\\.setAccessible\\(");

    private static final Path REFLECTION_WORKAROUND = MAIN_SOURCES.resolve("exiledsector/ui/refit/UiReflection.java");

    @Test
    void onlyTheRefitScreenHelperReachesReflectionAndOnlyThroughMethodHandles() throws IOException {
        assertTrue(Files.exists(REFLECTION_WORKAROUND), "The sandbox exemption names a file that no longer exists: " + REFLECTION_WORKAROUND);
        List<String> direct = Files.readAllLines(REFLECTION_WORKAROUND).stream()
                .filter(line -> FORBIDDEN.matcher(line).find() && !line.contains("Class.forName("))
                .map(String::trim)
                .toList();
        assertTrue(direct.isEmpty(), "Reflection types may only be loaded by name and used through MethodHandles:\n" + String.join("\n", direct));
    }

    @Test
    void noModCodeUsesApisThatStarsectorBlocksForScripts() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java") && !path.equals(REFLECTION_WORKAROUND)).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (FORBIDDEN.matcher(lines.get(i)).find()) {
                        violations.add(MAIN_SOURCES.relativize(file) + ":" + (i + 1) + ": " + lines.get(i).trim());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), "Starsector throws SecurityException for these in mod code; use "
                + "java.lang.invoke.MethodHandles or the game's own file APIs instead:\n" + String.join("\n", violations));
    }
}
