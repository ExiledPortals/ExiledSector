package exiledsector;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippedGraphicsTest {

    private static final Pattern OUR_IMAGE = Pattern.compile("graphics/(?:exiledSector|fonts/exiledSector)/[A-Za-z0-9_\\-./]+\\.(?:png|jpg|fnt)");
    private static final Pattern OUR_PATH_PREFIX = Pattern.compile("\"(graphics/exiledSector/[A-Za-z0-9_\\-./]*[/_])\"");
    private static final Pattern SOURCE_ART_IMAGE = Pattern.compile("\\bart/[A-Za-z0-9_\\-./ ]+\\.(?:png|jpg|gif)");
    private static final Set<String> DATA_EXTENSIONS = Set.of(".json", ".csv", ".faction");
    private static final Set<String> SHIPPED_GRAPHICS_FOLDERS = Set.of("exiledSector", "fonts");

    @Test
    void everyImageTheModReferencesUnderItsOwnFoldersExists() throws IOException {
        Map<String, Set<String>> missingByFile = new TreeMap<>();
        for (Path file : referencingFiles()) {
            Matcher image = OUR_IMAGE.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (image.find()) {
                if (!Files.isRegularFile(ProjectFiles.gameFile(image.group()))) {
                    missingByFile.computeIfAbsent(relative(file), key -> new TreeSet<>()).add(image.group());
                }
            }
        }
        assertEquals(Map.of(), missingByFile);
    }

    @Test
    void everyImagePathPrefixInCodeMatchesAtLeastOneImage() throws IOException {
        List<String> shippedImages = shippedImages();
        Set<String> unmatched = new TreeSet<>();
        int prefixes = 0;
        for (Path file : ProjectFiles.mainSources().values()) {
            Matcher prefix = OUR_PATH_PREFIX.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (prefix.find()) {
                prefixes++;
                String folderPrefix = prefix.group(1);
                if (shippedImages.stream().noneMatch(image -> image.startsWith(folderPrefix))) {
                    unmatched.add(relative(file) + ": " + folderPrefix);
                }
            }
        }
        assertTrue(prefixes > 0, "expected prefix-built image paths such as the refit schematic icons");
        assertEquals(Set.of(), unmatched);
    }

    @Test
    void nothingShippedPointsIntoTheUndeployedArtFolder() throws IOException {
        Set<String> references = new TreeSet<>();
        for (Path file : referencingFiles()) {
            Matcher art = SOURCE_ART_IMAGE.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (art.find()) {
                references.add(relative(file) + ": " + art.group());
            }
        }
        assertEquals(Set.of(), references);
    }

    @Test
    void theGraphicsFolderHoldsOnlyTheModNamespaceAndItsFonts() throws IOException {
        Set<String> folders = new TreeSet<>();
        try (Stream<Path> entries = Files.list(ProjectFiles.gameFile("graphics"))) {
            entries.forEach(entry -> folders.add(entry.getFileName().toString()));
        }
        assertEquals(SHIPPED_GRAPHICS_FOLDERS, folders);
        try (Stream<Path> fonts = Files.list(ProjectFiles.gameFile("graphics/fonts"))) {
            assertEquals(List.of("exiledSector"), fonts.map(entry -> entry.getFileName().toString()).toList());
        }
        assertFalse(shippedImages().isEmpty());
    }

    private static List<Path> referencingFiles() throws IOException {
        List<Path> files = new ArrayList<>(ProjectFiles.mainSources().values());
        try (Stream<Path> data = Files.walk(ProjectFiles.gameFile("data"))) {
            data.filter(Files::isRegularFile).filter(ShippedGraphicsTest::isDataFile).forEach(files::add);
        }
        return files;
    }

    private static boolean isDataFile(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && DATA_EXTENSIONS.contains(name.substring(dot).toLowerCase());
    }

    private static List<String> shippedImages() throws IOException {
        try (Stream<Path> images = Files.walk(ProjectFiles.gameFile("graphics/exiledSector"))) {
            return images.filter(Files::isRegularFile).map(ShippedGraphicsTest::relative).toList();
        }
    }

    private static String relative(Path file) {
        return ProjectFiles.projectRoot().relativize(file.toAbsolutePath()).toString().replace('\\', '/');
    }
}
