package exiledsector.ui.socket;

public interface AdvancedSearchRoom {

    FreeArea open();

    default void closed() {
    }

    static AdvancedSearchRoom fixed(FreeArea area) {
        return () -> area;
    }
}
