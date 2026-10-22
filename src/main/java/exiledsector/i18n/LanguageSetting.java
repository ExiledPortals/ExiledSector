package exiledsector.i18n;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.HostMod;
import exiledsector.ModSettings;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LanguageSetting {

    public static final String AUTO = "Auto";
    public static final String ENGLISH = "English";
    public static final String SIMPLIFIED_CHINESE = "Simplified Chinese";
    public static final List<String> OPTIONS = List.of(AUTO, ENGLISH, SIMPLIFIED_CHINESE);

    private static final Set<String> CJK_LANGUAGES = Set.of("zh", "ja", "ko");
    private static final String CJK_MODE_SETTING = "cjkMode";

    private LanguageSetting() {
    }

    public static String fieldId() {
        return HostMod.settingId("language");
    }

    public static String selected() {
        return ModSettings.stringOr(fieldId(), AUTO);
    }

    public static Languages resolve(String option, Locale jvmLocale, boolean gameRendersCjk) {
        String gameLocale = LocaleChain.of(jvmLocale);
        String uiLocale;
        if (option != null && option.startsWith(ENGLISH)) {
            uiLocale = LocaleChain.ENGLISH;
        } else if (option != null && option.startsWith(SIMPLIFIED_CHINESE)) {
            uiLocale = LocaleChain.SIMPLIFIED_CHINESE;
        } else {
            uiLocale = gameLocale;
        }
        boolean uiNeedsCjk = CJK_LANGUAGES.contains(LocaleChain.language(uiLocale));
        return new Languages(uiLocale, uiNeedsCjk && !gameRendersCjk ? LocaleChain.ENGLISH : uiLocale);
    }

    public static boolean gameRendersCjk(Locale jvmLocale) {
        if (jvmLocale != null && CJK_LANGUAGES.contains(jvmLocale.getLanguage())) {
            return true;
        }
        try {
            SettingsAPI settings = Global.getSettings();
            return settings != null && settings.getBoolean(CJK_MODE_SETTING);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
