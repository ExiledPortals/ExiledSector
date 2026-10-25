package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.Item;
import exiledsector.ui.SkillTreeSounds;

import java.util.function.Function;

public final class ModificationBay {

    public interface Listener {

        void closed();

        void pressedInside();

        default void wormholeOpened(String message) {
        }

        default void debugActionApplied() {
        }
    }

    static final float PANEL_GAP = 12f;
    static final float SIDE_BY_SIDE_MIN_SCALE = 3f;

    private final CustomPanelAPI hostPanel;
    private final Listener bayListener;
    private Function<Item, String> installedShipLookup;
    private SocketWorkbenchPanel benchPanel;
    private ItemStoragePanel storagePanel;
    private BenchModeSupport.PickerSession modePicker;
    private SocketPanelPlacement drawerPlacement;
    private BenchMode benchMode = BenchMode.HULL_ITEMS;
    private BenchModeSupport.OpenTarget openTarget;
    private boolean hullItemsOnly;
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

    public static ModificationBay openHullItemsOnly(CustomPanelAPI hostPanel, SocketPanelPlacement area, Function<Item, String> installedShipLookup,
                                                    Listener bayListener) {
        ModificationBay bay = new ModificationBay(hostPanel, installedShipLookup, bayListener);
        bay.hullItemsOnly = true;
        bay.layOut(area);
        return bay;
    }

    public static ModificationBay openMode(CustomPanelAPI hostPanel, SocketPanelPlacement area, BenchMode mode, Function<Item, String> installedShipLookup,
                                           Listener bayListener) {
        return openMode(hostPanel, area, mode, installedShipLookup, null, bayListener);
    }

    public static ModificationBay openMode(CustomPanelAPI hostPanel, SocketPanelPlacement area, BenchMode mode, Function<Item, String> installedShipLookup,
                                           BenchModeSupport.OpenTarget openTarget, Listener bayListener) {
        ModificationBay bay = new ModificationBay(hostPanel, installedShipLookup, bayListener);
        bay.benchMode = mode;
        bay.openTarget = openTarget;
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
                false, benchMode, benchListener());
        if (drawer) {
            float inset = SocketWorkbenchPanel.FRAME_INSET;
            drawerPlacement = new SocketPanelPlacement(benchLeft + inset, groupTop + inset, ItemStoragePanel.COMPACT_WIDTH, benchHeight - inset * 2f,
                    false);
        } else {
            openPicker(new SocketPanelPlacement(groupLeft, groupTop, ItemStoragePanel.COMPACT_WIDTH, benchHeight, area.instantly()));
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
                if (modePicker != null) {
                    modePicker.changed(loaded);
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

            @Override
            public BenchModeSupport.OpenTarget openTarget() {
                return hullItemsOnly ? null : openTarget;
            }

            @Override
            public void wormholeOpened(String message) {
                bayListener.wormholeOpened(message);
                close();
            }

            @Override
            public void debugActionApplied() {
                bayListener.debugActionApplied();
                close();
            }
        };
    }

    private boolean pickerOpen() {
        return storagePanel != null || modePicker != null && modePicker.isOpen();
    }

    private void closePicker() {
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (modePicker != null) {
            modePicker.close();
        }
    }

    private void toggleDrawer() {
        if (closing) {
            return;
        }
        if (pickerOpen()) {
            closePicker();
            return;
        }
        SkillTreeSounds.panelOpened();
        openPicker(drawerPlacement);
    }

    private void openPicker(SocketPanelPlacement placement) {
        BenchModeSupport modeSupport = BenchModes.of(benchMode);
        if (modeSupport != null) {
            openModePicker(modeSupport, placement);
        } else {
            openStorage(placement);
        }
    }

    private void openModePicker(BenchModeSupport modeSupport, SocketPanelPlacement placement) {
        if (modePicker == null) {
            modePicker = modeSupport.pickerSession();
        }
        modePicker.open(hostPanel, placement, benchPanel == null ? null : benchPanel.loaded(), new BenchModeSupport.PickerListener() {
            @Override
            public boolean accepting() {
                return benchPanel != null;
            }

            @Override
            public void picked(Item item) {
                benchPanel.load(item);
                if (drawer) {
                    modePicker.close();
                } else {
                    modePicker.refresh();
                }
            }

            @Override
            public void closed() {
                if (!drawer) {
                    close();
                }
            }

            @Override
            public void pressedInside() {
                bayListener.pressedInside();
            }
        });
    }

    private void openStorage(SocketPanelPlacement placement) {
        storagePanel = ItemStoragePanel.openPicker(hostPanel, placement, installedShipLookup, new ItemStoragePanel.Listener() {
            @Override
            public void selected(Item item) {
                if (item == null) {
                    if (benchPanel != null) {
                        benchPanel.load(null);
                    }
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
        }, hullItemsOnly);
        if (benchPanel != null && benchPanel.loaded() != null) {
            storagePanel.setSelected(benchPanel.loaded());
        }
    }

    public void refresh(Function<Item, String> lookup) {
        installedShipLookup = lookup;
        if (storagePanel != null) {
            storagePanel.refresh(lookup);
        }
        if (modePicker != null) {
            modePicker.refresh();
        }
        if (benchPanel != null) {
            benchPanel.refresh();
        }
    }

    public boolean contains(float x, float y) {
        return (benchPanel != null && benchPanel.contains(x, y)) || (storagePanel != null && storagePanel.contains(x, y))
                || (modePicker != null && modePicker.contains(x, y));
    }

    public boolean isOpen() {
        return !closing;
    }

    public BenchMode mode() {
        return benchMode;
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
        if (drawer && pickerOpen()) {
            closePicker();
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
        closePicker();
        if (benchPanel != null) {
            benchPanel.close();
        }
        bayListener.closed();
    }
}
