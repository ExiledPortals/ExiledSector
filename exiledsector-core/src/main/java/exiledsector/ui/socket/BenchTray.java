package exiledsector.ui.socket;

import java.util.List;

public interface BenchTray {

    String titleKey();

    String emptyKey();

    List<String> iconPaths();

    List<BenchTrayEntry> entries(BenchTrayContext context);

    default boolean listed() {
        return false;
    }
}
