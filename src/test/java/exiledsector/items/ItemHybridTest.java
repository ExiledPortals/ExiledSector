package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.SkillTypeEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemHybridTest {

    private static final Modifier PLATED_FRAME = new Modifier("plated_frame", ModifierSlot.PREFIX,
            List.of(ModifierLine.range("ARMOR_PERCENT", 5f, 10f), ModifierLine.range("HULL_PERCENT", 3f, 6f)));
    private static final Modifier FORTITUDE = Modifier.single(ModifierSlot.SUFFIX, ModifierLine.range("HULL_MULT", 4f, 6f));
    private static final Modifier CAPACITY = Modifier.single(ModifierSlot.PREFIX, ModifierLine.range("FLUX_CAPACITY_MULT", 4f, 6f));
    private static final Modifier SHIELDING = Modifier.single(ModifierSlot.SUFFIX, ModifierLine.range("SHIELD_DAMAGE_TAKEN_MULT", -6f, -4f));

    private static final ItemDefinition FRAME = new ItemDefinition("frame", ItemKind.SUBROUTINE, "Frame", ItemDefinition.FALLBACK_ICON,
            1f, false, "", List.of(new PoolEntry(PLATED_FRAME, 1f), new PoolEntry(CAPACITY, 1f)),
            List.of(new PoolEntry(FORTITUDE, 1f), new PoolEntry(SHIELDING, 1f)), "");

    @BeforeEach
    void register() {
        ItemDefinitions.register(List.of(FRAME));
    }

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
    }

    private static Item item(List<RolledModifier> explicits) {
        return Item.create("frame_1", "frame", 1L, explicits);
    }

    private static RolledModifier platedFrame(float armor, float hull) {
        return new RolledModifier("plated_frame", ModifierSlot.PREFIX,
                List.of(new RolledEffect("ARMOR_PERCENT", armor), new RolledEffect("HULL_PERCENT", hull)));
    }

    @Test
    void aHybridRollsEveryLineWithinItsOwnRange() {
        for (long seed = 0; seed < 50; seed++) {
            RolledModifier rolled = RolledModifier.roll(PLATED_FRAME, new Random(seed), false);

            assertEquals("plated_frame", rolled.modifierId());
            assertTrue(rolled.isHybrid());
            assertTrue(rolled.lines().get(0).magnitude() >= 5f && rolled.lines().get(0).magnitude() <= 10f);
            assertTrue(rolled.lines().get(1).magnitude() >= 3f && rolled.lines().get(1).magnitude() <= 6f);
        }
    }

    @Test
    void aHybridCountsAsOneModifierForRarityButAppliesEveryLine() {
        Item item = item(List.of(platedFrame(7f, 4f), RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect("HULL_MULT", 5f))));
        List<SkillTypeEffect> applied = item.skillEffects();

        assertEquals(ItemRarity.COMMON, item.rarity());
        assertEquals(List.of("ARMOR_PERCENT", "HULL_PERCENT", "HULL_MULT"), applied.stream().map(effect -> effect.effect().name()).toList());
        assertEquals(3, item.effectLines().size());
        assertEquals(List.of(ModifierSlot.PREFIX, ModifierSlot.PREFIX, ModifierSlot.SUFFIX),
                item.rolls(ModifierCategory.EXPLICIT).stream().map(SlottedRoll::slot).toList());
    }

    @Test
    void expandedLinesGroupAHybridUnderOneTaggedHeader() {
        Item item = item(List.of(platedFrame(7f, 4f), RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect("HULL_MULT", 5f))));

        List<SlottedLine> expanded = item.slottedEffectLines(true, null);
        List<StyledText> collapsed = item.effectLines(false);

        assertEquals(Arrays.asList(ModifierSlot.PREFIX, null, null, ModifierSlot.SUFFIX), expanded.stream().map(SlottedLine::slot).toList());
        assertEquals("<muted>Hybrid</muted>", expanded.get(0).text().toMarkup());
        assertTrue(expanded.get(1).text().toMarkup().startsWith("<muted>|</muted> "));
        assertTrue(expanded.get(1).text().plain().contains("(5%-10%)"));
        assertTrue(expanded.get(2).text().toMarkup().startsWith("<muted>|</muted> "));
        assertTrue(expanded.get(2).text().plain().contains("(3%-6%)"));
        assertTrue(expanded.get(3).text().plain().contains("(4%-6%)"));
        assertTrue(collapsed.stream().noneMatch(line -> line.plain().startsWith("|") || line.plain().equals("Hybrid")));
        assertEquals(3, collapsed.size());
    }

    @Test
    void recalibratingAHybridRerollsEachOfItsLines() {
        Item item = item(List.of(platedFrame(5f, 3f)));

        for (long seed = 0; seed < 20; seed++) {
            List<RolledModifier> before = item.explicits();
            assertSame(item, ItemCrafting.recalibrate(item, new Random(seed)));
            assertNotEquals(before, item.explicits());
            assertEquals("plated_frame", item.explicits().get(0).modifierId());
            assertEquals(2, item.explicits().get(0).lines().size());
        }
    }

    @Test
    void augmentingAddsAWholeModifierAndAPrefixLandsAfterTheOtherPrefixes() {
        Item item = item(List.of(RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect("HULL_MULT", 5f))));

        for (long seed = 0; seed < 50 && item.explicits().size() < 3; seed++) {
            ItemCrafting.augment(item, new Random(seed));
        }

        List<RolledModifier> explicits = item.explicits();
        assertTrue(explicits.size() >= 2);
        long prefixes = explicits.stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count();
        assertTrue(explicits.subList(0, (int) prefixes).stream().allMatch(modifier -> modifier.slot() == ModifierSlot.PREFIX));
        assertEquals(explicits.size(), explicits.stream().map(RolledModifier::modifierId).distinct().count(), "no modifier is added twice");
    }

    @Test
    void theRollerDrawsHybridsFromTheirSideOfThePool() {
        boolean sawHybrid = false;
        for (long seed = 0; seed < 200; seed++) {
            for (RolledModifier rolled : ItemRoller.roll(FRAME, seed, false)) {
                sawHybrid |= rolled.isHybrid();
                assertEquals(rolled.modifierId().equals("plated_frame") || rolled.modifierId().equals("FLUX_CAPACITY_MULT"),
                        rolled.slot() == ModifierSlot.PREFIX);
            }
        }
        assertTrue(sawHybrid);
    }
}
