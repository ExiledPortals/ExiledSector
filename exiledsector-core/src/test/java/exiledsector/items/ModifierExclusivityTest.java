package exiledsector.items;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierExclusivityTest {

    private static final String GROUP = "crippled";
    private static final Set<String> GROUPED = Set.of("GROUPED_ARMOR", "GROUPED_HULL", "GROUPED_FLUX", "GROUPED_SHIELD");

    private ItemDefinition definition;

    private static JSONObject row(String id, String slot, String lines, String groups) throws JSONException {
        return HullItemRows.modifierRow(id, slot, lines, id).put(ItemModifiers.EXCLUSIVE_GROUPS_COLUMN, groups);
    }

    @BeforeEach
    void register() throws JSONException {
        ItemModifiers.register(new JSONArray()
                .put(row("GROUPED_ARMOR", "prefix", "ARMOR_PERCENT:5:10", GROUP))
                .put(row("GROUPED_HULL", "prefix", "HULL_PERCENT:5:10", GROUP + "; other"))
                .put(row("GROUPED_FLUX", "prefix", "FLUX_CAPACITY_MULT:5:10", GROUP))
                .put(row("FREE_DISSIPATION", "prefix", "FLUX_DISSIPATION_MULT:5:10", ""))
                .put(row("GROUPED_SHIELD", "suffix", "SHIELD_DAMAGE_TAKEN_MULT:-10:-5", GROUP))
                .put(row("FREE_HULL", "suffix", "HULL_MULT:5:10", "")));
        definition = new ItemDefinition("frame", ItemKind.BRIDGE, "Frame", ItemDefinition.FALLBACK_ICON, 1f, false, "",
                entries("GROUPED_ARMOR", "GROUPED_HULL", "GROUPED_FLUX", "FREE_DISSIPATION"), entries("GROUPED_SHIELD", "FREE_HULL"), "");
        ItemDefinitions.register(List.of(definition));
    }

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static List<PoolEntry> entries(String... modifierIds) {
        return Arrays.stream(modifierIds).map(modifierId -> new PoolEntry(ItemModifiers.get(modifierId), 1f)).toList();
    }

    private static long groupedCount(List<RolledModifier> modifiers) {
        return modifiers.stream().filter(modifier -> GROUPED.contains(modifier.modifierId())).count();
    }

    @Test
    void theCatalogueReadsEachModifiersGroupsAndModifiersInNoGroupNeverClash() {
        assertEquals(Set.of(GROUP, "other"), ItemModifiers.exclusiveGroups("GROUPED_HULL"));
        assertEquals(Set.of(), ItemModifiers.exclusiveGroups("FREE_HULL"));
        assertTrue(ModifierExclusivity.clashes("GROUPED_ARMOR", List.of("FREE_HULL", "GROUPED_SHIELD")));
        assertFalse(ModifierExclusivity.clashes("GROUPED_ARMOR", List.of("GROUPED_ARMOR", "FREE_HULL")));
        assertFalse(ModifierExclusivity.clashes("FREE_DISSIPATION", List.of("GROUPED_ARMOR", "FREE_HULL")));
        assertThrows(IllegalArgumentException.class, () -> ItemModifiers.parseExclusiveGroups("GROUPED_ARMOR", "bad group"));
    }

    @Test
    void aRollNeverDrawsTwoModifiersOfOneGroupOnEitherSide() {
        boolean rolledOne = false;
        for (long seed = 0; seed < 200; seed++) {
            List<RolledModifier> rolled = ItemRoller.rollWithEffectCount(definition, seed, 4);

            assertTrue(groupedCount(rolled) <= 1, "seed " + seed + " rolled " + rolled);
            rolledOne |= groupedCount(rolled) == 1;
        }
        assertTrue(rolledOne);
    }

    @Test
    void addingAModifierSkipsEveryModifierThatSharesAGroupWithOneAlreadyThere() {
        RolledModifier armor = RolledModifier.roll(ItemModifiers.get("GROUPED_ARMOR"), new Random(1L));

        List<String> open = AffixLayout.openSides(definition, List.of(armor)).stream().flatMap(List::stream)
                .map(entry -> entry.modifier().id()).toList();

        assertEquals(List.of("FREE_DISSIPATION", "FREE_HULL"), open);
    }
}
