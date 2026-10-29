package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageSettingTest {

    @Test
    void autoFollowsTheGameLocaleEverywhere() {
        assertEquals(new Languages("en_US", "en_US"), LanguageSetting.resolve(LanguageSetting.AUTO, Locale.US, false));
        assertEquals(new Languages("zh_CN", "zh_CN"), LanguageSetting.resolve(LanguageSetting.AUTO, Locale.SIMPLIFIED_CHINESE, true));
    }

    @Test
    void forcingChineseOnAGameWithoutChineseFontsKeepsGameDrawnTextInEnglish() {
        assertEquals(new Languages("zh_CN", "en"), LanguageSetting.resolve(LanguageSetting.SIMPLIFIED_CHINESE, Locale.US, false));
    }

    @Test
    void forcingChineseOnAGameThatCanDrawItUsesChineseEverywhere() {
        assertEquals(new Languages("zh_CN", "zh_CN"), LanguageSetting.resolve(LanguageSetting.SIMPLIFIED_CHINESE, Locale.US, true));
    }

    @Test
    void forcingEnglishOverridesAChineseGame() {
        assertEquals(new Languages("en", "en"), LanguageSetting.resolve(LanguageSetting.ENGLISH, Locale.SIMPLIFIED_CHINESE, true));
    }

    @Test
    void unknownOrDamagedOptionsFallBackToAutoOrMatchByPrefix() {
        assertEquals(new Languages("en_US", "en_US"), LanguageSetting.resolve("Klingon", Locale.US, false));
        assertEquals(new Languages("en_US", "en_US"), LanguageSetting.resolve(null, Locale.US, false));
        assertEquals(new Languages("zh_CN", "en"), LanguageSetting.resolve("Simplified Chinese (?)", Locale.US, false));
    }

    @Test
    void aChineseGameLocaleMeansTheGameCanDrawChinese() {
        assertTrue(LanguageSetting.gameRendersCjk(Locale.SIMPLIFIED_CHINESE));
    }
}
