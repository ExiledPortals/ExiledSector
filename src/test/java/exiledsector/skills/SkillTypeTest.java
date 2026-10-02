package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SkillTypeTest {

    private static SkillType type(List<SkillTypeEffect> effects, List<HullSizeSkillEffect> hullSizeEffects) {
        return new SkillType.Builder("id", "Name", "a.png", SkillTier.SMALL)
                .effects(effects)
                .hullSizeEffects(hullSizeEffects)
                .build();
    }

    @Test
    void listsEachRegularEffectWithItsMagnitude() {
        SkillEffect a = mock(SkillEffect.class);
        SkillEffect b = mock(SkillEffect.class);
        SkillType type = type(List.of(new SkillTypeEffect(a, 1f), new SkillTypeEffect(b, 2f)), List.of());

        assertEquals(List.of(new SkillTypeEffect(a, 1f), new SkillTypeEffect(b, 2f)), type.effectsFor(HullSize.FRIGATE));
    }

    @Test
    void listsEachHullSizeEffectResolvedForTheGivenHullSize() {
        SkillEffect effect = mock(SkillEffect.class);
        SkillType type = type(List.of(), List.of(new HullSizeSkillEffect(effect, 1f, 2f, 3f, 4f)));

        assertEquals(List.of(new SkillTypeEffect(effect, 3f)), type.effectsFor(HullSize.CRUISER));
    }

    @Test
    void listsRegularEffectsBeforeHullSizeEffects() {
        SkillEffect regular = mock(SkillEffect.class);
        SkillEffect sized = mock(SkillEffect.class);
        SkillType type = type(List.of(new SkillTypeEffect(regular, 1f)),
                List.of(new HullSizeSkillEffect(sized, 1f, 1f, 1f, 1f)));

        assertEquals(List.of(new SkillTypeEffect(regular, 1f), new SkillTypeEffect(sized, 1f)), type.effectsFor(HullSize.FRIGATE));
    }

    @Test
    void anExclusivityListedOnEitherSideCountsBothWays() {
        SkillType lister = new SkillType.Builder("a", "A", "a.png", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of("b")).build();
        SkillType listed = new SkillType.Builder("b", "B", "a.png", SkillTier.NOTABLE).build();
        SkillType unrelated = new SkillType.Builder("c", "C", "a.png", SkillTier.NOTABLE).build();

        assertTrue(lister.isExclusiveWith(listed));
        assertTrue(listed.isExclusiveWith(lister));
        assertFalse(listed.isExclusiveWith(unrelated));
    }

    @Test
    void phantomHullModsAreAlsoExclusiveWithoutBeingListedTwice() {
        SkillType type = new SkillType.Builder("t", "T", "a.png", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("frontshield", "militarized_subsystems"))
                .phantomHullModIds(List.of("militarized_subsystems"))
                .build();

        assertEquals(List.of("frontshield", "militarized_subsystems"), type.getExclusiveHullModIds());
        assertEquals(List.of("militarized_subsystems"), type.getPhantomHullModIds());
    }

    @Test
    void tagsDefaultToAnEmptyListWhenNotSetOrSetToNull() {
        SkillType unset = new SkillType.Builder("t", "T", "a.png", SkillTier.SMALL).build();
        SkillType nulled = new SkillType.Builder("t", "T", "a.png", SkillTier.SMALL).tags(null).build();

        assertEquals(List.of(), unset.getTags());
        assertEquals(List.of(), nulled.getTags());
    }

    @Test
    void keepsTagsInTheGivenOrder() {
        SkillType type = new SkillType.Builder("t", "T", "a.png", SkillTier.SMALL)
                .tags(List.of("shield", "req_shields"))
                .build();

        assertEquals(List.of("shield", "req_shields"), type.getTags());
    }

    @Test
    void listsNothingWhenBothListsAreEmpty() {
        assertTrue(type(List.of(), List.of()).effectsFor(HullSize.FRIGATE).isEmpty());
    }

    @Test
    void theEquivalentHullmodIsTheVanillaHullmodWhenSet() {
        SkillType type = new SkillType.Builder("t", "T", "a.png", SkillTier.KEYSTONE)
                .vanillaHullModId("ballistic_rangefinder")
                .exclusiveHullModIds(List.of("other"))
                .build();

        assertEquals("ballistic_rangefinder", type.getEquivalentHullModId());
    }

    @Test
    void theEquivalentHullmodIsOtherwiseTheFirstExclusiveHullmod() {
        SkillType type = new SkillType.Builder("t", "T", "a.png", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("dedicated_targeting_core", "targetingunit"))
                .build();

        assertEquals("dedicated_targeting_core", type.getEquivalentHullModId());
    }

    @Test
    void aTypeWithoutHullmodLinksHasNoEquivalentHullmod() {
        SkillType type = new SkillType.Builder("t", "T", "a.png", SkillTier.SMALL)
                .phantomHullModIds(List.of("militarized_subsystems"))
                .build();

        assertNull(type.getEquivalentHullModId());
    }
}
