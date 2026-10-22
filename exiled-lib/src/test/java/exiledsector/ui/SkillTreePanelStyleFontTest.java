package exiledsector.ui;

import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class SkillTreePanelStyleFontTest {

    private static final String UNPARSEABLE_PATH = "[graphics/fonts/orbitron20aabold.fnt~]";
    private static final String PATH_WITH_SPACE = "graphics/fonts/my font.fnt";

    private static URISyntaxException illegalCharacter(String path) {
        return new URISyntaxException(path, "Illegal character in path");
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
