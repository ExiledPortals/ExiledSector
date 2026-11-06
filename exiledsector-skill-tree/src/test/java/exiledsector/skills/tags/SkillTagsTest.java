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
    void allIsThemeThenVolumeThenRequirementThenGateThenRule() {
        List<String> expected = new ArrayList<>(SkillTags.THEME);
        expected.addAll(SkillTags.VOLUME);
        expected.addAll(SkillTags.REQUIREMENT);
        expected.addAll(SkillTags.GATE);
        expected.addAll(SkillTags.RULE);

        assertEquals(expected, SkillTags.ALL);
    }

    @Test
    void noTagBelongsToMoreThanOneGroup() {
        assertEquals(SkillTags.ALL.size(), new HashSet<>(SkillTags.ALL).size());
    }

    @Test
    void theRetiredLuddicPathTagIsNotInTheVocabulary() {
        assertFalse(SkillTags.ALL.contains("luddic_path"));
        assertTrue(SkillTags.isVolume("luddic"));
    }

    @Test
    void isVolumeRecognisesOnlyVolumeTags() {
        assertTrue(SkillTags.isVolume("tritachyon"));
        assertFalse(SkillTags.isVolume("shield"));
        assertFalse(SkillTags.isVolume("req_shields"));
        assertFalse(SkillTags.isVolume(null));
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
