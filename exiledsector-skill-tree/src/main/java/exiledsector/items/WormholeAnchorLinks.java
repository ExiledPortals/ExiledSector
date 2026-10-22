package exiledsector.items;

import com.fs.starfarer.api.Global;
import exiledlib.items.ItemStore;
import exiledsector.skills.AnchorLinks;

import java.util.concurrent.atomic.AtomicInteger;

public final class WormholeAnchorLinks implements AnchorLinks.ExitLookup {

    private static final WormholeAnchorLinks INSTANCE = new WormholeAnchorLinks();
    private static final AtomicInteger REVISION = new AtomicInteger();

    private WormholeAnchorLinks() {
    }

    public static void install() {
        AnchorLinks.install(INSTANCE);
    }

    public static void refresh() {
        REVISION.incrementAndGet();
    }

    @Override
    public String exitNodeId(String socketedItemId) {
        WormholeAnchor anchor = anchor(socketedItemId);
        return anchor == null ? null : anchor.linkedExitNodeId();
    }

    @Override
    public boolean canOpen(String socketedItemId) {
        WormholeAnchor anchor = anchor(socketedItemId);
        if (anchor == null) {
            return false;
        }
        return NpcItems.isNpcId(socketedItemId) ? anchor.linkedExitNodeId() != null : anchor.canOpen(Global.getSector());
    }

    private static WormholeAnchor anchor(String socketedItemId) {
        return ItemStore.lookup(socketedItemId) instanceof WormholeAnchor anchor ? anchor : null;
    }

    @Override
    public int revision() {
        return REVISION.get();
    }
}
