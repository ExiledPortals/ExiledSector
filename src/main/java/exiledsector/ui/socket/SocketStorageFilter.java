package exiledsector.ui.socket;

public final class SocketStorageFilter {

    public enum Status {ALL, FREE, INSTALLED}

    public enum Sort {ACQUIRED, NAME, KIND, GRADE, ALIGNMENT, EFFECT_COUNT}

    static final SocketStorageFilter SESSION = new SocketStorageFilter();

    Status status = Status.ALL;
    Sort sort = Sort.ACQUIRED;
    boolean descending = true;
    String query = "";

    void setStatus(Status value) {
        status = value;
    }

    void cycleSort() {
        Sort[] sorts = Sort.values();
        sort = sorts[(sort.ordinal() + 1) % sorts.length];
        descending = sort == Sort.ACQUIRED;
    }

    void flipDirection() {
        descending = !descending;
    }

    void setQuery(String value) {
        query = value == null ? "" : value;
    }
}
