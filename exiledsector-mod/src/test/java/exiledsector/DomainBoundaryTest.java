package exiledsector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainBoundaryTest {

    private static final Path CLASS_PARTS = Path.of("docs/plans/domain_split_classes.csv");
    private static final Path DATA_FILES = Path.of("data");
    private static final Path FILE_PARTS = Path.of("docs/plans/domain_split_files.csv");
    private static final Path ALLOWED_CROSSINGS = Path.of("exiledsector-mod/src/test/resources/domain_boundary_allowlist.txt");
    private static final String LIB = "lib";
    private static final String CORE = "core";
    private static final Set<String> FEATURES = Set.of("skill-tree", "hull-upgrades", "star-charts");
    private static final String ARROW = " -> ";
    private static final String COMPOSITION_ROOT = "exiledsector.ExiledSectorModPlugin";
    private static final String COMPOSITION_ROOT_MODULE = "exiledsector-mod";
    private static final Map<String, String> MODULE_BY_PART = Map.of(LIB, "exiled-lib", CORE, "exiledsector-core",
            "skill-tree", "exiledsector-skill-tree", "hull-upgrades", "exiledsector-hull-upgrades", "star-charts", "exiledsector-star-charts");
    private static final Pattern DOMAIN_CLASS = Pattern.compile("\\bclass\\s+\\w+\\s+implements\\s+(?:ExiledSector)?Domain\\b");

    private static final Pattern LITERAL_OR_COMMENT = Pattern.compile(
            "\"(?:[^\"\\\\\\n]|\\\\.)*\"|'(?:[^'\\\\\\n]|\\\\.)+'|//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern HEADER_LINE = Pattern.compile("^\\s*(?:package|import)\\s[^;]*;", Pattern.MULTILINE);
    private static final Pattern MOD_IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?(exiledsector\\.[\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TYPE_NAME = Pattern.compile("\\b[A-Z]\\w*\\b");

    @Test
    void everyMainClassBelongsToExactlyOnePart() throws IOException {
        Set<String> classes = mainClasses().keySet();
        List<String> problems = new ArrayList<>();
        Map<String, String> parts = new HashMap<>();
        List<String> rows = Files.readAllLines(CLASS_PARTS);
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split(",", -1);
            if (cells.length != 2 || !isPart(cells[1])) {
                problems.add("Malformed row: " + row);
            } else if (parts.put(cells[0], cells[1]) != null) {
                problems.add("Listed twice: " + cells[0]);
            } else if (!classes.contains(cells[0])) {
                problems.add("No such main class: " + cells[0]);
            }
        }
        classes.stream().filter(name -> !parts.containsKey(name)).forEach(name -> problems.add("Not assigned to a part: " + name));

        assertTrue(problems.isEmpty(), CLASS_PARTS + " must assign every main class to one of lib, core, "
                + String.join(", ", new TreeSet<>(FEATURES)) + ":\n" + String.join("\n", problems));
    }

    @Test
    void everyMainClassSitsInTheMavenModuleOfItsPart() throws IOException {
        Map<String, String> parts = classParts();
        List<String> misplaced = new ArrayList<>();
        mainClasses().forEach((name, file) -> {
            String expected = name.equals(COMPOSITION_ROOT) ? COMPOSITION_ROOT_MODULE : MODULE_BY_PART.get(parts.get(name));
            String module = ProjectFiles.projectRoot().relativize(file.toAbsolutePath()).getName(0).toString();
            if (expected != null && !expected.equals(module)) {
                misplaced.add(name + " is in " + module + " but its part is in " + expected);
            }
        });
        assertTrue(misplaced.isEmpty(), String.join("\n", misplaced));
    }

    @Test
    void everyDataFileBelongsToExactlyOnePart() throws IOException {
        Set<String> files;
        try (Stream<Path> paths = Files.walk(DATA_FILES)) {
            files = new TreeSet<>(paths.filter(Files::isRegularFile).map(path -> path.toString().replace('\\', '/')).toList());
        }
        List<String> problems = new ArrayList<>();
        Set<String> listed = new TreeSet<>();
        List<String> rows = Files.readAllLines(FILE_PARTS);
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split(",", -1);
            if (cells.length != 2 || !isPart(cells[1])) {
                problems.add("Malformed row: " + row);
            } else if (!listed.add(cells[0])) {
                problems.add("Listed twice: " + cells[0]);
            } else if (!files.contains(cells[0])) {
                problems.add("No such data file: " + cells[0]);
            }
        }
        files.stream().filter(file -> !listed.contains(file)).forEach(file -> problems.add("Not assigned to a part: " + file));

        assertTrue(problems.isEmpty(), FILE_PARTS + " must assign every data file to one part:\n" + String.join("\n", problems));
    }

    @Test
    void noClassReachesAcrossAPartBoundaryOutsideTheAllowlist() throws IOException {
        Map<String, Path> classes = mainClasses();
        Map<String, String> parts = classParts();
        Set<String> crossings = new TreeSet<>();
        for (Map.Entry<String, Path> source : classes.entrySet()) {
            String sourcePart = parts.get(source.getKey());
            for (String target : references(source.getKey(), source.getValue(), classes.keySet())) {
                String targetPart = parts.get(target);
                boolean listsADomain = source.getKey().equals(COMPOSITION_ROOT) && isDomain(classes.get(target));
                if (sourcePart != null && targetPart != null && !listsADomain && !mayReach(sourcePart, targetPart)) {
                    crossings.add(source.getKey() + ARROW + target);
                }
            }
        }
        Set<String> allowed = new TreeSet<>(Files.readAllLines(ALLOWED_CROSSINGS).stream().filter(line -> !line.isBlank()).toList());

        Set<String> added = new TreeSet<>(crossings);
        added.removeAll(allowed);
        Set<String> cleared = new TreeSet<>(allowed);
        cleared.removeAll(crossings);

        assertTrue(added.isEmpty(), "Lib may only use lib, core may only use lib and core, and a feature domain may only use lib, core "
                + "and itself, though the mod plugin lists every Domain. These new crossings break that (see docs/plans/domain_split.md):\n" + String.join("\n", added));
        assertTrue(cleared.isEmpty(), "These crossings are gone, so remove them from " + ALLOWED_CROSSINGS + ":\n" + String.join("\n", cleared));
    }

    private static boolean isDomain(Path file) throws IOException {
        return DOMAIN_CLASS.matcher(Files.readString(file)).find();
    }

    private static boolean isPart(String part) {
        return part.equals(LIB) || part.equals(CORE) || FEATURES.contains(part);
    }

    private static boolean mayReach(String sourcePart, String targetPart) {
        if (targetPart.equals(LIB) || targetPart.equals(sourcePart)) {
            return true;
        }
        return targetPart.equals(CORE) && FEATURES.contains(sourcePart);
    }

    private static Map<String, Path> mainClasses() throws IOException {
        Map<String, Path> classes = new TreeMap<>();
        ProjectFiles.mainSources().forEach((relative, file) ->
                classes.put(relative.substring(0, relative.length() - ".java".length()).replace('/', '.'), file));
        return classes;
    }

    private static Map<String, String> classParts() throws IOException {
        Map<String, String> parts = new HashMap<>();
        List<String> rows = Files.readAllLines(CLASS_PARTS);
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split(",", -1);
            if (cells.length == 2) {
                parts.put(cells[0], cells[1]);
            }
        }
        return parts;
    }

    private static Set<String> references(String className, Path file, Set<String> classes) throws IOException {
        String code = LITERAL_OR_COMMENT.matcher(Files.readString(file)).replaceAll(" ");
        Set<String> references = new LinkedHashSet<>();
        Matcher imports = MOD_IMPORT.matcher(code);
        while (imports.find()) {
            String imported = enclosingClass(imports.group(1), classes);
            if (imported != null) {
                references.add(imported);
            }
        }
        String samePackage = className.substring(0, className.lastIndexOf('.') + 1);
        Matcher names = TYPE_NAME.matcher(HEADER_LINE.matcher(code).replaceAll(" "));
        while (names.find()) {
            String candidate = samePackage + names.group();
            if (classes.contains(candidate)) {
                references.add(candidate);
            }
        }
        references.remove(className);
        return references;
    }

    private static String enclosingClass(String importedName, Set<String> classes) {
        String name = importedName;
        while (!classes.contains(name)) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot < 0) {
                return null;
            }
            name = name.substring(0, lastDot);
        }
        return name;
    }
}
