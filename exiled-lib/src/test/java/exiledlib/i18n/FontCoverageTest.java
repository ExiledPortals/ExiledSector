package exiledlib.i18n;

import exiledlib.ProjectFiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontCoverageTest {

    private static final Pattern CHAR_ID = Pattern.compile("^char id=(\\d+) ", Pattern.MULTILINE);
    private static final Pattern GLYPH_BOX = Pattern.compile("^char id=\\d+\\s+x=\\d+\\s+y=(\\d+)\\s+width=\\d+\\s+height=(\\d+)", Pattern.MULTILINE);
    private static final int MAX_ATLAS_SIZE = 4096;

    private static Path fontFile(String locale) {
        Path root = ProjectFiles.projectRoot();
        return root.resolve(RealCatalogue.of(locale).raw("meta.font"));
    }

    private static Set<Integer> glyphs(String fnt) {
        Set<Integer> ids = new HashSet<>();
        Matcher matcher = CHAR_ID.matcher(fnt);
        while (matcher.find()) {
            ids.add(Integer.parseInt(matcher.group(1)));
        }
        return ids;
    }

    private static List<String> metadata(Path fnt) throws IOException {
        List<String> lines = Files.readAllLines(fnt, StandardCharsets.UTF_8);
        return List.of(BitmapFontGeneratorTest.LAZYFONT_SPLIT.split(lines.get(0) + " " + lines.get(1) + " " + lines.get(2)));
    }

    @Test
    void theChineseFontIsASinglePageAtlasLazyLibCanLoad() throws IOException {
        Path fnt = fontFile(LocaleChain.SIMPLIFIED_CHINESE);
        List<String> metadata = metadata(fnt);

        assertEquals(BitmapFontGeneratorTest.LAZYFONT_METADATA_LENGTH, metadata.size());
        assertEquals("1", metadata.get(35), "pages");
        assertTrue(Integer.parseInt(metadata.get(31)) <= MAX_ATLAS_SIZE && Integer.parseInt(metadata.get(33)) <= MAX_ATLAS_SIZE);
        assertTrue(Files.isRegularFile(fnt.resolveSibling(metadata.get(50).replace("\"", ""))), "atlas image");
        assertTrue(Files.isRegularFile(fnt.resolveSibling("OFL.txt")), "font licence");
    }

    @Test
    void theChineseAtlasIsNoTallerThanItsGlyphsNeed() throws IOException {
        Path fnt = fontFile(LocaleChain.SIMPLIFIED_CHINESE);
        int atlasHeight = Integer.parseInt(metadata(fnt).get(33));
        int glyphBottom = 0;
        Matcher matcher = GLYPH_BOX.matcher(Files.readString(fnt, StandardCharsets.UTF_8));
        while (matcher.find()) {
            glyphBottom = Math.max(glyphBottom, Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(2)));
        }

        assertTrue(glyphBottom > atlasHeight / 2, "glyphs end at y=" + glyphBottom + ", so the atlas could be half as tall");
    }

    @Test
    void theChineseFontHasEveryCharacterBothCataloguesAndTheCommonHanziUse() throws IOException {
        Set<Integer> available = glyphs(Files.readString(fontFile(LocaleChain.SIMPLIFIED_CHINESE), StandardCharsets.UTF_8));
        Set<Integer> needed = new TreeSet<>(BitmapFontGenerator.gb2312LevelOneHanzi());
        for (String locale : List.of(LocaleChain.ENGLISH, LocaleChain.SIMPLIFIED_CHINESE)) {
            for (String value : RealCatalogue.entries(locale).values()) {
                value.codePoints().filter(cp -> !Character.isISOControl(cp)).forEach(needed::add);
            }
        }
        needed.removeAll(available);
        StringBuilder missing = new StringBuilder();
        needed.forEach(missing::appendCodePoint);
        assertEquals("", missing.toString(), "Regenerate the atlas with BitmapFontGenerator; missing characters");
    }
}
