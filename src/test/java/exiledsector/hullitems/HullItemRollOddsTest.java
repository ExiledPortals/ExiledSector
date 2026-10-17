package exiledsector.hullitems;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemRollOddsTest {

    private static final double EXACT = 1e-9;

    private static HullItemDefinition definition(boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new HullItemDefinition("odds", SocketType.SUBROUTINE, "Odds", HullItemDefinition.FALLBACK_ICON, 1f, unique, "",
                prefixes, suffixes, "");
    }

    @Test
    void eachModifierShowsItsShareOfItsOwnListsWeight() {
        HullItemDefinition weighted = definition(false,
                List.of(new PoolEntry("P1", 1f, 2f, 1500f), new PoolEntry("P2", 1f, 2f, 500f)),
                List.of(new PoolEntry("S1", 1f, 2f, 1f), new PoolEntry("S2", 1f, 2f, 3f)));

        Map<String, Double> shares = HullItemRollOdds.weightShares(weighted);

        assertEquals(List.of("P1", "P2", "S1", "S2"), List.copyOf(shares.keySet()));
        assertEquals(0.75, shares.get("P1"), EXACT);
        assertEquals(0.25, shares.get("P2"), EXACT);
        assertEquals(0.25, shares.get("S1"), EXACT);
        assertEquals(0.75, shares.get("S2"), EXACT);
    }

    @Test
    void aRangeIsWrittenFromWeakestToStrongest() {
        String range = new RolledEffect("HULL_MULT", 6f).rangeDescription(new PoolEntry("HULL_MULT", 4f, 6f, 1f)).plain();
        String fixed = new RolledEffect("HULL_MULT", 5f).rangeDescription(new PoolEntry("HULL_MULT", 5f, 5f, 1f)).plain();
        String scaled = new RolledEffect("ARMOR_FLAT", 5f).rangeDescription(PoolEntry.perHullSize("ARMOR_FLAT", 1f, List.of(5f, 10f, 15f, 20f)))
                .plain();

        assertTrue(range.contains("4 to 6%"), range);
        assertTrue(fixed.contains("5%") && !fixed.contains(" to "), fixed);
        assertTrue(scaled.contains("5/10/15/20"), scaled);

        String twoValues = new RolledEffect("FLUX_SCALED_RATE_OF_FIRE_PERCENT", 13f)
                .rangeDescription(new PoolEntry("FLUX_SCALED_RATE_OF_FIRE_PERCENT", 5f, 13f, 1f)).plain();
        assertEquals(2, twoValues.chars().filter(character -> character == '(').count(), twoValues);
        String untaggedBefore = new RolledEffect("BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT", 9f)
                .rangeDescription(new PoolEntry("BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT", 5f, 9f, 1f)).plain();
        assertTrue(untaggedBefore.contains("5 to 9%"), untaggedBefore);
    }
}
