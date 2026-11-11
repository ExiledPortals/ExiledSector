package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierTagsTest {

    @AfterEach
    void clear() {
        ItemModifiers.clear();
    }

    @Test
    void theTagsColumnListsTagsInOrderAndNoneMarksAModifierTagless() throws JSONException {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow("TAGGED_ARMOR", "prefix", "ARMOR_PERCENT:5:10", "Plated").put(ItemModifiers.TAGS_COLUMN, "defence; armour ;low_hull"))
                .put(HullItemRows.modifierRow("TAGLESS_HULL", "prefix", "HULL_PERCENT:5:10", "Sturdy").put(ItemModifiers.TAGS_COLUMN, " none "))
                .put(HullItemRows.modifierRow("UNTAGGED_FLUX", "prefix", "FLUX_CAPACITY_MULT:5:10", "Charged").put(ItemModifiers.TAGS_COLUMN, ""))
                .put(HullItemRows.modifierRow("NO_COLUMN_SHIELD", "suffix", "SHIELD_DAMAGE_TAKEN_MULT:-10:-5", "Warded")));

        Modifier tagged = ItemModifiers.get("TAGGED_ARMOR");
        assertEquals(List.of("defence", "armour", "low_hull"), List.copyOf(tagged.tags()));
        assertFalse(tagged.tagless());
        assertThrows(UnsupportedOperationException.class, () -> tagged.tags().add("hull"));

        assertEquals(Set.of(), ItemModifiers.get("TAGLESS_HULL").tags());
        assertTrue(ItemModifiers.get("TAGLESS_HULL").tagless());

        for (String unwrittenId : List.of("UNTAGGED_FLUX", "NO_COLUMN_SHIELD")) {
            assertEquals(Set.of(), ItemModifiers.get(unwrittenId).tags(), unwrittenId);
            assertFalse(ItemModifiers.get(unwrittenId).tagless(), unwrittenId);
        }
    }

    @Test
    void aRowMixingNoneWithTagsOrListingABadTagIsSkipped() throws JSONException {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow("MIXED_ARMOR", "prefix", "ARMOR_PERCENT:5:10", "Plated").put(ItemModifiers.TAGS_COLUMN, "none;armour"))
                .put(HullItemRows.modifierRow("BAD_HULL", "prefix", "HULL_PERCENT:5:10", "Sturdy").put(ItemModifiers.TAGS_COLUMN, "low hull"))
                .put(HullItemRows.modifierRow("GOOD_FLUX", "prefix", "FLUX_CAPACITY_MULT:5:10", "Charged").put(ItemModifiers.TAGS_COLUMN, "flux")));

        assertNull(ItemModifiers.get("MIXED_ARMOR"));
        assertNull(ItemModifiers.get("BAD_HULL"));
        assertNotNull(ItemModifiers.get("GOOD_FLUX"));
    }

    @Test
    void aTaglessModifierCannotAlsoCarryTagsAndUnwrittenTagsStayDistinctFromTagless() {
        List<ModifierLine> lines = List.of(ModifierLine.range("HULL_MULT", 4f, 6f));

        assertThrows(IllegalArgumentException.class, () -> new Modifier("HULL_MULT", ModifierSlot.PREFIX, lines, Set.of("hull"), true));
        assertEquals(new Modifier("HULL_MULT", ModifierSlot.PREFIX, lines), new Modifier("HULL_MULT", ModifierSlot.PREFIX, lines, null, false));
        assertFalse(new Modifier("HULL_MULT", ModifierSlot.PREFIX, lines).equals(new Modifier("HULL_MULT", ModifierSlot.PREFIX, lines, Set.of(), true)));
    }

    @Test
    void referencedPoolEntriesCarryTheCatalogueTags() throws JSONException {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow("TAGGED_ARMOR", "prefix", "ARMOR_PERCENT:5:10", "Plated").put(ItemModifiers.TAGS_COLUMN, "armour")));

        assertEquals(Set.of("armour"), PoolEntry.parse("@TAGGED_ARMOR:3", ModifierSlot.PREFIX, ItemType.HULL_ITEM).modifier().tags());
    }
}
