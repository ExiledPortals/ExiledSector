package exiledsector.ui.socket;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemStatTotals;
import exiledsector.items.RolledLine;
import exiledsector.items.SlottedRoll;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierSearchTest {

    private static final String ARMOR = "ARMOR_PERCENT";
    private static final String HULL = "HULL_PERCENT";
    private static final String SPEED = "TOP_SPEED_PERCENT";
    private static final String ARMOR_FLAT = "ARMOR_FLAT";

    private record NumberEffect(String name) implements ModifierEffect {

        @Override
        public StyledText description(float magnitude) {
            String shownNumber = String.valueOf(Math.round(Math.abs(magnitude)));
            return new StyledText(shownNumber + "% test", List.of(new StyledText.Span(0, shownNumber.length() + 1, Style.GOOD)));
        }
    }

    private static final ModifierEffects EFFECTS = NumberEffect::new;

    private static SlottedRoll implicit(String statName, float value) {
        return new SlottedRoll(ModifierSlot.IMPLICIT, new RolledLine(statName, value), null);
    }

    private static SlottedRoll explicit(String statName, float value) {
        return new SlottedRoll(ModifierSlot.PREFIX, new RolledLine(statName, value), null);
    }

    private static ItemStatTotals totals(SlottedRoll... rolls) {
        return ItemStatTotals.of(List.of(rolls), EFFECTS, ScaleContext::ofHullSize);
    }

    private static final ItemStatTotals ARMORED = totals(implicit(ARMOR, 6f), explicit(ARMOR, 14f), explicit(HULL, 8f));
    private static final ItemStatTotals FAST = totals(explicit(SPEED, 10f), explicit(HULL, 3f));

    private static ModifierSearch.Row row(ModifierSearch.Row row, String statName, String minText, String maxText) {
        row.statName = statName;
        row.minText = minText;
        row.maxText = maxText;
        return row;
    }

    private static ModifierSearch andGroup(String... statMinMax) {
        ModifierSearch search = new ModifierSearch();
        fill(search, search.addGroup(ModifierSearch.GroupKind.AND), statMinMax);
        return search;
    }

    private static void fill(ModifierSearch search, ModifierSearch.Group group, String... statMinMax) {
        for (int index = 0; index < statMinMax.length; index += 3) {
            ModifierSearch.Row target = index == 0 ? group.rows.get(0) : search.addRow(group);
            row(target, statMinMax[index], statMinMax[index + 1], statMinMax[index + 2]);
        }
    }

    private static boolean matches(ModifierSearch search, ItemStatTotals totals) {
        search.apply();
        return search.matches(totals);
    }

    @Test
    void anAndGroupNeedsEveryRowToMatch() {
        assertTrue(matches(andGroup(ARMOR, "20", "", HULL, "5", ""), ARMORED));
        assertFalse(matches(andGroup(ARMOR, "20", "", HULL, "9", ""), ARMORED));
        assertFalse(matches(andGroup(ARMOR, "20", "", HULL, "5", ""), FAST));
    }

    @Test
    void aCountGroupNeedsAtLeastItsCountOfRowsToMatch() {
        ModifierSearch search = new ModifierSearch();
        ModifierSearch.Group group = search.addGroup(ModifierSearch.GroupKind.COUNT);
        fill(search, group, ARMOR, "10", "", HULL, "5", "", SPEED, "5", "");
        group.countText = "2";

        assertTrue(matches(search, ARMORED));
        assertFalse(matches(search, FAST), "only speed reaches its minimum");

        group.countText = "4";
        assertFalse(matches(search, ARMORED), "a count above the row count never passes");
    }

    @Test
    void aBlankMinOrMaxMeansNoLimitOnThatSide() {
        assertTrue(matches(andGroup(ARMOR, "", "", HULL, "", ""), ARMORED));
        assertTrue(matches(andGroup(ARMOR, "", "20"), ARMORED));
        assertFalse(matches(andGroup(ARMOR, "", "19"), ARMORED));
        assertTrue(matches(andGroup(ARMOR, "20", "20"), ARMORED), "the bounds include the shown value");
    }

    @Test
    void aRowSumsTheImplicitAndExplicitLinesTogether() {
        assertTrue(matches(andGroup(ARMOR, "20", "20"), ARMORED));
        assertFalse(matches(andGroup(ARMOR, "6", "6"), ARMORED), "the implicit alone never counts");
        assertFalse(matches(andGroup(ARMOR, "14", "14"), ARMORED), "the explicit alone never counts");
    }

    @Test
    void aRowOrGroupWithNoStatPickedIsIgnored() {
        ModifierSearch search = andGroup(ARMOR, "20", "");
        search.addRow(search.groups().get(0));
        search.addGroup(ModifierSearch.GroupKind.COUNT).countText = "3";

        assertTrue(matches(search, ARMORED));
        assertEquals(1, search.criteria().size());
        assertEquals(1, search.criteria().get(0).rows().size());
    }

    @Test
    void anItemWithoutTheStatFailsTheRowEvenWithNoBounds() {
        assertFalse(matches(andGroup(SPEED, "", ""), ARMORED));
        assertTrue(matches(andGroup(HULL, "", ""), ARMORED), "an explicit-only stat still counts");
    }

    @Test
    void aHullSizeStatMatchesWhenAnyHullSizeTotalIsInRange() {
        SlottedRoll scaled = new SlottedRoll(ModifierSlot.PREFIX, new RolledLine(ARMOR_FLAT, 20f),
                ModifierLine.perHullSize(ARMOR_FLAT, List.of(20f, 30f, 40f, 50f)));
        ItemStatTotals plated = totals(scaled);

        assertTrue(matches(andGroup(ARMOR_FLAT, "45", ""), plated));
        assertTrue(matches(andGroup(ARMOR_FLAT, "25", "35"), plated));
        assertFalse(matches(andGroup(ARMOR_FLAT, "55", ""), plated));
    }

    @Test
    void theStoragePanelKeepsTheLastAppliedSearchUntilSearchIsPressedAgain() {
        ModifierSearch search = andGroup(ARMOR, "20", "");
        search.apply();
        search.groups().get(0).rows.get(0).minText = "50";

        assertTrue(search.matches(ARMORED));
        search.apply();
        assertFalse(search.matches(ARMORED));
    }

    @Test
    void removingAGroupsLastRowRemovesTheGroup() {
        ModifierSearch search = andGroup(ARMOR, "20", "", HULL, "", "");
        ModifierSearch.Group group = search.groups().get(0);

        search.removeRow(group, group.rows.get(0));
        assertEquals(1, search.groups().size());
        search.removeRow(group, group.rows.get(0));
        assertTrue(search.groups().isEmpty());
    }

    @Test
    void boundsAcceptSignsPercentsAndDecimalCommas() {
        assertEquals(-7d, ModifierSearch.parseBound(" -7% "));
        assertEquals(2.5d, ModifierSearch.parseBound("2,5"));
        assertEquals(20d, ModifierSearch.parseBound("+20"));
        assertNull(ModifierSearch.parseBound("abc"));
        assertNull(ModifierSearch.parseBound(""));
        assertEquals(1, ModifierSearch.parseCount(""));
    }

    @Test
    void theFilterCountsTheSearchAsAFilterAndResetClearsIt() {
        ItemStorageFilter filter = new ItemStorageFilter();
        filter.modifierSearch.addGroup(ModifierSearch.GroupKind.AND);
        assertTrue(filter.isDefault(), "an empty row isn't a filter");

        filter.modifierSearch.groups().get(0).rows.get(0).statName = ARMOR;
        assertFalse(filter.isDefault());
        assertTrue(filter.isDefault(false), "panels without the advanced search ignore it");

        filter.reset();
        assertTrue(filter.isDefault());
        assertTrue(filter.modifierSearch.groups().isEmpty());
    }

    @Test
    void theStorageQueryAppliesTheSearchOnlyWhereThePanelOffersIt() {
        List<ItemStorageRow> rows = new ArrayList<>();
        rows.add(new ItemStorageRow(null, 0, "Armored", ItemRarity.COMMON, List.of("armored"), null, ARMORED));
        rows.add(new ItemStorageRow(null, 1, "Fast", ItemRarity.COMMON, List.of("fast"), null, FAST));
        ItemStorageFilter filter = new ItemStorageFilter();
        fill(filter.modifierSearch, filter.modifierSearch.addGroup(ModifierSearch.GroupKind.AND), SPEED, "", "");
        filter.modifierSearch.apply();

        assertEquals(List.of("Fast"), ItemStorageQuery.apply(rows, filter).stream().map(ItemStorageRow::name).toList());
        assertEquals(List.of("Fast", "Armored"), ItemStorageQuery.apply(rows, filter, false).stream().map(ItemStorageRow::name).toList());
    }
}
