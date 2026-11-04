package exiledsector.items;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStatTotalsTest {

    private static final Set<ModifierCategory> BOTH = EnumSet.allOf(ModifierCategory.class);
    private static final Set<ModifierCategory> IMPLICIT = EnumSet.of(ModifierCategory.IMPLICIT);
    private static final Set<ModifierCategory> EXPLICIT = EnumSet.of(ModifierCategory.EXPLICIT);

    private static final Modifier PLATED_FRAME = new Modifier("plated_frame", ModifierSlot.PREFIX,
            List.of(ModifierLine.range("ARMOR_PERCENT", 3f, 6f), ModifierLine.range("HULL_PERCENT", 3f, 6f)));
    private static final Modifier ARMORED = new Modifier("armored", ModifierSlot.SUFFIX, List.of(ModifierLine.range("ARMOR_PERCENT", 10f, 20f)));
    private static final Modifier LAMINATED = new Modifier("laminated", ModifierSlot.PREFIX,
            List.of(ModifierLine.perHullSize("ARMOR_FLAT", List.of(20f, 30f, 40f, 50f))));
    private static final Modifier BOLTED = new Modifier("bolted", ModifierSlot.SUFFIX, List.of(ModifierLine.range("ARMOR_FLAT", 10f, 10f)));

    private static final ItemDefinition FRAME = new ItemDefinition("frame", ItemKind.BRIDGE, "Frame", ItemDefinition.FALLBACK_ICON,
            1f, false, "", List.of(new PoolEntry(PLATED_FRAME, 1f), new PoolEntry(LAMINATED, 1f)),
            List.of(new PoolEntry(ARMORED, 1f), new PoolEntry(BOLTED, 1f)), "");

    private record RoundedEffect(String name) implements ModifierEffect {

        @Override
        public StyledText description(float magnitude) {
            String shownNumber = String.valueOf(Math.round(Math.abs(magnitude)));
            String plain = shownNumber + "% " + (magnitude < 0f ? "reduced" : "increased") + " test stat.";
            return new StyledText(plain, List.of(new StyledText.Span(0, shownNumber.length() + 1, Style.GOOD)));
        }
    }

    private static final ModifierEffects ROUNDED_EFFECTS = RoundedEffect::new;

    @BeforeEach
    void register() {
        ItemDefinitions.register(List.of(FRAME));
    }

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
    }

    private static Item frame(List<RolledModifier> explicits, List<RolledModifier> implicits) {
        Item item = Item.create("frame_1", "frame", 1L, explicits);
        item.replaceImplicits(implicits);
        return item;
    }

    private static RolledModifier platedFrame(float armor, float hull) {
        return new RolledModifier("plated_frame", ModifierSlot.PREFIX,
                List.of(new RolledLine("ARMOR_PERCENT", armor), new RolledLine("HULL_PERCENT", hull)));
    }

    private static RolledModifier rolled(String modifierId, ModifierSlot slot, String effectName, float magnitude) {
        return new RolledModifier(modifierId, slot, List.of(new RolledLine(effectName, magnitude)));
    }

    @Test
    void aHybridLineAddsToEachOfItsStats() {
        ItemStatTotals totals = ItemStatTotals.of(frame(List.of(platedFrame(5f, 4f), rolled("armored", ModifierSlot.SUFFIX, "ARMOR_PERCENT", 15f)), List.of()));

        assertEquals(List.of(20d), totals.totals("ARMOR_PERCENT", BOTH));
        assertEquals(List.of(4d), totals.totals("HULL_PERCENT", BOTH));
    }

    @Test
    void implicitAndExplicitLinesAreKeptApart() {
        ItemStatTotals totals = ItemStatTotals.of(frame(List.of(platedFrame(5f, 4f), rolled("armored", ModifierSlot.SUFFIX, "ARMOR_PERCENT", 15f)),
                List.of(rolled("ARMOR_PERCENT", ModifierSlot.IMPLICIT, "ARMOR_PERCENT", 6f))));

        assertEquals(List.of(6d), totals.totals("ARMOR_PERCENT", IMPLICIT));
        assertEquals(List.of(20d), totals.totals("ARMOR_PERCENT", EXPLICIT));
        assertEquals(List.of(26d), totals.totals("ARMOR_PERCENT", BOTH));
        assertEquals(List.of(), totals.totals("HULL_PERCENT", IMPLICIT), "an item with no implicit of the stat has no implicit total");
        assertEquals(List.of(), totals.totals("TOP_SPEED_PERCENT", BOTH));
    }

    @Test
    void aHullSizeLineGivesOneTotalPerHullSizeOnTopOfTheFixedLines() {
        ItemStatTotals totals = ItemStatTotals.of(frame(List.of(rolled("laminated", ModifierSlot.PREFIX, "ARMOR_FLAT", 20f),
                rolled("bolted", ModifierSlot.SUFFIX, "ARMOR_FLAT", 10f)), List.of()));

        assertEquals(List.of(30d, 40d, 50d, 60d), totals.totals("ARMOR_FLAT", BOTH));
        assertEquals(List.of(), totals.totals("ARMOR_FLAT", IMPLICIT));
    }

    @Test
    void totalsUseTheShownValueRoundedToItsDecimalsWithTheValuesSign() {
        List<SlottedRoll> rolls = List.of(
                new SlottedRoll(ModifierSlot.PREFIX, new RolledLine("TEST_STAT", 14.6f), ModifierLine.range("TEST_STAT", 10f, 20f)),
                new SlottedRoll(ModifierSlot.SUFFIX, new RolledLine("TEST_STAT", 4.6f), ModifierLine.range("TEST_STAT", 1f, 5f)),
                new SlottedRoll(ModifierSlot.PREFIX, new RolledLine("TEST_REDUCTION", -7.2f), ModifierLine.range("TEST_REDUCTION", -10f, -5f)));
        ItemStatTotals totals = ItemStatTotals.of(rolls, ROUNDED_EFFECTS, hullSize -> ScaleContext.ofHullSize(hullSize));

        assertEquals(List.of(20d), totals.totals("TEST_STAT", BOTH));
        assertEquals(List.of(-7d), totals.totals("TEST_REDUCTION", BOTH));
    }

    @Test
    void anItemWithNoLinesSharesTheEmptyTotals() {
        ItemStatTotals totals = ItemStatTotals.of(List.of(), ROUNDED_EFFECTS, ScaleContext::ofHullSize);

        assertSame(ItemStatTotals.NONE, totals);
        assertTrue(totals.stats().isEmpty());
    }
}
