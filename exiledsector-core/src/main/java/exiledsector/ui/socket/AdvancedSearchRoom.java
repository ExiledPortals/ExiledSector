package exiledsector.ui.socket;

import java.util.function.Consumer;

public interface AdvancedSearchRoom {

    void open(Consumer<FreeArea> ready);

    default void closed() {
    }

    static AdvancedSearchRoom fixed(FreeArea area) {
        return ready -> ready.accept(area);
    }
}
