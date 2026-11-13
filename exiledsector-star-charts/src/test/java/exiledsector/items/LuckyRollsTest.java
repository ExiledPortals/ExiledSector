package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckyRollsTest {

    private static final String PLATE = "lucky_plate";
    private static final String EMITTER = "lucky_emitter";
    private static final String HYBRID = "lucky_hybrid";
    private static final String FIXED = "lucky_fixed";
    private static final String RELIC = "lucky_relic";
    private static final int DROPS = 2000;

    @BeforeEach
    void setUp() {
        PoolEntry hull = PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 1f, 100f, 1f);
        PoolEntry shield = PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -100f, -1f, 1f);
        ItemDefinitions.register(List.of(
                definition(PLATE, false, List.of(hull), List.of()),
                definition(EMITTER, false, List.of(), List.of(shield)),
                definition(HYBRID, false, List.of(hull), List.of(shield)),
                definition(FIXED, false, List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 5f, 5f, 1f)), List.of()),
                definition(RELIC, true, List.of(hull), List.of())));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    private static ItemDefinition definition(String id, boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, ItemKind.ARMOR_PLATING, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "", prefixes, suffixes, "");
    }

    private static ItemData dropped(String definitionId, long seed, int modifierCount) {
        return ItemData.rolledWithModifierCount(ItemDefinitions.get(definitionId), seed, modifierCount);
    }

    private static float magnitude(ItemData item, int modifier) {
        return item.explicits().get(modifier).lines().get(0).magnitude();
    }

    private static float averageRoll(String definitionId, boolean lucky) {
        float total = 0f;
        for (long seed = 0L; seed < DROPS; seed++) {
            ItemData item = dropped(definitionId, seed, 1);
            total += magnitude(lucky ? LuckyRolls.withLuckyModifier(item) : item, 0);
        }
        return total / DROPS;
    }

    @Test
    void aLuckyModifierKeepsTheBetterOfTwoRolls() {
        for (long seed = 0L; seed < 200L; seed++) {
            ItemData item = dropped(PLATE, seed, 1);
            assertTrue(magnitude(LuckyRolls.withLuckyModifier(item), 0) >= magnitude(item, 0));
        }
        assertEquals(50.5f, averageRoll(PLATE, false), 3f);
        assertEquals(67f, averageRoll(PLATE, true), 3f);
    }

    @Test
    void aModifierWhereLessIsBetterKeepsTheLowerRoll() {
        for (long seed = 0L; seed < 200L; seed++) {
            ItemData item = dropped(EMITTER, seed, 1);
            assertTrue(magnitude(LuckyRolls.withLuckyModifier(item), 0) <= magnitude(item, 0));
        }
        assertEquals(-67f, averageRoll(EMITTER, true), 3f);
    }

    @Test
    void onlyOneModifierRollsAgainAndEitherCanBeTheOne() {
        int[] changedPerModifier = new int[2];
        for (long seed = 0L; seed < 200L; seed++) {
            ItemData item = dropped(HYBRID, seed, 2);
            ItemData lucky = LuckyRolls.withLuckyModifier(item);
            int differing = 0;
            for (int modifier = 0; modifier < 2; modifier++) {
                boolean changed = !item.explicits().get(modifier).equals(lucky.explicits().get(modifier));
                differing += changed ? 1 : 0;
                changedPerModifier[modifier] += changed ? 1 : 0;
            }
            assertTrue(differing <= 1);
            assertEquals(item.frozenName(), lucky.frozenName());
        }
        assertTrue(changedPerModifier[0] > 25 && changedPerModifier[1] > 25,
                "changed " + changedPerModifier[0] + " and " + changedPerModifier[1]);
    }

    @Test
    void theRollIsTheSameForTheSameDropAndNeverStacks() {
        for (long seed = 0L; seed < 50L; seed++) {
            ItemData lucky = LuckyRolls.withLuckyModifier(dropped(HYBRID, seed, 2));

            assertEquals(lucky, LuckyRolls.withLuckyModifier(dropped(HYBRID, seed, 2)));
            assertEquals(lucky, LuckyRolls.withLuckyModifier(lucky));
        }
    }

    @Test
    void uniquesGetALuckyRollToo() {
        int changed = 0;
        for (long seed = 0L; seed < 50L; seed++) {
            ItemData item = dropped(RELIC, seed, 1);
            changed += item.equals(LuckyRolls.withLuckyModifier(item)) ? 0 : 1;
        }
        assertTrue(changed > 0);
    }

    @Test
    void fixedAndImmutableModifiersAreLeftAlone() {
        ItemData fixed = dropped(FIXED, 3L, 1);
        ItemData plate = dropped(PLATE, 3L, 1);
        ItemData immutable = new ItemData(plate.definitionId(), plate.seed(), List.of(plate.explicits().get(0).asImmutable()),
                plate.frozenName(), plate.traits());

        assertSame(fixed, LuckyRolls.withLuckyModifier(fixed));
        assertSame(immutable, LuckyRolls.withLuckyModifier(immutable));
        assertNull(LuckyRolls.withLuckyModifier(null));
    }
}
