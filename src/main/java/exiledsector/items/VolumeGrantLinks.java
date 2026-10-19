package exiledsector.items;

import exiledsector.skills.NodeGrants;

import java.util.concurrent.atomic.AtomicInteger;

public final class VolumeGrantLinks implements NodeGrants.GrantLookup {

    private static final VolumeGrantLinks INSTANCE = new VolumeGrantLinks();
    private static final AtomicInteger REVISION = new AtomicInteger();

    private VolumeGrantLinks() {
    }

    public static void install() {
        NodeGrants.install(INSTANCE);
    }

    public static void refresh() {
        REVISION.incrementAndGet();
    }

    @Override
    public String grantedNodeId(String socketedItemId) {
        Subroutine volumeSubroutine = volumeSubroutine(socketedItemId);
        return volumeSubroutine == null ? null : volumeSubroutine.currentNodeId();
    }

    static Subroutine volumeSubroutine(String socketedItemId) {
        if (socketedItemId == null || NpcItems.isNpcId(socketedItemId)) {
            return null;
        }
        return ItemStore.lookup(socketedItemId) instanceof Subroutine subroutine && subroutine.subroutineKind() == SubroutineKind.VOLUME
                ? subroutine : null;
    }

    @Override
    public int revision() {
        return REVISION.get();
    }
}
