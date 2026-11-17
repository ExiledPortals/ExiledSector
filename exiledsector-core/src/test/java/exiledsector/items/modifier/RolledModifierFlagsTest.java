package exiledsector.items.modifier;

import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RolledModifierFlagsTest {

    private static final RolledLine LINE = new RolledLine("HULL_MULT", 5f);

    private static RolledModifier flagged(ModifierFlags flags) {
        return new RolledModifier("HULL_MULT", ModifierSlot.SUFFIX, List.of(LINE), flags);
    }

    @Test
    void eachFlagSetterChangesOnlyItsOwnFlag() {
        RolledModifier plain = flagged(ModifierFlags.NONE);

        assertEquals(new ModifierFlags(true, false, false), plain.asImmutable().flags());
        assertEquals(new ModifierFlags(false, true, false), plain.overchargedWith(List.of(LINE)).flags());
        assertEquals(new ModifierFlags(false, false, true), plain.asWarped().flags());
        assertEquals(new ModifierFlags(true, true, true), plain.asImmutable().asWarped().overchargedWith(List.of(LINE)).flags());
    }

    @Test
    void copiesKeepEveryFlagAndRerollingDropsOnlyTheOvercharge() {
        ModifierFlags allFlags = new ModifierFlags(true, true, true);
        RolledModifier modifier = flagged(allFlags);

        assertEquals(allFlags, modifier.withModifierId("RENAMED").flags());
        assertEquals(allFlags, modifier.withLines(List.of(new RolledLine("HULL_MULT", 6f))).flags());
        assertEquals(allFlags, modifier.mapLines(line -> line).flags());
        assertEquals(new ModifierFlags(true, false, true), modifier.rerolled(effectName -> (ModifierLine) null, new Random(1L)).flags());
    }

    @Test
    void modifiersDifferingOnlyInFlagsAreNotEqual() {
        assertEquals(flagged(ModifierFlags.NONE.asWarped()), flagged(new ModifierFlags(false, false, true)));
        assertNotEquals(flagged(ModifierFlags.NONE), flagged(ModifierFlags.NONE.asImmutable()));
        assertNotEquals(flagged(ModifierFlags.NONE.asImmutable()), flagged(ModifierFlags.NONE.asOvercharged()));
    }
}
