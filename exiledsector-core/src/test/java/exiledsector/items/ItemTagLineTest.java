package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTagLineTest {

    private static final Modifier PLATED_FRAME = new Modifier("plated_frame", ModifierSlot.PREFIX,
            List.of(ModifierLine.range("ARMOR_PERCENT", 5f, 10f), ModifierLine.range("HULL_PERCENT", 3f, 6f)),
            new LinkedHashSet<>(List.of("armour", "hull")), false);
    private static final Modifier SHIELDING = new Modifier("SHIELD_DAMAGE_TAKEN_MULT", ModifierSlot.SUFFIX,
            List.of(ModifierLine.range("SHIELD_DAMAGE_TAKEN_MULT", -6f, -4f)), Set.of("front_shield"), false);
    private static final Modifier FORTITUDE = new Modifier("HULL_MULT", ModifierSlot.SUFFIX,
            List.of(ModifierLine.range("HULL_MULT", 4f, 6f)), Set.of(), true);

    private static final ItemDefinition FRAME = new ItemDefinition("frame", ItemKind.BRIDGE, "Frame", ItemDefinition.FALLBACK_ICON,
            1f, false, "", List.of(new PoolEntry(PLATED_FRAME, 1f)), List.of(new PoolEntry(SHIELDING, 1f), new PoolEntry(FORTITUDE, 1f)), "");

    @BeforeEach
    void register() {
        ItemDefinitions.register(List.of(FRAME));
    }

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
    }

    private static Item item() {
        RolledModifier platedFrame = new RolledModifier("plated_frame", ModifierSlot.PREFIX,
                List.of(new RolledLine("ARMOR_PERCENT", 7f), new RolledLine("HULL_PERCENT", 4f)));
        return Item.create("frame_1", "frame", 1L, List.of(platedFrame,
                RolledModifier.single(ModifierSlot.SUFFIX, new RolledLine("SHIELD_DAMAGE_TAKEN_MULT", -5f)),
                RolledModifier.single(ModifierSlot.SUFFIX, new RolledLine("HULL_MULT", 5f))));
    }

    @Test
    void expandedLinesListEachTaggedModifiersOwnTagsUnderIt() {
        List<String> expanded = item().slottedEffectLines(true, null).stream().map(line -> line.text().toMarkup()).toList();

        assertEquals(7, expanded.size(), expanded.toString());
        assertEquals("<muted>Armor, Hull</muted>", expanded.get(3));
        assertEquals("<muted>Front Shield</muted>", expanded.get(5));
        assertTrue(expanded.get(6).contains("(4%-6%)"), expanded.get(6));
    }

    @Test
    void collapsedLinesShowNoTags() {
        List<StyledText> collapsed = item().effectLines(false);

        assertEquals(4, collapsed.size());
        assertTrue(collapsed.stream().noneMatch(line -> line.plain().startsWith("Tags")));
    }
}
