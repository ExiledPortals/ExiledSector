package exiledsector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OldestGameCompatibilityTest {

    static final String OLDEST_GAME = "0.98a-RC8";
    private static final Path COMPILED_CLASSES = Path.of("target", "classes");

    @Test
    void everyGameClassMemberAndAbstractMethodOurShippedCodeReliesOnExistsInTheOldestSupportedGame() throws IOException {
        Path libs = ProjectFiles.projectRoot().resolve("libs");
        Path oldestGameLibs = libs.resolve(OLDEST_GAME);
        assertTrue(Files.isDirectory(oldestGameLibs),
                "Run setup-libs.ps1 to copy the " + OLDEST_GAME + " game jars into " + oldestGameLibs);
        Map<String, CompiledClass> ourClasses = ourCompiledClasses();
        assertFalse(ourClasses.isEmpty(), "No compiled classes found under the modules' " + COMPILED_CLASSES);
        List<Path> oldestGameJarPaths = jarsIn(oldestGameLibs);
        Set<String> gameJarNames = new TreeSet<>(oldestGameJarPaths.stream().map(jar -> jar.getFileName().toString()).toList());
        List<Path> libraryJarPaths = jarsIn(libs).stream().filter(jar -> !gameJarNames.contains(jar.getFileName().toString())).toList();

        List<ZipFile> oldestGameJars = open(oldestGameJarPaths);
        List<ZipFile> libraryJars = open(libraryJarPaths);
        Set<String> problems = new TreeSet<>();
        try {
            OldestGameApi api = new OldestGameApi(ourClasses, oldestGameJars, libraryJars);
            ourClasses.values().forEach(ours -> problems.addAll(api.problemsWith(ours)));
        } finally {
            closeAll(oldestGameJars);
            closeAll(libraryJars);
        }

        assertEquals(Set.of(), problems, "Game " + OLDEST_GAME + " lacks what this code needs; use only API that both game versions have");
    }

    private static Map<String, CompiledClass> ourCompiledClasses() throws IOException {
        Map<String, CompiledClass> classes = new TreeMap<>();
        for (Path module : ProjectFiles.moduleRoots()) {
            Path compiled = module.resolve(COMPILED_CLASSES);
            if (!Files.isDirectory(compiled)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(compiled)) {
                for (Path classFile : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                    try (InputStream classBytes = Files.newInputStream(classFile)) {
                        CompiledClass compiledClass = CompiledClass.read(classBytes);
                        classes.put(compiledClass.name(), compiledClass);
                    }
                }
            }
        }
        return classes;
    }

    private static List<Path> jarsIn(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(path -> path.toString().endsWith(".jar")).sorted().toList();
        }
    }

    private static List<ZipFile> open(List<Path> jars) throws IOException {
        List<ZipFile> opened = new ArrayList<>();
        for (Path jar : jars) {
            opened.add(new ZipFile(jar.toFile()));
        }
        return opened;
    }

    private static void closeAll(List<ZipFile> jars) throws IOException {
        for (ZipFile jar : jars) {
            jar.close();
        }
    }
}
