package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRollOddsTest {

    private static final double EXACT = 1e-9;

    private static ItemDefinition definition(boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition("odds", ItemKind.BRIDGE, "Odds", ItemDefinition.FALLBACK_ICON, 1f, unique, "",
                prefixes, suffixes, "");
    }

    @Test
    void eachModifierShowsItsShareOfItsOwnListsWeight() {
        ItemDefinition weighted = definition(false,
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "P1", 1f, 2f, 1500f), PoolEntry.of(ModifierSlot.PREFIX, "P2", 1f, 2f, 500f)),
                List.of(PoolEntry.of(ModifierSlot.SUFFIX, "S1", 1f, 2f, 1f), PoolEntry.of(ModifierSlot.SUFFIX, "S2", 1f, 2f, 3f)));

        Map<String, Double> shares = ItemRollOdds.weightShares(weighted);

        assertEquals(List.of("P1", "P2", "S1", "S2"), List.copyOf(shares.keySet()));
        assertEquals(0.75, shares.get("P1"), EXACT);
        assertEquals(0.25, shares.get("P2"), EXACT);
        assertEquals(0.25, shares.get("S1"), EXACT);
        assertEquals(0.75, shares.get("S2"), EXACT);
    }

    @Test
    void aRangeIsWrittenFromWeakestToStrongest() {
        String range = new RolledEffect("HULL_MULT", 6f).rangeDescription(PoolEntry.of(ModifierSlot.PREFIX, "HULL_MULT", 4f, 6f, 1f).line()).plain();
        String fixed = new RolledEffect("HULL_MULT", 5f).rangeDescription(PoolEntry.of(ModifierSlot.PREFIX, "HULL_MULT", 5f, 5f, 1f).line()).plain();
        String scaled = new RolledEffect("ARMOR_FLAT", 5f).rangeDescription(PoolEntry.perHullSize(ModifierSlot.PREFIX, "ARMOR_FLAT", 1f, List.of(5f, 10f, 15f, 20f)).line())
                .plain();

        assertTrue(range.contains("4 to 6%"), range);
        assertTrue(fixed.contains("5%") && !fixed.contains(" to "), fixed);
        assertTrue(scaled.contains("5/10/15/20"), scaled);

        String twoValues = new RolledEffect("FLUX_SCALED_RATE_OF_FIRE_PERCENT", 13f)
                .rangeDescription(PoolEntry.of(ModifierSlot.PREFIX, "FLUX_SCALED_RATE_OF_FIRE_PERCENT", 5f, 13f, 1f).line()).plain();
        assertEquals(2, twoValues.chars().filter(character -> character == '(').count(), twoValues);
        String untaggedBefore = new RolledEffect("BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT", 9f)
                .rangeDescription(PoolEntry.of(ModifierSlot.PREFIX, "BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT", 5f, 9f, 1f).line()).plain();
        assertTrue(untaggedBefore.contains("5 to 9%"), untaggedBefore);
    }
}
