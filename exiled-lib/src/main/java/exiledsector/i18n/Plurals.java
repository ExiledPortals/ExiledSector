package exiledsector.i18n;

import java.util.Set;

final class Plurals {

    static final String ONE = "one";
    static final String OTHER = "other";

    private static final Set<String> LANGUAGES_WITHOUT_PLURALS = Set.of("zh", "ja", "ko");

    private Plurals() {
    }

    static String category(String locale, int count) {
        if (LANGUAGES_WITHOUT_PLURALS.contains(LocaleChain.language(locale))) {
            return OTHER;
        }
        return count == 1 ? ONE : OTHER;
    }
}
