package exiledsector.ui;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ShownScreens {

    public record Shown(Object screen, Runnable close) {
    }

    private static final Deque<Shown> SHOWN = new ArrayDeque<>();

    private ShownScreens() {
    }

    public static void shown(Object screen, Runnable close) {
        hidden(screen);
        SHOWN.push(new Shown(screen, close));
    }

    public static void hidden(Object screen) {
        SHOWN.removeIf(shown -> shown.screen() == screen);
    }

    public static Shown top() {
        return SHOWN.peek();
    }

    public static void clear() {
        SHOWN.clear();
    }
}
