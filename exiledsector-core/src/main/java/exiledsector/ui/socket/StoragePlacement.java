package exiledsector.ui.socket;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.Item;
import exiledsector.items.SocketCustody;

import java.util.Map;
import java.util.function.Function;

public final class StoragePlacement<T> {

    private final Function<Item, String> placingHint;
    private ItemStoragePanel storagePanel;
    private Item placingItem;
    private T target;
    private int storageRevision = Integer.MIN_VALUE;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();

    public StoragePlacement(Function<Item, String> placingHint) {
        this.placingHint = placingHint;
    }

    public ItemStoragePanel panel() {
        return storagePanel;
    }

    public boolean isOpen() {
        return storagePanel != null;
    }

    public Item placingItem() {
        return placingItem;
    }

    public T target() {
        return target;
    }

    public void reconcileOwnedShips() {
        ownedShips = SocketCustody.reconcile();
    }

    public Function<Item, String> shipNames() {
        return SocketCustody.shipNames(ownedShips);
    }

    public void opened(ItemStoragePanel panel, int revision) {
        storagePanel = panel;
        storageRevision = revision;
    }

    public boolean isStale(int revision) {
        return storagePanel != null && revision != storageRevision;
    }

    public void refresh(int revision) {
        if (storagePanel == null) {
            return;
        }
        storageRevision = revision;
        storagePanel.refresh(shipNames());
        if (placingItem != null) {
            storagePanel.setSelected(placingItem, placingHint.apply(placingItem));
        }
    }

    public void startPlacing(Item item) {
        placingItem = item;
        if (storagePanel != null) {
            storagePanel.setSelected(item, placingHint.apply(item));
        }
    }

    public void stopPlacing() {
        placingItem = null;
        if (storagePanel != null) {
            storagePanel.setSelected(null);
        }
    }

    public void setTarget(T newTarget) {
        target = newTarget;
        if (storagePanel != null) {
            storagePanel.setTargetingSocket(newTarget != null);
        }
    }

    public void forget() {
        storagePanel = null;
        placingItem = null;
        target = null;
    }
}
