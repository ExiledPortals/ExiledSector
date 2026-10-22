package exiledsector.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LocaleChain {

    public static final String ENGLISH = "en";
    public static final String SIMPLIFIED_CHINESE = "zh_CN";

    private static final Map<String, String> LANGUAGE_ALIASES = Map.of("zh", SIMPLIFIED_CHINESE);

    private LocaleChain() {
    }

    public static String of(Locale locale) {
        if (locale == null || locale.getLanguage().isEmpty()) {
            return ENGLISH;
        }
        return locale.getCountry().isEmpty() ? locale.getLanguage() : locale.getLanguage() + "_" + locale.getCountry();
    }

    public static String language(String locale) {
        int separator = locale.indexOf('_');
        return separator < 0 ? locale : locale.substring(0, separator);
    }

    public static List<String> highestPriorityFirst(String locale) {
        List<String> chain = new ArrayList<>();
        add(chain, locale);
        String language = language(locale);
        add(chain, language);
        String alias = LANGUAGE_ALIASES.get(language);
        if (alias != null) {
            add(chain, alias);
        }
        add(chain, ENGLISH);
        return chain;
    }

    private static void add(List<String> chain, String locale) {
        if (!chain.contains(locale)) {
            chain.add(locale);
        }
    }
}
