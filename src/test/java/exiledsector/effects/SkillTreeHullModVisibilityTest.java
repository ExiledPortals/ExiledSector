package exiledsector.effects;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeHullModVisibilityTest {

    private static List<String> csvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());
        return fields;
    }

    @Test
    void theSkillTreeHullModShowsOnShipsButCannotBePickedBuiltInOrFoundInTheCodex() throws Exception {
        List<String> lines = Files.readAllLines(RealSkillData.projectRoot().resolve("data/hullmods/hull_mods.csv"), StandardCharsets.UTF_8);
        List<String> header = csvFields(lines.get(0));
        List<String> row = null;
        for (String line : lines) {
            List<String> fields = csvFields(line);
            if (fields.size() > 1 && fields.get(1).equals(SkillTreeHullMod.ID)) {
                row = fields;
            }
        }
        assertNotNull(row);

        assertEquals("TRUE", row.get(header.indexOf("hidden")).toUpperCase());
        assertFalse("TRUE".equalsIgnoreCase(row.get(header.indexOf("hiddenEverywhere"))));
        String tags = row.get(header.indexOf("tags"));
        assertTrue(tags.contains(Tags.HULLMOD_NO_BUILD_IN), tags);
        assertTrue(tags.contains(Tags.HIDE_IN_CODEX), tags);
        assertFalse(row.get(header.indexOf("sprite")).isEmpty());
    }

    @Test
    void theSkillTreeHullModCanNeverBeAddedOrRemovedByHand() {
        assertFalse(new SkillTreeHullMod().canBeAddedOrRemovedNow(null, null, null));
    }
}
