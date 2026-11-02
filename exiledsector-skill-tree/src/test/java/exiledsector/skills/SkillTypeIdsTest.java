package exiledsector.skills;

import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillTypeIdsTest {

    private static final List<String> KEPT_ID_PREFIXES = List.of("nskr_", "rat_", "tania_");
    private static final String DOCTRINE_PREFIX = "doctrine_";

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.load();
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    static String slug(String displayName) {
        return displayName.toLowerCase(Locale.ROOT).replaceAll("['’]", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static boolean keepsItsId(SkillType type) {
        return type.getTier() == SkillTier.ROOT || KEPT_ID_PREFIXES.stream().anyMatch(type.getId()::startsWith);
    }

    @Test
    void slugsDropApostrophesAndJoinWordsWithUnderscores() {
        assertEquals("lions_gaze", slug("Lion's Gaze"));
        assertEquals("shield_conversion_omni", slug("Shield Conversion - Omni"));
        assertEquals("alpha_grade_fire_control", slug("Alpha-Grade Fire Control"));
    }

    @Test
    void everySkillTypeIdIsItsEnglishDisplayNameSoTheNameInGameFindsTheCode() {
        List<String> drifted = SkillTree.getAllTypes().values().stream()
                .filter(type -> !keepsItsId(type))
                .filter(type -> {
                    String expected = slug(type.getSourceName());
                    return !type.getId().equals(expected) && !type.getId().equals(DOCTRINE_PREFIX + expected);
                })
                .map(type -> type.getId() + " (" + type.getSourceName() + ")")
                .sorted()
                .toList();

        assertEquals(List.of(), drifted, "Rename these skill types to match their display names, through "
                + NodeReplacements.TYPE_RENAMES_PATH + " and " + NodeReplacements.NODE_RENAMES_PATH + " if they shipped in a release.");
    }
}
