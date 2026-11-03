package exiledsector.ui.socket;

import exiledsector.items.ItemRarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemStorageQueryTest {

    private static ItemStorageRow row(int order, String name, ItemRarity rarity, String installedIn, String... effectLines) {
        List<String> searchLines = new ArrayList<>();
        searchLines.add(name.toLowerCase(Locale.ROOT));
        for (String effectLine : effectLines) {
            searchLines.add(effectLine.toLowerCase(Locale.ROOT));
        }
        return new ItemStorageRow(null, order, name, rarity, searchLines, installedIn);
    }

    private static final ItemStorageRow MILITARY = row(0, "Military-grade Domain Subroutine", ItemRarity.COMMON, null,
            "Reduces shield upkeep by 25%.", "Increases beam weapon damage by 12%.");
    private static final ItemStorageRow CONSUMER = row(1, "Consumer-grade Domain Subroutine", ItemRarity.RARE, "ISS Ravenous",
            "Increases top speed by 7%.", "Increases ballistic weapon damage by 8%.");
    private static final ItemStorageRow CORE = row(2, "Remnant Fragment", ItemRarity.UNIQUE, null,
            "Increases flux capacity by 5%.", "Reduces shield upkeep by 10%.");
    private static final List<ItemStorageRow> ROWS = List.of(MILITARY, CONSUMER, CORE);

    private static List<String> names(List<ItemStorageRow> rows) {
        return rows.stream().map(ItemStorageRow::name).toList();
    }

    private static List<String> shown(ItemStorageFilter filter) {
        return names(ItemStorageQuery.apply(ROWS, filter));
    }

    private static ItemStorageFilter everything() {
        ItemStorageFilter filter = new ItemStorageFilter();
        filter.setStatus(ItemStorageFilter.Status.ALL);
        return filter;
    }

    @Test
    void anUntouchedFilterShowsOnlyFreeItemsNewestFirst() {
        assertEquals(names(List.of(CORE, MILITARY)), shown(new ItemStorageFilter()));
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(everything()));
    }

    @Test
    void statusSplitsFreeFromInstalled() {
        ItemStorageFilter filter = new ItemStorageFilter();
        filter.setStatus(ItemStorageFilter.Status.FREE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.setStatus(ItemStorageFilter.Status.INSTALLED);
        assertEquals(names(List.of(CONSUMER)), shown(filter));
    }

    @Test
    void rarityChipsShowAnyChosenRarityAndTogglingOneOffRemovesIt() {
        ItemStorageFilter filter = everything();
        filter.toggleRarity(ItemRarity.COMMON);
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.toggleRarity(ItemRarity.UNIQUE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.toggleRarity(ItemRarity.COMMON);
        filter.toggleRarity(ItemRarity.UNIQUE);
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));
    }

    @Test
    void searchFindsEffectsByTheirTextAndNeedsEveryWordOnOneLine() {
        ItemStorageFilter filter = everything();
        filter.setQuery("  Shield UPKEEP ");
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.setQuery("weapon damage");
        assertEquals(names(List.of(CONSUMER, MILITARY)), shown(filter));

        filter.setQuery("shield damage");
        assertEquals(List.of(), shown(filter));

        filter.setQuery("remnant");
        assertEquals(names(List.of(CORE)), shown(filter));
    }

    @Test
    void aNumberWithPlusKeepsOnlyLinesOfAtLeastThatValue() {
        ItemStorageFilter filter = everything();
        filter.setQuery("shield upkeep 20+");
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.setQuery("10+");
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.setQuery("speed 8+");
        assertEquals(List.of(), shown(filter));
    }

    @Test
    void rarityOrderPutsTheRarestFirstAndFlipsOnASecondPick() {
        ItemStorageFilter filter = everything();
        filter.pickSort(ItemStorageFilter.Sort.RARITY);
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));

        filter.pickSort(ItemStorageFilter.Sort.RARITY);
        assertEquals(names(List.of(MILITARY, CONSUMER, CORE)), shown(filter));
    }

    @Test
    void ageOrderFlipsToOldestFirstAndAnotherSortStartsUnflipped() {
        ItemStorageFilter filter = everything();
        filter.pickSort(ItemStorageFilter.Sort.AGE);
        assertEquals(names(List.of(MILITARY, CONSUMER, CORE)), shown(filter));

        filter.pickSort(ItemStorageFilter.Sort.RARITY);
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));
    }

    @Test
    void valueOrderRanksByTheMatchedLineAndPutsItemsWithoutOneLast() {
        ItemStorageFilter filter = everything();
        filter.pickSort(ItemStorageFilter.Sort.VALUE);
        filter.setQuery("damage");
        assertEquals(names(List.of(MILITARY, CONSUMER)), shown(filter));

        filter.setQuery("shield upkeep");
        assertEquals(names(List.of(MILITARY, CORE)), shown(filter));

        filter.pickSort(ItemStorageFilter.Sort.VALUE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.setQuery("");
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));

        filter.setQuery("subroutine");
        assertEquals(names(List.of(CONSUMER, MILITARY)), shown(filter));
    }

    @Test
    void theLineValueIsItsFirstStandaloneNumber() {
        assertEquals(OptionalDouble.of(25), ItemStorageSearch.firstNumber("reduces shield upkeep by 25%."));
        assertEquals(OptionalDouble.of(1.5), ItemStorageSearch.firstNumber("multiplies range by 1.5x, then 3 more"));
        assertEquals(OptionalDouble.of(-10), ItemStorageSearch.firstNumber("-10% hull"));
        assertEquals(OptionalDouble.of(1200), ItemStorageSearch.firstNumber("adds 1,200 armor"));
        assertEquals(OptionalDouble.of(5), ItemStorageSearch.firstNumber("tier-3 core adds 5"));
        assertEquals(OptionalDouble.empty(), ItemStorageSearch.firstNumber("no numbers here"));
    }
}
