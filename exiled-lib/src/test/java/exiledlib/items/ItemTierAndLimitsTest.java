package exiledlib.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.modifiers.ModifierSlot;
import exiledlib.modifiers.ScaleContext;
import exiledlib.modifiers.TierRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTierAndLimitsTest {

    private static final PoolEntry CAPACITY = PoolEntry.of(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 4f, 6f, 1f);
    private static final PoolEntry DISSIPATION = PoolEntry.of(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 4f, 6f, 2f);
    private static final PoolEntry BEAMS = PoolEntry.of(ModifierSlot.PREFIX, "BEAM_WEAPON_DAMAGE_PERCENT", 10f, 15f, 1f);
    private static final PoolEntry HULL = PoolEntry.of(ModifierSlot.SUFFIX, "HULL_MULT", 4f, 6f, 1f);
    private static final PoolEntry ARMOR = PoolEntry.of(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 6f, 9f, 1f);
    private static final PoolEntry SHIELDS = PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 1f);
    private static final String GATED_EFFECT = "BEAM_WEAPON_DAMAGE_PERCENT";

    private static ItemDefinition definition(List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition("chip", ItemKind.SUBROUTINE, "Chip", ItemDefinition.FALLBACK_ICON, 1f, false, "", prefixes, suffixes, "");
    }

    private static final ItemDefinition UNGATED = definition(List.of(CAPACITY, DISSIPATION, BEAMS), List.of(HULL, ARMOR, SHIELDS));
    private static final ItemDefinition GATED = definition(List.of(CAPACITY, DISSIPATION, BEAMS.withTiers(TierRange.atLeast(5))),
            List.of(HULL, ARMOR, SHIELDS));

    @AfterEach
    void restore() {
        AffixLayout.register(ItemType.SUBROUTINE, null);
        ItemDefinitions.clear();
    }

    private static boolean rollsGatedEffect(List<RolledModifier> modifiers) {
        return RolledModifier.lines(modifiers).stream().anyMatch(line -> line.effectName().equals(GATED_EFFECT));
    }

    @Test
    void poolEntriesAllowEveryTierUnlessGiven() {
        assertSame(TierRange.ANY, CAPACITY.tiers());
        assertEquals(CAPACITY, new PoolEntry(CAPACITY.modifier(), CAPACITY.weight(), null));
        assertTrue(BEAMS.withTiers(TierRange.atLeast(5)).rollsAtTier(TierRange.NO_TIER));
        assertFalse(BEAMS.withTiers(TierRange.atLeast(5)).rollsAtTier(4));
        assertTrue(BEAMS.withTiers(TierRange.atLeast(5)).rollsAtTier(5));
    }

    @Test
    void rollingWithoutATierIgnoresTheTierGates() {
        for (long seed = 0; seed < 300; seed++) {
            assertEquals(ItemRoller.roll(UNGATED, seed, false), ItemRoller.roll(GATED, seed, false), "seed " + seed);
            assertEquals(ItemRoller.roll(UNGATED, seed, false), ItemRoller.roll(UNGATED, seed, false, 9), "seed " + seed);
        }
    }

    @Test
    void rollingAtATierLeavesOutEntriesGatedAboveIt() {
        boolean gatedRolledAtFive = false;
        for (long seed = 0; seed < 300; seed++) {
            assertFalse(rollsGatedEffect(ItemRoller.roll(GATED, seed, false, 4)), "seed " + seed);
            gatedRolledAtFive |= rollsGatedEffect(ItemRoller.roll(GATED, seed, false, 5));
        }
        assertTrue(gatedRolledAtFive);
    }

    @Test
    void augmentingAtATierOffersOnlyEntriesAllowedThere() {
        List<RolledModifier> none = List.of();

        assertEquals(3, AffixLayout.openSides(GATED, none).get(0).size());
        assertEquals(2, AffixLayout.openSides(GATED, none, 4).get(0).size());
        assertEquals(3, AffixLayout.openSides(GATED, none, 5).get(0).size());
    }

    @Test
    void shipItemsHaveNoTierAndScaleByHullSizeOnly() {
        ItemDefinitions.register(List.of(UNGATED));
        Item item = new ItemStore().add(UNGATED, 3L);

        assertEquals(TierRange.NO_TIER, item.tier());
        assertSame(ScaleContext.ofHullSize(HullSize.CRUISER), item.scaleContext(HullSize.CRUISER));
        assertSame(ScaleContext.NONE, item.scaleContext(null));
    }

    @Test
    void theDefaultLimitsRollTheSameEffectCountsAsBefore() {
        assertSame(AffixLayout.DEFAULT_LIMITS, AffixLayout.limits(ItemType.SUBROUTINE));
        assertSame(AffixLayout.DEFAULT_LIMITS, AffixLayout.limits(ItemType.HULL_ITEM));
        assertSame(AffixLayout.DEFAULT_LIMITS, AffixLayout.limits(null));
        for (int step = 0; step <= 10000; step++) {
            float roll = step / 10000f;
            assertEquals(legacyEffectCount(roll), AffixLayout.rolledEffectCount(AffixLayout.DEFAULT_LIMITS, roll), "roll " + roll);
        }
    }

    private static int legacyEffectCount(float roll) {
        if (roll < AffixLayout.ONE_EFFECT_CHANCE) {
            return 1;
        }
        float upToTwo = AffixLayout.ONE_EFFECT_CHANCE + AffixLayout.TWO_EFFECTS_CHANCE;
        if (roll < upToTwo) {
            return 2;
        }
        return roll < upToTwo + AffixLayout.THREE_EFFECTS_CHANCE ? 3 : 4;
    }

    @Test
    void aTypeCanCarryMoreModifiersWithItsOwnLimits() {
        AffixLayout.Limits wide = new AffixLayout.Limits(3, 6, 3, List.of(0f, 0f, 0f, 0f, 0f));
        assertNull(AffixLayout.register(ItemType.SUBROUTINE, wide));

        assertEquals(ItemRarity.COMMON, AffixLayout.rarityFor(UNGATED, 3));
        assertEquals(ItemRarity.RARE, AffixLayout.rarityFor(UNGATED, 4));
        Set<Integer> counts = new HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            List<RolledModifier> rolled = ItemRoller.roll(UNGATED, seed, false);
            counts.add(rolled.size());
            assertEquals(3, rolled.stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count());
        }
        assertEquals(Set.of(6), counts);
        assertEquals(2, AffixLayout.openSides(UNGATED, ItemRoller.roll(UNGATED, 1L, false).subList(0, 2)).size());
        assertSame(wide, AffixLayout.register(ItemType.SUBROUTINE, null));
        assertSame(AffixLayout.DEFAULT_LIMITS, AffixLayout.limits(ItemType.SUBROUTINE));
    }

    @Test
    void limitsNeedAChanceForEveryCountBelowTheMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new AffixLayout.Limits(2, 4, 2, List.of(0.5f)));
        assertThrows(IllegalArgumentException.class, () -> new AffixLayout.Limits(5, 4, 2, List.of(0.1f, 0.1f, 0.1f)));
        assertThrows(IllegalArgumentException.class, () -> new AffixLayout.Limits(2, 4, 0, List.of(0.1f, 0.1f, 0.1f)));
    }
}
