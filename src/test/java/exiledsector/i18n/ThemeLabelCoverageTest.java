package exiledsector.i18n;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeLabelCoverageTest {

    @Test
    void everyThemeTagHasABuildLabelInEveryCatalogue() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String catalogue : List.of("en.json", "zh_CN.json")) {
            JSONObject strings = RealSkillData.readJson(RealSkillData.projectRoot().resolve(Path.of("data/strings/exiledSector", catalogue)));
            for (String theme : SkillTags.THEME) {
                if (!strings.has("theme." + theme)) {
                    missing.add(catalogue + ": theme." + theme);
                }
            }
        }

        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }
}
