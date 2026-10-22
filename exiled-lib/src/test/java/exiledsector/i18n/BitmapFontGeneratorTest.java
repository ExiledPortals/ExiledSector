package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitmapFontGeneratorTest {

    static final Pattern LAZYFONT_SPLIT = Pattern.compile("=|\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    static final int LAZYFONT_METADATA_LENGTH = 51;
    static final int LAZYFONT_CHARDATA_LENGTH = 21;

    private static List<String> tokens(String line) {
        return List.of(LAZYFONT_SPLIT.split(line));
    }

    @Test
    void writesAHeaderAndCharLinesLazyFontCanParse() {
        SortedSet<Integer> characters = new TreeSet<>(List.of((int) ' ', (int) 'A', (int) 'g', (int) '%'));
        BitmapFontGenerator.Atlas atlas = BitmapFontGenerator.generate(new Font(Font.DIALOG, Font.PLAIN, 20), 20, 24,
                128, 128, characters);

        String[] lines = BitmapFontGenerator.toFnt(atlas, "test_0.png").split("\n");
        List<String> metadata = tokens(lines[0] + " " + lines[1] + " " + lines[2]);

        assertEquals(LAZYFONT_METADATA_LENGTH, metadata.size());
        assertEquals("24", metadata.get(27));
        assertEquals("128", metadata.get(31));
        assertEquals("128", metadata.get(33));
        assertEquals("\"test_0.png\"", metadata.get(50));
        assertEquals("chars count=4", lines[3]);
        for (int i = 4; i < lines.length; i++) {
            assertEquals(LAZYFONT_CHARDATA_LENGTH, tokens(lines[i]).size(), lines[i]);
        }
    }

    @Test
    void placesGlyphsInsideTheAtlasWithoutOverlap() {
        SortedSet<Integer> characters = new TreeSet<>();
        for (int c = 'A'; c <= 'Z'; c++) {
            characters.add(c);
        }
        BitmapFontGenerator.Atlas atlas = BitmapFontGenerator.generate(new Font(Font.DIALOG, Font.PLAIN, 20), 20, 24,
                256, 256, characters);

        List<BitmapFontGenerator.Glyph> glyphs = atlas.glyphs();
        for (BitmapFontGenerator.Glyph a : glyphs) {
            assertTrue(a.x() + a.width() <= 256 && a.y() + a.height() <= 256);
            for (BitmapFontGenerator.Glyph b : glyphs) {
                boolean overlap = a != b && a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
                        && a.y() < b.y() + b.height() && b.y() < a.y() + a.height();
                assertTrue(!overlap, a + " overlaps " + b);
            }
        }
    }

    @Test
    void refusesToSilentlyDropGlyphsThatDoNotFit() {
        SortedSet<Integer> characters = new TreeSet<>();
        for (int c = 'A'; c <= 'z'; c++) {
            characters.add(c);
        }
        assertThrows(IllegalStateException.class, () -> BitmapFontGenerator.generate(new Font(Font.DIALOG, Font.PLAIN, 20),
                20, 24, 32, 32, characters));
    }

    @Test
    void theCommonHanziTableHasAllThreeThousandSevenHundredFiftyFiveCharacters() {
        assertEquals(3755, BitmapFontGenerator.gb2312LevelOneHanzi().size());
    }
}
