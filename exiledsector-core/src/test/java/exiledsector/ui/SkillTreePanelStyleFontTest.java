package exiledsector.ui;

import exiledsector.ProjectFiles;
import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.RealCatalogue;
import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SkillTreePanelStyleFontTest {

    private static final String UNPARSEABLE_PATH = "[graphics/fonts/orbitron20aabold.fnt~]";
    private static final String PATH_WITH_SPACE = "graphics/fonts/my font.fnt";
    private static final Pattern LINE_HEIGHT = Pattern.compile("^common lineHeight=(\\d+) ", Pattern.MULTILINE);

    private static URISyntaxException illegalCharacter(String path) {
        return new URISyntaxException(path, "Illegal character in path");
    }

    @Test
    void chineseBodyTextDrawsAtTheAtlasNativeSize() throws IOException {
        Path fnt = ProjectFiles.projectRoot().resolve(RealCatalogue.of(LocaleChain.SIMPLIFIED_CHINESE).raw("meta.font"));
        Matcher lineHeight = LINE_HEIGHT.matcher(Files.readString(fnt, StandardCharsets.UTF_8));

        assertTrue(lineHeight.find(), "line height");
        assertEquals(String.valueOf((int) SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE), lineHeight.group(1),
                "LazyFont scales text by font size / line height");
    }

    @Test
    void aFontPathLazyLibCannotParseFallsBackToTheDefaultFont() {
        LazyFont orbitron = mock(LazyFont.class);
        try (MockedStatic<LazyFont> lazyFont = Mockito.mockStatic(LazyFont.class)) {
            lazyFont.when(() -> LazyFont.loadFont(UNPARSEABLE_PATH)).thenAnswer(invocation -> {
                throw illegalCharacter(UNPARSEABLE_PATH);
            });
            lazyFont.when(() -> LazyFont.loadFont(PATH_WITH_SPACE)).thenAnswer(invocation -> {
                throw illegalCharacter(PATH_WITH_SPACE);
            });
            lazyFont.when(() -> LazyFont.loadFont(SkillTreePanelStyle.DEFAULT_FONT_PATH)).thenReturn(orbitron);

            assertSame(orbitron, SkillTreePanelStyle.loadFontOrDefault(UNPARSEABLE_PATH));
            assertSame(orbitron, SkillTreePanelStyle.loadFontOrDefault(PATH_WITH_SPACE));
        }
    }

    @Test
    void givesUpQuietlyWhenEvenTheDefaultFontFails() {
        try (MockedStatic<LazyFont> lazyFont = Mockito.mockStatic(LazyFont.class)) {
            lazyFont.when(() -> LazyFont.loadFont(Mockito.anyString())).thenThrow(new IllegalStateException("no texture"));

            assertNull(SkillTreePanelStyle.loadFontOrDefault("graphics/fonts/exiledSector/notosanssc.fnt"));
        }
    }
}
