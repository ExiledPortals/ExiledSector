package exiledsector.skills;

import java.util.concurrent.atomic.AtomicReference;

public final class AnchorLinks {

    public interface ExitLookup {

        String exitNodeId(String socketedItemId);

        boolean canOpen(String socketedItemId);

        int revision();
    }

    public static final ExitLookup NONE = new ExitLookup() {
        @Override
        public String exitNodeId(String socketedItemId) {
            return null;
        }

        @Override
        public boolean canOpen(String socketedItemId) {
            return false;
        }

        @Override
        public int revision() {
            return 0;
        }
    };

    private static final AtomicReference<ExitLookup> LOOKUP = new AtomicReference<>(NONE);

    private AnchorLinks() {
    }

    public static ExitLookup lookup() {
        return LOOKUP.get();
    }

    public static void install(ExitLookup exitLookup) {
        LOOKUP.set(exitLookup == null ? NONE : exitLookup);
    }
}
