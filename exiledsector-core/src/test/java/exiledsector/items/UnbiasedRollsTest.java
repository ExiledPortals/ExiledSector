package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnbiasedRollsTest {

    private ItemDefinition definition;

    @BeforeEach
    void register() {
        List<PoolEntry> prefixes = List.of(
                PoolEntry.of(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 4f, 6f, 1f),
                PoolEntry.of(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 4f, 6f, 3.5f),
                PoolEntry.of(ModifierSlot.PREFIX, "BEAM_WEAPON_DAMAGE_PERCENT", 10f, 15f, 0.3f),
                PoolEntry.of(ModifierSlot.PREFIX, "ARMOR_PERCENT", 6f, 9f, 7f));
        List<PoolEntry> suffixes = List.of(
                PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 2f),
                PoolEntry.of(ModifierSlot.SUFFIX, "HULL_MULT", 4f, 6f, 0.7f),
                PoolEntry.of(ModifierSlot.SUFFIX, "HULL_PERCENT", 5f, 10f, 4.2f));
        definition = new ItemDefinition("weighted_frame", ItemKind.BRIDGE, "Frame", ItemDefinition.FALLBACK_ICON, 1f, false, "", prefixes, suffixes, "");
        ItemDefinitions.register(List.of(definition));
    }

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
    }

    private List<String> rolledIds() {
        List<String> rolledIds = new ArrayList<>();
        for (long seed = 0; seed < 12; seed++) {
            rolledIds.add(ItemRoller.rollWithModifierCount(definition, seed, 3).stream().map(RolledModifier::modifierId).toList().toString());
        }
        return rolledIds;
    }

    private List<String> addedIds() {
        return addedIds(AffixLayout.openSides(definition, List.of()), TagWeights.NONE);
    }

    private static List<String> addedIds(List<List<PoolEntry>> sides, TagWeights tagWeights) {
        List<String> addedIds = new ArrayList<>();
        for (long seed = 0; seed < 12; seed++) {
            addedIds.add(AffixLayout.rollFrom(sides, new Random(seed), tagWeights).modifierId());
        }
        return addedIds;
    }

    private static PoolEntry tagged(String effectName, float weight, String... tags) {
        return new PoolEntry(new Modifier(effectName, ModifierSlot.SUFFIX, List.of(ModifierLine.range(effectName, 4f, 6f)), Set.of(tags), false), weight);
    }

    @Test
    void seededRollsAndCraftedAdditionsDrawTheSameModifiersAsBeforeTagWeights() {
        assertEquals(List.of(
                "[ARMOR_PERCENT, SHIELD_DAMAGE_TAKEN_MULT, HULL_PERCENT]",
                "[FLUX_DISSIPATION_MULT, ARMOR_PERCENT, HULL_PERCENT]",
                "[FLUX_DISSIPATION_MULT, ARMOR_PERCENT, HULL_PERCENT]",
                "[FLUX_DISSIPATION_MULT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[FLUX_DISSIPATION_MULT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[FLUX_DISSIPATION_MULT, ARMOR_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[ARMOR_PERCENT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[FLUX_DISSIPATION_MULT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[ARMOR_PERCENT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[ARMOR_PERCENT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[ARMOR_PERCENT, HULL_PERCENT, SHIELD_DAMAGE_TAKEN_MULT]",
                "[FLUX_DISSIPATION_MULT, ARMOR_PERCENT, HULL_PERCENT]"), rolledIds());
        assertEquals(List.of("HULL_PERCENT", "SHIELD_DAMAGE_TAKEN_MULT", "HULL_MULT", "HULL_PERCENT", "HULL_PERCENT", "SHIELD_DAMAGE_TAKEN_MULT",
                "HULL_MULT", "HULL_PERCENT", "HULL_PERCENT", "SHIELD_DAMAGE_TAKEN_MULT", "HULL_PERCENT", "HULL_PERCENT"), addedIds());
    }

    @Test
    void theNoSourcesWeightIsThePoolWeightUnchanged() {
        for (float poolWeight : new float[]{0.3f, 1f, 3.5f, 7f, 1e-6f, 12345.678f}) {
            assertEquals(Float.floatToIntBits(poolWeight), Float.floatToIntBits(tagged("HULL_MULT", poolWeight, "hull").biasedWeight(TagWeights.NONE)));
        }
    }

    @Test
    void aSourceThatMatchesNoTagLeavesCraftedAdditionsAsTheyWere() {
        List<List<PoolEntry>> sides = List.of(List.of(tagged("HULL_MULT", 1f, "hull"), tagged("ARMOR_PERCENT", 3f, "armour"),
                tagged("SHIELD_DAMAGE_TAKEN_MULT", 0.5f, "shield")));

        assertEquals(addedIds(sides, TagWeights.NONE), addedIds(sides, TagWeights.NONE.with(Map.of("weapon", 0f, "cargo", 10f))));
    }

    @Test
    void aBlockedTagNeverRollsAndOtherModifiersKeepRolling() {
        List<List<PoolEntry>> sides = List.of(List.of(tagged("HULL_MULT", 1f, "hull"), tagged("ARMOR_PERCENT", 30f, "armour"),
                tagged("SHIELD_DAMAGE_TAKEN_MULT", 0.5f, "shield")));

        List<String> addedIds = addedIds(sides, TagWeights.NONE.with(Map.of("armour", 0f)));

        assertFalse(addedIds.contains("ARMOR_PERCENT"));
        assertEquals(Set.of("HULL_MULT", "SHIELD_DAMAGE_TAKEN_MULT"), Set.copyOf(addedIds));
    }
}
