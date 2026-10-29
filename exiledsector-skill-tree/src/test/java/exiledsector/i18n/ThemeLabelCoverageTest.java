package exiledsector.i18n;

import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeLabelCoverageTest {

    @Test
    void everyThemeTagHasABuildLabelInEveryCatalogue() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String locale : List.of(LocaleChain.ENGLISH, "zh_CN")) {
            Map<String, String> strings = RealCatalogue.entries(locale);
            for (String theme : SkillTags.THEME) {
                if (!strings.containsKey("theme." + theme)) {
                    missing.add(locale + ": theme." + theme);
                }
            }
        }

        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }
}
