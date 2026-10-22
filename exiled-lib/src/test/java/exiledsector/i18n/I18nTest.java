package exiledsector.i18n;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class I18nTest {

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;
    private final List<LoggingEvent> errors = new ArrayList<>();
    private final AppenderSkeleton errorCapture = new AppenderSkeleton() {
        @Override
        protected void append(LoggingEvent event) {
            if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                errors.add(event);
            }
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    };

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        Logger.getLogger(I18n.class).setLevel(Level.ERROR);
        Logger.getLogger(I18n.class).addAppender(errorCapture);
    }

    private static RuntimeException notFound(String locale) {
        return new RuntimeException("Error loading [data/strings/exiledSector/core/" + locale
                + ".json] resource, not found in [../mods/ExiledSector,CLASSPATH]");
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        Logger.getLogger(I18n.class).removeAppender(errorCapture);
        Logger.getLogger(I18n.class).setLevel(null);
    }

    @Test
    void loadsTheLocaleOverEnglishAndSkipsFilesNoModProvides() throws Exception {
        when(settings.getMergedJSON("data/strings/exiledSector/core/en.json"))
                .thenReturn(new JSONObject("{\"a\":\"English A\",\"b\":\"English B\"}"));
        when(settings.getMergedJSON("data/strings/exiledSector/core/zh.json")).thenThrow(notFound("zh"));
        when(settings.getMergedJSON("data/strings/exiledSector/core/zh_CN.json"))
                .thenReturn(new JSONObject("{\"a\":\"中文甲\"}"));

        I18n.load("zh_CN");

        assertEquals("zh_CN", I18n.locale());
        assertEquals("中文甲", Translation.text("a"));
        assertEquals("English B", Translation.text("b"));
        assertEquals(List.of(), errors);
    }

    @Test
    void mergesTheLibraryCatalogueWithEveryPartCatalogue() throws Exception {
        when(settings.getMergedJSON("data/strings/exiledLib/en.json")).thenReturn(new JSONObject("{\"a\":\"Library A\"}"));
        when(settings.getMergedJSON("data/strings/exiledSector/starCharts/en.json")).thenReturn(new JSONObject("{\"b\":\"Charts B\"}"));

        I18n.load("en");

        assertEquals("Library A", Translation.text("a"));
        assertEquals("Charts B", Translation.text("b"));
        assertEquals(List.of(), errors);
    }

    @Test
    void aMissingEnglishFileLeavesAnEmptyCatalogueAndIsReported() throws Exception {
        when(settings.getMergedJSON("data/strings/exiledSector/core/en.json")).thenThrow(notFound("en"));

        I18n.load("en");

        assertEquals("[[a]]", Translation.text("a"));
        assertEquals(1, errors.size());
    }

    @Test
    void aBrokenTranslationFileIsReportedAndItsStringsFallBackToEnglish() throws Exception {
        when(settings.getMergedJSON("data/strings/exiledSector/core/en.json")).thenReturn(new JSONObject("{\"a\":\"English A\"}"));
        when(settings.getMergedJSON("data/strings/exiledSector/core/zh.json")).thenThrow(notFound("zh"));
        when(settings.getMergedJSON("data/strings/exiledSector/core/zh_CN.json"))
                .thenThrow(new JSONException("mods/ExiledSector (data/strings/exiledSector/core/zh_CN.json)\nExpected a ',' or '}'"));

        I18n.load("zh_CN");

        assertEquals("English A", Translation.text("a"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getRenderedMessage().contains("zh_CN"));
        assertTrue(errors.get(0).getRenderedMessage().contains("data/strings/exiledSector/core/zh_CN.json"));
    }

    @Test
    void anUnexpectedFailureReadingATranslationIsReportedRatherThanTreatedAsMissing() throws Exception {
        when(settings.getMergedJSON("data/strings/exiledSector/core/en.json")).thenReturn(new JSONObject("{\"a\":\"English A\"}"));
        when(settings.getMergedJSON("data/strings/exiledSector/core/ru.json")).thenThrow(new IllegalStateException("disk on fire"));

        I18n.load("ru");

        assertEquals("English A", Translation.text("a"));
        assertEquals(1, errors.size());
    }

    @Test
    void gameDrawnTextUsesTheGameCatalogueOnlyInsideItsScope() {
        I18n.install(new Catalogue("zh_CN", Map.of("a", "中文")), new Catalogue("en", Map.of("a", "English")));

        assertEquals("中文", Translation.text("a"));
        assertEquals("English", I18n.forGameText(() -> Translation.text("a")));
        assertEquals("English", Translation.gameText("a"));
        assertThrows(IllegalStateException.class, () -> I18n.forGameText(() -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals("中文", Translation.text("a"));
        assertEquals(new Languages("zh_CN", "en"), I18n.languages());
    }

    @Test
    void ignoresValuesThatAreNotText() throws Exception {
        when(settings.getMergedJSON("data/strings/exiledSector/core/en.json"))
                .thenReturn(new JSONObject("{\"a\":\"text\",\"b\":3,\"c\":{\"d\":\"e\"}}"));

        I18n.load("en");

        assertEquals(1, I18n.catalogue().size());
    }
}
