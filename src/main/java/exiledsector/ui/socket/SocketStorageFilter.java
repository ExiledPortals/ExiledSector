package exiledsector.ui.socket;

import exiledsector.socketables.SocketableKind;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SocketStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    public enum Sort {ACQUIRED, NAME, KIND, GRADE, ALIGNMENT, EFFECT_COUNT}

    static final SocketStorageFilter SESSION = new SocketStorageFilter();

    final Set<SocketableKind> hiddenKinds = EnumSet.noneOf(SocketableKind.class);
    final Set<String> hiddenGrades = new HashSet<>();
    final Set<String> hiddenAlignments = new HashSet<>();
    final Set<String> themes = new LinkedHashSet<>();
    Status status = Status.ALL;
    Sort sort = Sort.ACQUIRED;
    boolean descending = true;
    String query = "";
    int page;

    void toggle(Set<String> hidden, String value) {
        if (!hidden.remove(value)) {
            hidden.add(value);
        }
        page = 0;
    }

    void toggleKind(SocketableKind kind) {
        if (!hiddenKinds.remove(kind)) {
            hiddenKinds.add(kind);
        }
        page = 0;
    }

    void toggleTheme(String theme) {
        if (!themes.remove(theme)) {
            themes.add(theme);
        }
        page = 0;
    }

    void setStatus(Status value) {
        status = value;
        page = 0;
    }

    void cycleSort() {
        Sort[] sorts = Sort.values();
        sort = sorts[(sort.ordinal() + 1) % sorts.length];
        descending = sort == Sort.ACQUIRED;
        page = 0;
    }

    void flipDirection() {
        descending = !descending;
        page = 0;
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
        page = 0;
    }
}
