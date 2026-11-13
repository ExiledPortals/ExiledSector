package exiledsector.ui.atlas;

import exiledsector.charts.officer.OfficerRank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class OfficerBoardLayout {

    record Entry(int recordId, String rankId, int renown, int seat) {
    }

    record Seat(int row, int column, int rowSize) {
    }

    record Link(Seat subordinate, Seat superior) {

        boolean touches(Seat seat) {
            return subordinate.equals(seat) || superior.equals(seat);
        }
    }

    private static final Comparator<Entry> UNSEATED_ORDER = Comparator.comparingInt(Entry::renown).reversed().thenComparingInt(Entry::recordId);

    private final List<String> rankIdsTopFirst;
    private final List<List<Integer>> rows;
    private final List<Integer> rowSizes;
    private final Map<Integer, Seat> seats;

    private OfficerBoardLayout(List<String> rankIdsTopFirst, List<List<Integer>> rows, List<Integer> rowSizes, Map<Integer, Seat> seats) {
        this.rankIdsTopFirst = rankIdsTopFirst;
        this.rows = rows;
        this.rowSizes = rowSizes;
        this.seats = seats;
    }

    static OfficerBoardLayout of(List<OfficerRank> ranksLowestFirst, Collection<Entry> entries) {
        List<OfficerRank> ranksTopFirst = new ArrayList<>(ranksLowestFirst);
        Collections.reverse(ranksTopFirst);
        List<String> rankIdsTopFirst = ranksTopFirst.stream().map(OfficerRank::id).toList();
        List<List<Entry>> entriesByRow = new ArrayList<>();
        for (int row = 0; row < rankIdsTopFirst.size(); row++) {
            entriesByRow.add(new ArrayList<>());
        }
        for (Entry entry : entries) {
            int row = rankIdsTopFirst.indexOf(entry.rankId());
            entriesByRow.get(row < 0 ? rankIdsTopFirst.size() - 1 : row).add(entry);
        }
        List<List<Integer>> rows = new ArrayList<>();
        List<Integer> rowSizes = new ArrayList<>();
        Map<Integer, Seat> seats = new HashMap<>();
        for (int row = 0; row < entriesByRow.size(); row++) {
            TreeMap<Integer, Integer> recordIdByColumn = columns(entriesByRow.get(row), ranksTopFirst.get(row).places());
            int rowSize = Math.max(ranksTopFirst.get(row).places(), recordIdByColumn.isEmpty() ? 0 : recordIdByColumn.lastKey() + 1);
            recordIdByColumn.forEach((column, recordId) -> seats.put(recordId, new Seat(rowSizes.size(), column, rowSize)));
            rows.add(List.copyOf(recordIdByColumn.values()));
            rowSizes.add(rowSize);
        }
        return new OfficerBoardLayout(rankIdsTopFirst, List.copyOf(rows), List.copyOf(rowSizes), Map.copyOf(seats));
    }

    private static TreeMap<Integer, Integer> columns(List<Entry> rowEntries, int places) {
        TreeMap<Integer, Integer> recordIdByColumn = new TreeMap<>();
        List<Entry> unseated = new ArrayList<>();
        rowEntries.sort(Comparator.comparingInt(Entry::recordId));
        for (Entry entry : rowEntries) {
            if (entry.seat() >= 0 && entry.seat() < places && !recordIdByColumn.containsKey(entry.seat())) {
                recordIdByColumn.put(entry.seat(), entry.recordId());
            } else {
                unseated.add(entry);
            }
        }
        unseated.sort(UNSEATED_ORDER);
        int column = 0;
        for (Entry entry : unseated) {
            while (recordIdByColumn.containsKey(column)) {
                column++;
            }
            recordIdByColumn.put(column, entry.recordId());
        }
        return recordIdByColumn;
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
        for (int rowSize : rowSizes) {
            widest = Math.max(widest, rowSize);
        }
        return widest;
    }

    List<Link> links() {
        List<Link> links = new ArrayList<>();
        for (int row = 1; row < rowSizes.size(); row++) {
            int rowSize = rowSizes.get(row);
            int superiorRowSize = rowSizes.get(row - 1);
            if (rowSize <= 0 || superiorRowSize <= 0) {
                continue;
            }
            for (int column = 0; column < rowSize; column++) {
                links.add(new Link(new Seat(row, column, rowSize), new Seat(row - 1, column * superiorRowSize / rowSize, superiorRowSize)));
            }
        }
        return List.copyOf(links);
    }
}
