package exiledsector.ui.socket;

import exiledsector.items.ItemRarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemStorageQueryTest {

    private static ItemStorageRow row(int order, String name, ItemRarity rarity, String effectText,
                                        String installedIn) {
        String search = (name + "\n" + effectText).toLowerCase();
        return new ItemStorageRow(null, order, name, rarity, search, installedIn);
    }

    private static final ItemStorageRow MILITARY = row(0, "Military-grade Domain Subroutine", ItemRarity.COMMON,
            "Reduces shield upkeep by 25%. Increases beam weapon damage by 12%.", null);
    private static final ItemStorageRow CONSUMER = row(1, "Consumer-grade Domain Subroutine", ItemRarity.RARE,
            "Increases top speed by 7%. Increases ballistic weapon damage by 8%.", "ISS Ravenous");
    private static final ItemStorageRow CORE = row(2, "Remnant Fragment", ItemRarity.UNIQUE,
            "Increases flux capacity by 5%.", null);
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
    void searchFindsEffectsByTheirTextAndNeedsEveryWord() {
        ItemStorageFilter filter = everything();
        filter.setQuery("  Shield UPKEEP ");
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.setQuery("weapon damage");
        assertEquals(names(List.of(CONSUMER, MILITARY)), shown(filter));

        filter.setQuery("remnant beam");
        assertEquals(List.of(), shown(filter));
    }
}
