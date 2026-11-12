package exiledsector.ui.atlas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class OfficerBoardLayout {

    record Entry(int recordId, String rankId, int renown) {
    }

    record Seat(int row, int column, int rowSize) {
    }

    private static final Comparator<Entry> ROW_ORDER = Comparator.comparingInt(Entry::renown).reversed().thenComparingInt(Entry::recordId);

    private final List<String> rankIdsTopFirst;
    private final List<List<Integer>> rows;
    private final Map<Integer, Seat> seats;

    private OfficerBoardLayout(List<String> rankIdsTopFirst, List<List<Integer>> rows, Map<Integer, Seat> seats) {
        this.rankIdsTopFirst = rankIdsTopFirst;
        this.rows = rows;
        this.seats = seats;
    }

    static OfficerBoardLayout of(List<String> rankIdsLowestFirst, Collection<Entry> entries) {
        List<String> rankIdsTopFirst = new ArrayList<>(rankIdsLowestFirst);
        Collections.reverse(rankIdsTopFirst);
        List<List<Entry>> entriesByRow = new ArrayList<>();
        for (int row = 0; row < rankIdsTopFirst.size(); row++) {
            entriesByRow.add(new ArrayList<>());
        }
        for (Entry entry : entries) {
            int row = rankIdsTopFirst.indexOf(entry.rankId());
            entriesByRow.get(row < 0 ? rankIdsTopFirst.size() - 1 : row).add(entry);
        }
        List<List<Integer>> rows = new ArrayList<>();
        Map<Integer, Seat> seats = new HashMap<>();
        for (int row = 0; row < entriesByRow.size(); row++) {
            List<Entry> rowEntries = entriesByRow.get(row);
            rowEntries.sort(ROW_ORDER);
            List<Integer> recordIds = new ArrayList<>();
            for (int column = 0; column < rowEntries.size(); column++) {
                int recordId = rowEntries.get(column).recordId();
                recordIds.add(recordId);
                seats.put(recordId, new Seat(row, column, rowEntries.size()));
            }
            rows.add(List.copyOf(recordIds));
        }
        return new OfficerBoardLayout(List.copyOf(rankIdsTopFirst), List.copyOf(rows), Map.copyOf(seats));
    }

    int rowCount() {
        return rows.size();
    }

    List<Integer> row(int row) {
        return rows.get(row);
    }

    String rankIdOfRow(int row) {
        return rankIdsTopFirst.get(row);
    }

    int rowOfRank(String rankId) {
        int row = rankIdsTopFirst.indexOf(rankId);
        return row < 0 ? rankIdsTopFirst.size() - 1 : row;
    }

    Seat seat(int recordId) {
        return seats.get(recordId);
    }

    boolean contains(int recordId) {
        return seats.containsKey(recordId);
    }

    int widestRow() {
        int widest = 0;
        for (List<Integer> row : rows) {
            widest = Math.max(widest, row.size());
        }
        return widest;
    }
}
