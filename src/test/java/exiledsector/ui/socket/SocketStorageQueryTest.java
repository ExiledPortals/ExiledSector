package exiledsector.ui.socket;

import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableRarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketStorageQueryTest {

    private static SocketStorageRow row(int order, String name, SocketableKind kind, String grade, String alignment, Set<String> themes,
                                        int effectCount, String effectText, String installedIn) {
        String search = (name + "\n" + effectText).toLowerCase();
        return new SocketStorageRow(null, order, name, SocketableRarity.MAGIC, null, kind, grade, alignment, themes, effectCount, search,
                installedIn, List.of(), List.of());
    }

    private static final SocketStorageRow MILITARY = row(0, "Military-grade Domain Subroutine", SocketableKind.SUBROUTINE, "military",
            "high_tech", Set.of("shield", "beam"), 3, "Reduces shield upkeep by 25%. Increases beam weapon damage by 12%.", null);
    private static final SocketStorageRow CONSUMER = row(1, "Consumer-grade Domain Subroutine", SocketableKind.SUBROUTINE, "consumer",
            "low_tech", Set.of("speed", "ballistic"), 2, "Increases top speed by 7%. Increases ballistic weapon damage by 8%.",
            "ISS Ravenous");
    private static final SocketStorageRow CORE = row(2, "Remnant Fragment", SocketableKind.AI_CORE, "military", "high_tech",
            Set.of("flux"), 4, "Increases flux capacity by 5%.", null);
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
    void hiddenKindsGradesAndAlignmentsAreLeftOut() {
        SocketStorageFilter filter = filter();
        filter.toggleKind(SocketableKind.AI_CORE);
        assertEquals(names(List.of(MILITARY, CONSUMER)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.toggle(filter.hiddenGrades, "consumer");
        assertEquals(names(List.of(MILITARY)), names(SocketStorageQuery.apply(ROWS, filter)));

        filter.toggleKind(SocketableKind.AI_CORE);
        filter.toggle(filter.hiddenAlignments, "high_tech");
        assertEquals(List.of(), names(SocketStorageQuery.apply(ROWS, filter)));
    }

    @Test
    void tickedThemesKeepItemsWithAnyOfThem() {
        SocketStorageFilter filter = filter();
        filter.toggleTheme("beam");
        filter.toggleTheme("flux");

        assertEquals(names(List.of(MILITARY, CORE)), names(SocketStorageQuery.apply(ROWS, filter)));
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

    @Test
    void pagesHoldFortyRowsAndOutOfRangePagesSnapBack() {
        List<SocketStorageRow> many = new ArrayList<>();
        for (int i = 0; i < 95; i++) {
            many.add(row(i, "Item " + i, SocketableKind.TEAM, "", "", Set.of(), 2, "", null));
        }

        assertEquals(3, SocketStorageQuery.pageCount(many.size()));
        assertEquals(1, SocketStorageQuery.pageCount(0));
        assertEquals(40, SocketStorageQuery.page(many, 0).size());
        assertEquals(15, SocketStorageQuery.page(many, 2).size());
        assertEquals("Item 80", SocketStorageQuery.page(many, 7).get(0).name());
        assertEquals(0, SocketStorageQuery.clampPage(-3, many.size()));
    }

    @Test
    void changingAFilterGoesBackToTheFirstPage() {
        SocketStorageFilter filter = filter();
        filter.page = 4;
        filter.toggleTheme("flux");
        assertEquals(0, filter.page);
        filter.page = 4;
        filter.setQuery("x");
        assertEquals(0, filter.page);
    }
}
