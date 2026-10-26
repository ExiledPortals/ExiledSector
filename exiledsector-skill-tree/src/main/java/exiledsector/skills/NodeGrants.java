package exiledsector.skills;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class NodeGrants {

    public interface GrantLookup {

        List<String> grantedNodeIds(String socketedItemId);

        default List<String> grantedNodeIds(String socketedItemId, String socketId) {
            return grantedNodeIds(socketedItemId);
        }

        int revision();
    }

    public static final GrantLookup NONE = new GrantLookup() {
        @Override
        public List<String> grantedNodeIds(String socketedItemId) {
            return List.of();
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
