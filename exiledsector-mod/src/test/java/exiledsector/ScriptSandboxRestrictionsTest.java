package exiledsector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptSandboxRestrictionsTest {

    private static final Pattern FORBIDDEN = Pattern.compile(
            "java\\.lang\\.reflect\\.|\\bjava\\.io\\.File\\b|java\\.nio\\.file\\.|\\.getDeclaredMethod\\(|\\.getDeclaredField\\(|\\.getMethod\\(|\\.setAccessible\\(");

    private static final String REFLECTION_WORKAROUND = "exiledsector/ui/refit/UiReflection.java";

    @Test
    void onlyTheRefitScreenHelperReachesReflectionAndOnlyThroughMethodHandles() throws IOException {
        Path workaround = ProjectFiles.mainSources().get(REFLECTION_WORKAROUND);
        assertTrue(workaround != null, "The sandbox exemption names a file that no longer exists: " + REFLECTION_WORKAROUND);
        List<String> direct = Files.readAllLines(workaround).stream()
                .filter(line -> FORBIDDEN.matcher(line).find() && !line.contains("Class.forName("))
                .map(String::trim)
                .toList();
        assertTrue(direct.isEmpty(), "Reflection types may only be loaded by name and used through MethodHandles:\n" + String.join("\n", direct));
    }

    @Test
    void noModCodeUsesApisThatStarsectorBlocksForScripts() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, Path> file : ProjectFiles.mainSources().entrySet()) {
            if (file.getKey().equals(REFLECTION_WORKAROUND)) {
                continue;
            }
            List<String> lines = Files.readAllLines(file.getValue());
            for (int i = 0; i < lines.size(); i++) {
                if (FORBIDDEN.matcher(lines.get(i)).find()) {
                    violations.add(file.getKey() + ":" + (i + 1) + ": " + lines.get(i).trim());
                }
            }
        }

        assertTrue(violations.isEmpty(), "Starsector throws SecurityException for these in mod code; use "
                + "java.lang.invoke.MethodHandles or the game's own file APIs instead:\n" + String.join("\n", violations));
    }
}
