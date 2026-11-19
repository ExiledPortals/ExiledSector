package exiledsector.items.modifier;

import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckyRollsCountTest {

    private static final String HYBRID = "lucky_count_hybrid";
    private static final long SEEDS = 200L;

    @BeforeEach
    void setUp() {
        PoolEntry hull = PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 1f, 100f, 1f);
        PoolEntry shield = PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -100f, -1f, 1f);
        ItemDefinitions.register(List.of(new ItemDefinition(HYBRID, ItemKind.ARMOR_PLATING, HYBRID, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(hull), List.of(shield), "")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    private static ItemData dropped(long seed) {
        return ItemData.rolled(ItemDefinitions.get(HYBRID), seed, ItemRoll.withModifierCount(2));
    }

    private static ItemData withLuckyModifiers(ItemData item, int luckyModifiers) {
        return LuckyRolls.withLuck(item, new Luck(luckyModifiers, false, false, Luck.BASE_ROLLS));
    }

    private static int changedModifiers(ItemData item, ItemData lucky) {
        int changed = 0;
        for (int modifier = 0; modifier < item.explicits().size(); modifier++) {
            changed += item.explicits().get(modifier).equals(lucky.explicits().get(modifier)) ? 0 : 1;
        }
        return changed;
    }

    @Test
    void oneLuckyModifierIsTheSingleLuckyRoll() {
        for (long seed = 0L; seed < SEEDS; seed++) {
            ItemData item = dropped(seed);

            assertEquals(LuckyRolls.withLuckyModifier(item), withLuckyModifiers(item, 1));
        }
    }

    @Test
    void twoLuckyModifiersRollTwoDifferentModifiersAgain() {
        int bothChanged = 0;
        for (long seed = 0L; seed < SEEDS; seed++) {
            ItemData item = dropped(seed);
            ItemData lucky = withLuckyModifiers(item, 2);

            for (int modifier = 0; modifier < 2; modifier++) {
                float before = item.explicits().get(modifier).lines().get(0).magnitude();
                float after = lucky.explicits().get(modifier).lines().get(0).magnitude();
                assertTrue(modifier == 0 ? after >= before : after <= before, "seed " + seed);
            }
            bothChanged += changedModifiers(item, lucky) == 2 ? 1 : 0;
        }
        assertTrue(bothChanged > SEEDS / 10, "both changed " + bothChanged);
    }

    @Test
    void moreLuckyModifiersThanTheItemHasRollsEachOfItsModifiersOnce() {
        for (long seed = 0L; seed < SEEDS; seed++) {
            ItemData item = dropped(seed);

            assertEquals(withLuckyModifiers(item, 2), withLuckyModifiers(item, 5));
        }
    }

    @Test
    void noLuckyModifiersLeaveTheItemAsItIs() {
        ItemData item = dropped(7L);

        assertSame(item, withLuckyModifiers(item, 0));
        assertSame(item, withLuckyModifiers(item, -1));
    }
}
