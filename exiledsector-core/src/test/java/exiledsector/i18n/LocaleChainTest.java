package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocaleChainTest {

    @Test
    void namesLocalesTheWayJavaAndStarsectorDo() {
        assertEquals("zh_CN", LocaleChain.of(Locale.SIMPLIFIED_CHINESE));
        assertEquals("en_US", LocaleChain.of(Locale.US));
        assertEquals("zh", LocaleChain.of(new Locale("zh")));
        assertEquals("en", LocaleChain.of(null));
    }

    @Test
    void fallsBackThroughTheLanguageToEnglish() {
        assertEquals(List.of("en_US", "en"), LocaleChain.highestPriorityFirst("en_US"));
        assertEquals(List.of("zh_CN", "zh", "en"), LocaleChain.highestPriorityFirst("zh_CN"));
        assertEquals(List.of("en"), LocaleChain.highestPriorityFirst("en"));
    }

    @Test
    void otherChineseLocalesFallBackToSimplifiedChineseBeforeEnglish() {
        assertEquals(List.of("zh", "zh_CN", "en"), LocaleChain.highestPriorityFirst("zh"));
        assertEquals(List.of("zh_TW", "zh", "zh_CN", "en"), LocaleChain.highestPriorityFirst("zh_TW"));
    }
}
