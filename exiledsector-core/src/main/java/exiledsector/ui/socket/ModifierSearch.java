package exiledsector.ui.socket;

import exiledsector.items.ItemStatTotals;
import exiledsector.modifiers.ModifierCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class ModifierSearch {

    static final String DEFAULT_COUNT_TEXT = "1";
    private static final double BOUND_TOLERANCE = 1e-6;
    private static final Set<ModifierCategory> ALL_LINES = Collections.unmodifiableSet(EnumSet.allOf(ModifierCategory.class));

    enum GroupKind {AND, COUNT}

    static final class Row {

        String statName;
        String minText = "";
        String maxText = "";

        boolean hasStat() {
            return statName != null;
        }
    }

    static final class Group {

        final GroupKind kind;
        final List<Row> rows = new ArrayList<>();
        String countText = DEFAULT_COUNT_TEXT;

        Group(GroupKind kind) {
            this.kind = kind;
        }
    }

    record RowCriteria(String statName, Double min, Double max) {

        boolean matches(ItemStatTotals totals) {
            for (double total : totals.totals(statName, ALL_LINES)) {
                if ((min == null || total >= min - BOUND_TOLERANCE) && (max == null || total <= max + BOUND_TOLERANCE)) {
                    return true;
                }
            }
            return false;
        }
    }

    record GroupCriteria(GroupKind kind, int requiredMatches, List<RowCriteria> rows) {

        boolean matches(ItemStatTotals totals) {
            if (kind == GroupKind.AND) {
                return rows.stream().allMatch(row -> row.matches(totals));
            }
            return rows.stream().filter(row -> row.matches(totals)).count() >= requiredMatches;
        }
    }

    private final List<Group> groups = new ArrayList<>();
    private List<GroupCriteria> applied = List.of();

    List<Group> groups() {
        return Collections.unmodifiableList(groups);
    }

    Group addGroup(GroupKind kind) {
        Group group = new Group(kind);
        group.rows.add(new Row());
        groups.add(group);
        return group;
    }

    Row addRow(Group group) {
        Row row = new Row();
        group.rows.add(row);
        return row;
    }

    void removeRow(Group group, Row row) {
        group.rows.remove(row);
        if (group.rows.isEmpty()) {
            groups.remove(group);
        }
    }

    void apply() {
        applied = criteria();
    }

    void clear() {
        groups.clear();
        applied = List.of();
    }

    boolean isActive() {
        return !applied.isEmpty();
    }

    boolean isDefault() {
        return applied.isEmpty() && groups.stream().noneMatch(group -> group.rows.stream().anyMatch(Row::hasStat));
    }

    boolean matches(ItemStatTotals totals) {
        return applied.stream().allMatch(group -> group.matches(totals));
    }

    List<GroupCriteria> criteria() {
        List<GroupCriteria> compiled = new ArrayList<>();
        for (Group group : groups) {
            List<RowCriteria> rows = group.rows.stream().filter(Row::hasStat)
                    .map(row -> new RowCriteria(row.statName, parseBound(row.minText), parseBound(row.maxText))).toList();
            if (!rows.isEmpty()) {
                int requiredMatches = group.kind == GroupKind.COUNT ? parseCount(group.countText) : rows.size();
                compiled.add(new GroupCriteria(group.kind, requiredMatches, rows));
            }
        }
        return List.copyOf(compiled);
    }

    static Double parseBound(String boundText) {
        if (boundText == null) {
            return null;
        }
        String cleaned = boundText.replace("%", "").replace("+", "").replace(',', '.').replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            double bound = Double.parseDouble(cleaned);
            return Double.isFinite(bound) ? bound : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static int parseCount(String countText) {
        Double count = parseBound(countText);
        return count == null ? Integer.parseInt(DEFAULT_COUNT_TEXT) : (int) Math.ceil(count);
    }
}
