package exiledsector.skills;

import java.util.concurrent.atomic.AtomicReference;

public final class NodeGrants {

    public interface GrantLookup {

        String grantedNodeId(String socketedItemId);

        int revision();
    }

    public static final GrantLookup NONE = new GrantLookup() {
        @Override
        public String grantedNodeId(String socketedItemId) {
            return null;
        }

        @Override
        public int revision() {
            return 0;
        }
    };

    private static final AtomicReference<GrantLookup> LOOKUP = new AtomicReference<>(NONE);

    private NodeGrants() {
    }

    public static GrantLookup lookup() {
        return LOOKUP.get();
    }

    public static void install(GrantLookup grantLookup) {
        LOOKUP.set(grantLookup == null ? NONE : grantLookup);
    }
}
