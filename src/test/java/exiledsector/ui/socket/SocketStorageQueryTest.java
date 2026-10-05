package exiledsector.ui.socket;

import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableRarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketStorageQueryTest {

    private static SocketStorageRow row(int order, String name, SocketableKind kind, String grade, String alignment, int effectCount,
                                        String effectText, String installedIn) {
        String search = (name + "\n" + effectText).toLowerCase();
        return new SocketStorageRow(null, order, name, SocketableRarity.MAGIC, kind, grade, alignment, effectCount, search, installedIn);
    }

    private static final SocketStorageRow MILITARY = row(0, "Military-grade Domain Subroutine", SocketableKind.SUBROUTINE, "military",
            "high_tech", 3, "Reduces shield upkeep by 25%. Increases beam weapon damage by 12%.", null);
    private static final SocketStorageRow CONSUMER = row(1, "Consumer-grade Domain Subroutine", SocketableKind.SUBROUTINE, "consumer",
            "low_tech", 2, "Increases top speed by 7%. Increases ballistic weapon damage by 8%.", "ISS Ravenous");
    private static final SocketStorageRow CORE = row(2, "Remnant Fragment", SocketableKind.AI_CORE, "military", "high_tech", 4,
            "Increases flux capacity by 5%.", null);
    private static final List<SocketStorageRow> ROWS = List.of(MILITARY, CONSUMER, CORE);

    private static List<String> names(List<SocketStorageRow> rows) {
        return rows.stream().map(SocketStorageRow::name).toList();
    }

    private static SocketStorageFilter filter() {
        SocketStorageFilter filter = new SocketStorageFilter();
        filter.sort = SocketStorageFilter.Sort.ACQUIRED;
        filter.descending = false;
        return filter;
    }

    @Test
    void anUntouchedFilterShowsEverythingNewestFirst() {
        assertEquals(names(List.of(CORE, CONSUMER, MILITARY)), names(SocketStorageQuery.apply(ROWS, new SocketStorageFilter())));
    }

    @Test
    void statusSplitsFreeFromInstalled() {
        SocketStorageFilter filter = filter();
        filter.setStatus(SocketStorageFilter.Status.FREE);
        assertEquals(names(List.of(MILITARY, CORE)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.setStatus(SocketStorageFilter.Status.INSTALLED);
        assertEquals(names(List.of(CONSUMER)), names(SocketStorageQuery.apply(ROWS, filter)));
    }

    @Test
    void searchFindsEffectsByTheirTextAndNeedsEveryWord() {
        SocketStorageFilter filter = filter();
        filter.setQuery("  Shield UPKEEP ");
        assertEquals(names(List.of(MILITARY)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.setQuery("weapon damage");
        assertEquals(names(List.of(MILITARY, CONSUMER)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.setQuery("remnant beam");
        assertEquals(List.of(), names(SocketStorageQuery.apply(ROWS, filter)));
    }

    @Test
    void everySortOrdersTheRowsAndTheDirectionFlipsIt() {
        SocketStorageFilter filter = filter();
        filter.sort = SocketStorageFilter.Sort.NAME;
        assertEquals(names(List.of(CONSUMER, MILITARY, CORE)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.sort = SocketStorageFilter.Sort.KIND;
        assertEquals(names(List.of(MILITARY, CONSUMER, CORE)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.sort = SocketStorageFilter.Sort.GRADE;
        assertEquals(names(List.of(CONSUMER, MILITARY, CORE)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.sort = SocketStorageFilter.Sort.ALIGNMENT;
        assertEquals(names(List.of(MILITARY, CORE, CONSUMER)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.sort = SocketStorageFilter.Sort.EFFECT_COUNT;
        assertEquals(names(List.of(CONSUMER, MILITARY, CORE)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.flipDirection();
        assertEquals(names(List.of(CORE, MILITARY, CONSUMER)), names(SocketStorageQuery.apply(ROWS, filter)));
    }

    @Test
    void cyclingTheSortVisitsEveryOrderAndOnlyAcquiredStartsNewestFirst() {
        SocketStorageFilter filter = new SocketStorageFilter();
        for (int i = 0; i < SocketStorageFilter.Sort.values().length; i++) {
            filter.cycleSort();
            assertEquals(filter.sort == SocketStorageFilter.Sort.ACQUIRED, filter.descending, filter.sort.name());
        }
        assertEquals(SocketStorageFilter.Sort.ACQUIRED, filter.sort);
    }
}
