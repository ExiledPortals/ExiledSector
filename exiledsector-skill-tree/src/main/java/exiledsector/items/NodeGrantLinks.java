package exiledsector.items;

import exiledsector.skills.NodeGrants;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class NodeGrantLinks implements NodeGrants.GrantLookup {

    private static final NodeGrantLinks INSTANCE = new NodeGrantLinks();
    private static final AtomicInteger REVISION = new AtomicInteger();

    private NodeGrantLinks() {
    }

    public static void install() {
        NodeGrants.install(INSTANCE);
    }

    public static void refresh() {
        REVISION.incrementAndGet();
    }

    @Override
    public List<String> grantedNodeIds(String socketedItemId) {
        Subroutine cache = modspecCache(socketedItemId);
        return cache == null ? List.of() : cache.currentNodeIds();
    }

    static Subroutine modspecCache(String socketedItemId) {
        if (socketedItemId == null || NpcItems.isNpcId(socketedItemId)) {
            return null;
        }
        return ItemStore.lookup(socketedItemId) instanceof Subroutine subroutine && subroutine.subroutineKind() == SubroutineKind.MODSPEC_CACHE
                ? subroutine : null;
    }

    @Override
    public int revision() {
        return REVISION.get();
    }
}
