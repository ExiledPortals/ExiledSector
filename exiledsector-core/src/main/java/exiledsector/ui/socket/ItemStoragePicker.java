package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.Item;

import java.util.function.Function;

final class ItemStoragePicker implements BenchModeSupport.PickerSession {

    private final Function<Item, String> installedShipLookup;
    private final boolean sectionItemsOnly;
    private ItemStoragePanel storagePanel;

    ItemStoragePicker(Function<Item, String> installedShipLookup, boolean sectionItemsOnly) {
        this.installedShipLookup = installedShipLookup;
        this.sectionItemsOnly = sectionItemsOnly;
    }

    @Override
    public void open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Item loaded, BenchModeSupport.PickerListener listener) {
        storagePanel = ItemStoragePanel.openPicker(hostPanel, placement, installedShipLookup, new ItemStoragePanel.Listener() {
            @Override
            public void selected(Item item) {
                if (listener.accepting()) {
                    listener.picked(item);
                }
            }

            @Override
            public void closed() {
                storagePanel = null;
                listener.closed();
            }

            @Override
            public void pressedInside() {
                listener.pressedInside();
            }

            @Override
            public void disassembled() {
                listener.storeChanged();
            }
        }, sectionItemsOnly);
        if (storagePanel != null && loaded != null) {
            storagePanel.setSelected(loaded);
        }
    }

    @Override
    public boolean isOpen() {
        return storagePanel != null;
    }

    @Override
    public void changed(Item loaded) {
        if (storagePanel != null) {
            storagePanel.refresh(installedShipLookup);
            storagePanel.setSelected(loaded);
        }
    }

    @Override
    public void refresh() {
        if (storagePanel != null) {
            storagePanel.refresh(installedShipLookup);
        }
    }

    @Override
    public boolean escape() {
        return storagePanel != null && storagePanel.escape();
    }

    @Override
    public void close() {
        if (storagePanel != null) {
            storagePanel.close();
        }
    }

    @Override
    public boolean contains(float x, float y) {
        return storagePanel != null && storagePanel.contains(x, y);
    }
}
