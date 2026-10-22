package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledlib.effects.RealVariantTags;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SkillConflictWarningsTest {

    @Test
    void recordingForTwoDifferentVariantsKeepsBothEntries() {
        ShipVariantAPI a = mock(ShipVariantAPI.class);
        ShipVariantAPI b = mock(ShipVariantAPI.class);
        RealVariantTags.attach(a);
        RealVariantTags.attach(b);

        SkillConflictWarnings.recordRemoval(a, "adaptiveshields", "frontemitter");
        SkillConflictWarnings.recordRemoval(b, "armoredcladding", "hull");

        assertEquals(new SkillConflictWarnings.Removal("adaptiveshields", "frontemitter"), SkillConflictWarnings.get(a));
        assertEquals(new SkillConflictWarnings.Removal("armoredcladding", "hull"), SkillConflictWarnings.get(b));
    }

    @Test
    void clearRemovesOnlyTheGivenVariantsEntry() {
        ShipVariantAPI a = mock(ShipVariantAPI.class);
        ShipVariantAPI b = mock(ShipVariantAPI.class);
        RealVariantTags.attach(a);
        RealVariantTags.attach(b);
        SkillConflictWarnings.recordRemoval(a, "adaptiveshields", "frontemitter");
        SkillConflictWarnings.recordRemoval(b, "armoredcladding", "hull");

        SkillConflictWarnings.clear(a);

        assertNull(SkillConflictWarnings.get(a));
        assertEquals("armoredcladding", SkillConflictWarnings.get(b).removedHullModId());
    }

    @Test
    void aNewRemovalReplacesTheOldOneAndLeavesOtherTagsAlone() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        Set<String> tags = RealVariantTags.attach(variant);
        tags.add("some_other_tag");

        SkillConflictWarnings.recordRemoval(variant, "adaptiveshields", "frontemitter");
        SkillConflictWarnings.recordRemoval(variant, "armoredcladding", "hull");

        assertEquals(new SkillConflictWarnings.Removal("armoredcladding", "hull"), SkillConflictWarnings.get(variant));
        assertEquals(2, tags.size());
        assertTrue(tags.contains("some_other_tag"));
    }

    @Test
    void getReturnsNullForAVariantThatWasNeverRecorded() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        RealVariantTags.attach(variant);

        assertNull(SkillConflictWarnings.get(variant));
    }
}
