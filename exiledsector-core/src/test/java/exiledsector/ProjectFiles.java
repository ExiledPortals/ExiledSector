package exiledsector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ProjectFiles {

    public static final String ROOT_PROPERTY = "exiledsector.root";
    private static final Path ROOT_MARKER = Path.of("data/skilltrees/skill_types.json");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Pattern MODULE = Pattern.compile("<module>([^<]+)</module>");

    private ProjectFiles() {
    }

    public static Path projectRoot() {
        String override = System.getProperty(ROOT_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath();
        }
        Path workingDirectory = Path.of("").toAbsolutePath();
        if (Files.isRegularFile(workingDirectory.resolve(ROOT_MARKER))) {
            return workingDirectory;
        }
        Path fromClasses = rootAboveCompiledClasses();
        return fromClasses != null ? fromClasses : workingDirectory;
    }

    private static Path rootAboveCompiledClasses() {
        CodeSource codeSource = ProjectFiles.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return null;
        }
        try {
            for (Path candidate = Path.of(codeSource.getLocation().toURI()); candidate != null; candidate = candidate.getParent()) {
                if (Files.isRegularFile(candidate.resolve(ROOT_MARKER))) {
                    return candidate;
                }
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    public static Path gameFile(String gamePath) {
        return projectRoot().resolve(gamePath);
    }

    public static List<Path> mainSourceRoots() throws IOException {
        Matcher module = MODULE.matcher(Files.readString(projectRoot().resolve("pom.xml"), StandardCharsets.UTF_8));
        List<Path> roots = new ArrayList<>();
        while (module.find()) {
            roots.add(projectRoot().resolve(module.group(1)).resolve(MAIN_SOURCES));
        }
        return roots;
    }

    public static Map<String, Path> mainSources() throws IOException {
        Map<String, Path> sourcesByRelativePath = new TreeMap<>();
        for (Path root : mainSourceRoots()) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    sourcesByRelativePath.put(root.relativize(file).toString().replace('\\', '/'), file);
                }
            }
        }
        return sourcesByRelativePath;
    }

    public static Path mainSource(String relativePath) throws IOException {
        Path source = mainSources().get(relativePath.replace('\\', '/'));
        if (source == null) {
            throw new IllegalArgumentException("No main source " + relativePath);
        }
        return source;
    }

    public static JSONObject readJson(Path path) throws IOException, JSONException {
        return new JSONObject(Files.readString(path, StandardCharsets.UTF_8));
    }
}
