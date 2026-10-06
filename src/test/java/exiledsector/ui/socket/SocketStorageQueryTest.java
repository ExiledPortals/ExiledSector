package exiledsector.ui.socket;

import exiledsector.socketables.SocketableRarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketStorageQueryTest {

    private static SocketStorageRow row(int order, String name, SocketableRarity rarity, String grade, String effectText,
                                        String installedIn) {
        String search = (name + "\n" + effectText).toLowerCase();
        return new SocketStorageRow(null, order, name, rarity, grade, search, installedIn);
    }

    private static final SocketStorageRow MILITARY = row(0, "Military-grade Domain Subroutine", SocketableRarity.COMMON, "military",
            "Reduces shield upkeep by 25%. Increases beam weapon damage by 12%.", null);
    private static final SocketStorageRow CONSUMER = row(1, "Consumer-grade Domain Subroutine", SocketableRarity.RARE, "consumer",
            "Increases top speed by 7%. Increases ballistic weapon damage by 8%.", "ISS Ravenous");
    private static final SocketStorageRow CORE = row(2, "Remnant Fragment", SocketableRarity.UNIQUE, "military",
            "Increases flux capacity by 5%.", null);
    private static final List<SocketStorageRow> ROWS = List.of(MILITARY, CONSUMER, CORE);

    private static List<String> names(List<SocketStorageRow> rows) {
        return rows.stream().map(SocketStorageRow::name).toList();
    }

    private static List<String> shown(SocketStorageFilter filter) {
        return names(SocketStorageQuery.apply(ROWS, filter));
    }

    private static SocketStorageFilter everything() {
        SocketStorageFilter filter = new SocketStorageFilter();
        filter.setStatus(SocketStorageFilter.Status.ALL);
        return filter;
    }

    @Test
    void anUntouchedFilterShowsOnlyFreeItemsNewestFirst() {
        assertEquals(names(List.of(CORE, MILITARY)), shown(new SocketStorageFilter()));
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(everything()));
    }

    @Test
    void statusSplitsFreeFromInstalled() {
        SocketStorageFilter filter = new SocketStorageFilter();
        filter.setStatus(SocketStorageFilter.Status.FREE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.setStatus(SocketStorageFilter.Status.INSTALLED);
        assertEquals(names(List.of(CONSUMER)), shown(filter));
    }

    @Test
    void rarityChipsShowAnyChosenRarityAndTogglingOneOffRemovesIt() {
        SocketStorageFilter filter = everything();
        filter.toggleRarity(SocketableRarity.COMMON);
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.toggleRarity(SocketableRarity.UNIQUE);
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.toggleRarity(SocketableRarity.COMMON);
        filter.toggleRarity(SocketableRarity.UNIQUE);
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));
    }

    @Test
    void gradeChipsShowAnyChosenGradeAndCombineWithRarity() {
        SocketStorageFilter filter = everything();
        filter.toggleGrade("military");
        assertEquals(names(List.of(CORE, MILITARY)), shown(filter));

        filter.toggleGrade("consumer");
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), shown(filter));

        filter.toggleRarity(SocketableRarity.RARE);
        assertEquals(names(List.of(CONSUMER)), shown(filter));
    }

    @Test
    void searchFindsEffectsByTheirTextAndNeedsEveryWord() {
        SocketStorageFilter filter = everything();
        filter.setQuery("  Shield UPKEEP ");
        assertEquals(names(List.of(MILITARY)), shown(filter));

        filter.setQuery("weapon damage");
        assertEquals(names(List.of(CONSUMER, MILITARY)), shown(filter));

        filter.setQuery("remnant beam");
        assertEquals(List.of(), shown(filter));
    }
}
