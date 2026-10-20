package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.radius.RadiusSize;
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

    private static DoctrineArchive augmentingDoctrine(List<PoolEntry> pool, int minLines, int maxLines) {
        return new DoctrineArchive("odds_doctrine", DoctrineBehaviour.AUGMENT, RadiusSize.LARGE, List.of(), List.of(), List.of(), pool,
                minLines, maxLines, null);
    }

    @Test
    void aDoctrineLineShowsTheChanceThatAnyOneNodeGetsIt() {
        List<PoolEntry> pool = List.of(PoolEntry.of(ModifierSlot.PREFIX, "D1", 1f, 2f, 3f), PoolEntry.of(ModifierSlot.PREFIX, "D2", 1f, 2f, 1f),
                PoolEntry.of(ModifierSlot.PREFIX, "D3", 1f, 2f, 0f));

        Map<String, Double> oneLine = ItemRollOdds.doctrineLineChances(augmentingDoctrine(pool, 1, 1));
        assertEquals(List.of("D1", "D2", "D3"), List.copyOf(oneLine.keySet()));
        assertEquals(0.75, oneLine.get("D1"), EXACT);
        assertEquals(0.25, oneLine.get("D2"), EXACT);
        assertEquals(0.0, oneLine.get("D3"), EXACT);

        Map<String, Double> oneOrTwoLines = ItemRollOdds.doctrineLineChances(augmentingDoctrine(pool, 1, 2));
        assertEquals(0.875, oneOrTwoLines.get("D1"), EXACT);
        assertEquals(0.625, oneOrTwoLines.get("D2"), EXACT);
        assertEquals(0.0, oneOrTwoLines.get("D3"), EXACT);

        List<PoolEntry> even = List.of(PoolEntry.of(ModifierSlot.PREFIX, "E1", 1f, 2f, 1f), PoolEntry.of(ModifierSlot.PREFIX, "E2", 1f, 2f, 1f),
                PoolEntry.of(ModifierSlot.PREFIX, "E3", 1f, 2f, 1f), PoolEntry.of(ModifierSlot.PREFIX, "E4", 1f, 2f, 1f));
        Map<String, Double> twoOfFour = ItemRollOdds.doctrineLineChances(augmentingDoctrine(even, 2, 2));
        twoOfFour.values().forEach(chance -> assertEquals(0.5, chance, EXACT));
        Map<String, Double> moreLinesThanEntries = ItemRollOdds.doctrineLineChances(augmentingDoctrine(even, 5, 6));
        moreLinesThanEntries.values().forEach(chance -> assertEquals(1.0, chance, EXACT));
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
