package exiledsector.ui.socket;

import java.util.List;

final class BenchTrays {

    private BenchTray open;

    BenchTray open() {
        return open;
    }

    boolean isOpen() {
        return open != null;
    }

    boolean isOpen(BenchTray tray) {
        return open == tray;
    }

    void toggle(BenchTray tray) {
        open = open == tray ? null : tray;
    }

    void close() {
        open = null;
    }

    void keepOnly(List<BenchTray> offered) {
        if (open != null && !offered.contains(open)) {
            open = null;
        }
    }
}
