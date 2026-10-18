package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.Item;
import exiledsector.ui.SkillTreeSounds;

import java.util.function.Function;

public final class ModificationBay {

    public interface Listener {

        void closed();

        void pressedInside();
    }

    static final float PANEL_GAP = 12f;
    static final float SIDE_BY_SIDE_MIN_SCALE = 3f;

    private final CustomPanelAPI hostPanel;
    private final Listener bayListener;
    private Function<Item, String> installedShipLookup;
    private SocketWorkbenchPanel benchPanel;
    private ItemStoragePanel storagePanel;
    private SocketPanelPlacement drawerPlacement;
    private boolean drawer;
    private boolean closing;

    private ModificationBay(CustomPanelAPI hostPanel, Function<Item, String> installedShipLookup, Listener bayListener) {
        this.hostPanel = hostPanel;
        this.installedShipLookup = installedShipLookup;
        this.bayListener = bayListener;
    }

    public static ModificationBay open(CustomPanelAPI hostPanel, SocketPanelPlacement area, Function<Item, String> installedShipLookup,
                                       Listener bayListener) {
        ModificationBay bay = new ModificationBay(hostPanel, installedShipLookup, bayListener);
        bay.layOut(area);
        return bay;
    }

    static boolean fitsSideBySide(float areaWidth, float areaHeight) {
        return SocketWorkbenchPanel.scaleFor(areaWidth - ItemStoragePanel.COMPACT_WIDTH - PANEL_GAP, areaHeight) >= SIDE_BY_SIDE_MIN_SCALE;
    }

    private void layOut(SocketPanelPlacement area) {
        drawer = !fitsSideBySide(area.panelWidth(), area.panelHeight());
        float benchAreaWidth = drawer ? area.panelWidth() : area.panelWidth() - ItemStoragePanel.COMPACT_WIDTH - PANEL_GAP;
        float scale = SocketWorkbenchPanel.scaleFor(benchAreaWidth, area.panelHeight());
        float benchWidth = SocketWorkbenchPanel.sizeFor(scale, BenchScene.ART_WIDTH);
        float benchHeight = SocketWorkbenchPanel.sizeFor(scale, BenchScene.ART_HEIGHT);
        float groupWidth = drawer ? benchWidth : ItemStoragePanel.COMPACT_WIDTH + PANEL_GAP + benchWidth;
        float groupLeft = area.panelLeft() + (area.panelWidth() - groupWidth) / 2f;
        float groupTop = area.panelTop() + (area.panelHeight() - benchHeight) / 2f;
        float benchLeft = drawer ? groupLeft : groupLeft + ItemStoragePanel.COMPACT_WIDTH + PANEL_GAP;
        benchPanel = SocketWorkbenchPanel.open(hostPanel, new SocketPanelPlacement(benchLeft, groupTop, benchWidth, benchHeight, area.instantly()),
                false, benchListener());
        if (drawer) {
            float inset = SocketWorkbenchPanel.FRAME_INSET;
            drawerPlacement = new SocketPanelPlacement(benchLeft + inset, groupTop + inset, ItemStoragePanel.COMPACT_WIDTH, benchHeight - inset * 2f,
                    false);
        } else {
            openStorage(new SocketPanelPlacement(groupLeft, groupTop, ItemStoragePanel.COMPACT_WIDTH, benchHeight, area.instantly()));
        }
    }

    private SocketWorkbenchPanel.Listener benchListener() {
        return new SocketWorkbenchPanel.Listener() {
            @Override
            public void changed(Item loaded) {
                if (storagePanel != null) {
                    storagePanel.refresh(installedShipLookup);
                    storagePanel.setSelected(loaded);
                }
            }

            @Override
            public void closed() {
                benchPanel = null;
                close();
            }

            @Override
            public void pressedInside() {
                bayListener.pressedInside();
            }

            @Override
            public boolean offersItemPicker() {
                return drawer;
            }

            @Override
            public void requestItem() {
                toggleDrawer();
            }
        };
    }

    private void toggleDrawer() {
        if (closing) {
            return;
        }
        if (storagePanel != null) {
            storagePanel.close();
            return;
        }
        SkillTreeSounds.panelOpened();
        openStorage(drawerPlacement);
    }

    private void openStorage(SocketPanelPlacement placement) {
        storagePanel = ItemStoragePanel.openPicker(hostPanel, placement, installedShipLookup, new ItemStoragePanel.Listener() {
            @Override
            public void selected(Item item) {
                if (item == null) {
                    if (storagePanel != null) {
                        storagePanel.setSelected(null);
                    }
                    return;
                }
                if (benchPanel == null) {
                    return;
                }
                benchPanel.load(item);
                if (drawer && storagePanel != null) {
                    storagePanel.close();
                } else if (storagePanel != null) {
                    storagePanel.setSelected(item);
                }
            }

            @Override
            public void closed() {
                storagePanel = null;
                if (!drawer) {
                    close();
                }
            }

            @Override
            public void pressedInside() {
                bayListener.pressedInside();
            }

            @Override
            public void disassembled() {
                if (benchPanel != null) {
                    benchPanel.refresh();
                }
            }
        });
        if (benchPanel != null && benchPanel.loaded() != null) {
            storagePanel.setSelected(benchPanel.loaded());
        }
    }

    public void refresh(Function<Item, String> lookup) {
        installedShipLookup = lookup;
        if (storagePanel != null) {
            storagePanel.refresh(lookup);
        }
        if (benchPanel != null) {
            benchPanel.refresh();
        }
    }

    public boolean contains(float x, float y) {
        return (benchPanel != null && benchPanel.contains(x, y)) || (storagePanel != null && storagePanel.contains(x, y));
    }

    public boolean isOpen() {
        return !closing;
    }

    public boolean escape() {
        if (closing) {
            return false;
        }
        if (benchPanel != null && benchPanel.escape()) {
            return true;
        }
        if (storagePanel != null && storagePanel.escape()) {
            return true;
        }
        if (drawer && storagePanel != null) {
            storagePanel.close();
            return true;
        }
        close();
        return true;
    }

    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (benchPanel != null) {
            benchPanel.close();
        }
        bayListener.closed();
    }
}
