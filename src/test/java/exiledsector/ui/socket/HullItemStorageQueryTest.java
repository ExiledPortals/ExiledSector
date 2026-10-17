package exiledsector.ui.socket;

import exiledsector.hullitems.HullItemRarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HullItemStorageQueryTest {

    private static HullItemStorageRow row(int order, String name, HullItemRarity rarity, String effectText,
                                        String installedIn) {
        String search = (name + "\n" + effectText).toLowerCase();
        return new HullItemStorageRow(null, order, name, rarity, search, installedIn);
    }

    private static final HullItemStorageRow MILITARY = row(0, "Military-grade Domain Subroutine", HullItemRarity.COMMON,
            "Reduces shield upkeep by 25%. Increases beam weapon damage by 12%.", null);
    private static final HullItemStorageRow CONSUMER = row(1, "Consumer-grade Domain Subroutine", HullItemRarity.RARE,
            "Increases top speed by 7%. Increases ballistic weapon damage by 8%.", "ISS Ravenous");
    private static final HullItemStorageRow CORE = row(2, "Remnant Fragment", HullItemRarity.UNIQUE,
            "Increases flux capacity by 5%.", null);
    private static final List<HullItemStorageRow> ROWS = List.of(MILITARY, CONSUMER, CORE);

    private static List<String> names(List<HullItemStorageRow> rows) {
        return rows.stream().map(HullItemStorageRow::name).toList();
    }

    private static List<String> shown(HullItemStorageFilter filter) {
        return names(HullItemStorageQuery.apply(ROWS, filter));
    }

    private static HullItemStorageFilter everything() {
        HullItemStorageFilter filter = new HullItemStorageFilter();
        filter.setStatus(HullItemStorageFilter.Status.ALL);
        return filter;
    }

    @Test
    void anUntouchedFilterShowsOnlyFreeItemsNewestFirst() {
        assertEquals(names(List.of(CORE, MILITARY)), shown(new HullItemStorageFilter()));
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(everything()));
    }

    @Test
    void statusSplitsFreeFromInstalled() {
        HullItemStorageFilter filter = new HullItemStorageFilter();
        filter.setStatus(HullItemStorageFilter.Status.FREE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.setStatus(HullItemStorageFilter.Status.INSTALLED);
        assertEquals(names(List.of(CONSUMER)), shown(filter));
    }

    @Test
    void rarityChipsShowAnyChosenRarityAndTogglingOneOffRemovesIt() {
        HullItemStorageFilter filter = everything();
        filter.toggleRarity(HullItemRarity.COMMON);
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.toggleRarity(HullItemRarity.UNIQUE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.toggleRarity(HullItemRarity.COMMON);
        filter.toggleRarity(HullItemRarity.UNIQUE);
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));
    }

    @Test
    void searchFindsEffectsByTheirTextAndNeedsEveryWord() {
        HullItemStorageFilter filter = everything();
        filter.setQuery("  Shield UPKEEP ");
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.setQuery("weapon damage");
        assertEquals(names(List.of(CONSUMER, MILITARY)), shown(filter));

        filter.setQuery("remnant beam");
        assertEquals(List.of(), shown(filter));
    }
}
