package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagInhibitorWeightsTest {

    private static PoolEntry tagged(String effectName, float weight, String... tags) {
        return new PoolEntry(new Modifier(effectName, ModifierSlot.PREFIX, List.of(ModifierLine.range(effectName, 4f, 6f)), Set.of(tags), false), weight);
    }

    private static int beamRolls(TagWeights favouredTags) {
        List<List<PoolEntry>> sides = List.of(List.of(tagged("BEAM_WEAPON_DAMAGE_PERCENT", 1f, "beam"), tagged("HULL_MULT", 4f, "hull"),
                tagged("ARMOR_PERCENT", 4f, "armour")));
        int beamRolls = 0;
        for (long seed = 0; seed < 400; seed++) {
            if ("BEAM_WEAPON_DAMAGE_PERCENT".equals(AffixLayout.rollFrom(sides, new Random(seed), favouredTags).modifierId())) {
                beamRolls++;
            }
        }
        return beamRolls;
    }

    @Test
    void aTagInhibitorMakesItsTagFarMoreLikelyToRoll() {
        int unweighted = beamRolls(TagWeights.NONE);
        int favoured = beamRolls(CraftingInhibitor.favouredTags(List.of(CraftingInhibitor.BEAM)));

        assertTrue(favoured > unweighted * 2, unweighted + " -> " + favoured);
    }

    @Test
    void severalTagInhibitorsFavourEachTagWithoutStackingOnAModifierCarryingBoth() {
        TagWeights favouredTags = CraftingInhibitor.favouredTags(List.of(CraftingInhibitor.BEAM, CraftingInhibitor.ARMOUR, CraftingInhibitor.PREFIX));

        assertEquals(CraftingInhibitor.FAVOURED_TAG_MULTIPLIER, tagged("BEAM_WEAPON_DAMAGE_PERCENT", 1f, "beam").biasedWeight(favouredTags), 0.0001f);
        assertEquals(CraftingInhibitor.FAVOURED_TAG_MULTIPLIER, tagged("ARMOR_PERCENT", 1f, "armour", "beam").biasedWeight(favouredTags), 0.0001f);
        assertEquals(1f, tagged("HULL_MULT", 1f, "hull").biasedWeight(favouredTags), 0.0001f);
    }

    @Test
    void slotInhibitorsFavourNothingAndTagInhibitorsGuardNoSide() {
        assertSame(TagWeights.NONE, CraftingInhibitor.favouredTags(List.of(CraftingInhibitor.PREFIX, CraftingInhibitor.SUFFIX)));
        assertEquals(Set.of(ModifierSlot.PREFIX), CraftingInhibitor.inhibitedSlots(List.of(CraftingInhibitor.PREFIX, CraftingInhibitor.PHASE)));
        assertEquals(Set.of(CraftingInhibitor.PHASE), CraftingInhibitor.favouring(List.of(CraftingInhibitor.PREFIX, CraftingInhibitor.PHASE)));
        assertEquals(7, CraftingInhibitor.tagInhibitors().size());
        for (CraftingInhibitor inhibitor : CraftingInhibitor.tagInhibitors()) {
            assertNull(inhibitor.inhibitedSlot());
            assertTrue(!inhibitor.growable() && inhibitor.dropWeight() == 0f, inhibitor.name());
        }
    }
}
