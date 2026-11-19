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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckyRollsChosenModifiersTest {

    private static final String PLATE = "chosen_lucky_plate";
    private static final String HYBRID = "chosen_lucky_hybrid";
    private static final String HULL_EFFECT = "HULL_PERCENT";
    private static final int DROPS = 2000;

    @BeforeEach
    void setUp() {
        PoolEntry hull = PoolEntry.of(ModifierSlot.PREFIX, HULL_EFFECT, 1f, 100f, 1f);
        PoolEntry shield = PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -100f, -1f, 1f);
        ItemDefinitions.register(List.of(
                definition(PLATE, List.of(hull), List.of()),
                definition(HYBRID, List.of(hull), List.of(shield))));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    private static ItemDefinition definition(String id, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, ItemKind.ARMOR_PLATING, id, ItemDefinition.FALLBACK_ICON, 1f, false, "", prefixes, suffixes, "");
    }

    private static ItemData dropped(String definitionId, long seed, int modifierCount) {
        return ItemData.rolled(ItemDefinitions.get(definitionId), seed, ItemRoll.withModifierCount(modifierCount));
    }

    private static boolean isHullModifier(RolledModifier rolled) {
        return rolled.lines().stream().anyMatch(line -> line.effectName().equals(HULL_EFFECT));
    }

    private static String hullModifierId(ItemData item) {
        return item.explicits().stream().filter(LuckyRollsChosenModifiersTest::isHullModifier).findFirst().orElseThrow().modifierId();
    }

    @Test
    void onlyTheChosenModifiersRollAgainKeepingTheBetter() {
        int changed = 0;
        for (long seed = 0L; seed < 200L; seed++) {
            ItemData item = dropped(HYBRID, seed, 2);
            String hullId = hullModifierId(item);
            ItemData lucky = LuckyRolls.withLuckyModifiers(item, hullId::equals);
            for (int modifier = 0; modifier < 2; modifier++) {
                RolledModifier before = item.explicits().get(modifier);
                RolledModifier after = lucky.explicits().get(modifier);
                if (isHullModifier(before)) {
                    assertTrue(after.lines().get(0).magnitude() >= before.lines().get(0).magnitude());
                    changed += before.equals(after) ? 0 : 1;
                } else {
                    assertEquals(before, after);
                }
            }
            assertEquals(item.frozenName(), lucky.frozenName());
        }
        assertTrue(changed > 25, "changed " + changed);
    }

    @Test
    void everyChosenModifierIsLucky() {
        float total = 0f;
        for (long seed = 0L; seed < DROPS; seed++) {
            ItemData lucky = LuckyRolls.withLuckyModifiers(dropped(PLATE, seed, 1), modifierId -> true);
            total += lucky.explicits().get(0).lines().get(0).magnitude();
        }
        assertEquals(67f, total / DROPS, 3f);
    }

    @Test
    void theRollIsTheSameForTheSameDropAndLeavesOtherItemsAlone() {
        ItemData item = dropped(HYBRID, 7L, 2);
        ItemData lucky = LuckyRolls.withLuckyModifiers(item, modifierId -> true);

        assertEquals(lucky, LuckyRolls.withLuckyModifiers(dropped(HYBRID, 7L, 2), modifierId -> true));
        assertSame(item, LuckyRolls.withLuckyModifiers(item, modifierId -> false));
        assertNull(LuckyRolls.withLuckyModifiers(null, modifierId -> true));
    }
}
