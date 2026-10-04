package exiledsector.skills.tags;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTagsTest {

    @Test
    void allIsThemeThenRegionThenRequirement() {
        List<String> expected = new ArrayList<>(SkillTags.THEME);
        expected.addAll(SkillTags.REGION);
        expected.addAll(SkillTags.REQUIREMENT);

        assertEquals(expected, SkillTags.ALL);
    }

    @Test
    void noTagBelongsToMoreThanOneGroup() {
        assertEquals(SkillTags.ALL.size(), new HashSet<>(SkillTags.ALL).size());
    }

    @Test
    void theRetiredLuddicPathTagIsNotInTheVocabulary() {
        assertFalse(SkillTags.ALL.contains("luddic_path"));
        assertTrue(SkillTags.isRegion("luddic"));
    }

    @Test
    void isRegionRecognisesOnlyRegionTags() {
        assertTrue(SkillTags.isRegion("tritachyon"));
        assertFalse(SkillTags.isRegion("shield"));
        assertFalse(SkillTags.isRegion("req_shields"));
        assertFalse(SkillTags.isRegion(null));
    }

    @Test
    void isRequirementRecognisesOnlyRequirementTags() {
        assertTrue(SkillTags.isRequirement("req_shields"));
        assertTrue(SkillTags.isRequirement("campaign_only"));
        assertFalse(SkillTags.isRequirement("shield"));
        assertFalse(SkillTags.isRequirement("core"));
        assertFalse(SkillTags.isRequirement(null));
    }
}
