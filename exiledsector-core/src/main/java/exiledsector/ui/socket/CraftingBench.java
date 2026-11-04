package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.Item;
import exiledsector.ui.SkillTreeSounds;

import java.util.function.Function;

public final class CraftingBench {

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
    private CraftingBenchPanel benchPanel;
    private BenchModeSupport.PickerSession picker;
    private SocketPanelPlacement drawerPlacement;
    private BenchSideBySide sideBySide;
    private BenchMode benchMode = BenchMode.HULL_ITEMS;
    private BenchModeSupport.OpenTarget openTarget;
    private boolean hullItemsOnly;
    private boolean drawer;
    private boolean closing;
    private Item craftedItem;

    private CraftingBench(CustomPanelAPI hostPanel, Function<Item, String> installedShipLookup, Listener bayListener) {
        this.hostPanel = hostPanel;
        this.installedShipLookup = installedShipLookup;
        this.bayListener = bayListener;
    }

    public static CraftingBench open(CustomPanelAPI hostPanel, SocketPanelPlacement area, Function<Item, String> installedShipLookup,
                                       Listener bayListener) {
        CraftingBench bay = new CraftingBench(hostPanel, installedShipLookup, bayListener);
        bay.layOut(area);
        return bay;
    }

    public static CraftingBench openHullItemsOnly(CustomPanelAPI hostPanel, SocketPanelPlacement area, Function<Item, String> installedShipLookup,
                                                    Listener bayListener) {
        CraftingBench bay = new CraftingBench(hostPanel, installedShipLookup, bayListener);
        bay.hullItemsOnly = true;
        bay.layOut(area);
        return bay;
    }

    public static CraftingBench openMode(CustomPanelAPI hostPanel, SocketPanelPlacement area, BenchMode mode, Function<Item, String> installedShipLookup,
                                           Listener bayListener) {
        return openMode(hostPanel, area, mode, installedShipLookup, null, bayListener);
    }

    public static CraftingBench openMode(CustomPanelAPI hostPanel, SocketPanelPlacement area, BenchMode mode, Function<Item, String> installedShipLookup,
                                           BenchModeSupport.OpenTarget openTarget, Listener bayListener) {
        CraftingBench bay = new CraftingBench(hostPanel, installedShipLookup, bayListener);
        bay.benchMode = mode;
        bay.openTarget = openTarget;
        bay.layOut(area);
        return bay;
    }

    static boolean fitsSideBySide(float areaWidth, float areaHeight) {
        return CraftingBenchPanel.scaleFor(areaWidth - ItemStoragePanel.COMPACT_WIDTH - PANEL_GAP, areaHeight) >= SIDE_BY_SIDE_MIN_SCALE;
    }

    private void layOut(SocketPanelPlacement area) {
        drawer = !fitsSideBySide(area.panelWidth(), area.panelHeight());
        if (drawer) {
            float scale = CraftingBenchPanel.scaleFor(area.panelWidth(), area.panelHeight());
            float benchWidth = CraftingBenchPanel.sizeFor(scale, BenchScene.ART_WIDTH);
            float benchHeight = CraftingBenchPanel.sizeFor(scale, BenchScene.ART_HEIGHT);
            float benchLeft = area.panelLeft() + (area.panelWidth() - benchWidth) / 2f;
            float benchTop = area.panelTop() + (area.panelHeight() - benchHeight) / 2f;
            benchPanel = CraftingBenchPanel.open(hostPanel, new SocketPanelPlacement(benchLeft, benchTop, benchWidth, benchHeight, area.instantly()),
                    false, benchMode, benchListener());
            float inset = CraftingBenchPanel.FRAME_INSET;
            drawerPlacement = new SocketPanelPlacement(benchLeft + inset, benchTop + inset, ItemStoragePanel.COMPACT_WIDTH, benchHeight - inset * 2f,
                    false);
            return;
        }
        sideBySide = BenchSideBySide.in(area);
        BenchSideBySide.Placed placed = sideBySide.placed(false);
        benchPanel = CraftingBenchPanel.open(hostPanel, new SocketPanelPlacement(placed.benchLeft(), placed.benchTop(), placed.benchWidth(),
                placed.benchHeight(), area.instantly()), false, benchMode, benchListener());
        openPicker(new SocketPanelPlacement(placed.storageLeft(), sideBySide.groupTop(), ItemStoragePanel.COMPACT_WIDTH, sideBySide.groupHeight(),
                area.instantly()).withAdvancedSearchRoom(advancedSearchRoom()));
    }

    private AdvancedSearchRoom advancedSearchRoom() {
        return new AdvancedSearchRoom() {
            @Override
            public FreeArea open() {
                arrange(true);
                return sideBySide.searchArea();
            }

            @Override
            public void closed() {
                arrange(false);
            }
        };
    }

    private void arrange(boolean searchOpen) {
        if (closing) {
            return;
        }
        BenchSideBySide.Placed placed = sideBySide.placed(searchOpen);
        if (benchPanel != null) {
            benchPanel.rescale(placed.benchLeft(), placed.benchTop(), placed.benchScale());
        }
        if (picker != null) {
            picker.moveTo(placed.storageLeft(), sideBySide.groupTop());
        }
    }

    private CraftingBenchPanel.Listener benchListener() {
        return new CraftingBenchPanel.Listener() {
            @Override
            public void changed(Item loaded) {
                if (loaded != null) {
                    craftedItem = loaded;
                }
                if (picker != null) {
                    picker.changed(loaded);
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
        return picker != null && picker.isOpen();
    }

    private void closePicker() {
        if (picker != null) {
            picker.close();
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
        if (picker == null) {
            picker = BenchModes.of(benchMode).pickerSession();
        }
        if (picker == null) {
            picker = new ItemStoragePicker(item -> installedShipLookup.apply(item), hullItemsOnly || benchMode != BenchMode.HULL_ITEMS);
        }
        picker.open(hostPanel, placement, benchPanel == null ? null : benchPanel.loaded(), new BenchModeSupport.PickerListener() {
            @Override
            public boolean accepting() {
                return benchPanel != null;
            }

            @Override
            public void picked(Item item) {
                benchPanel.load(item);
                if (drawer && item != null) {
                    picker.close();
                } else {
                    picker.changed(item);
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

            @Override
            public void storeChanged() {
                if (benchPanel != null) {
                    benchPanel.refresh();
                }
            }
        });
    }

    public void refresh(Function<Item, String> lookup) {
        installedShipLookup = lookup;
        if (picker != null) {
            picker.refresh();
        }
        if (benchPanel != null) {
            benchPanel.refresh();
        }
    }

    public boolean contains(float x, float y) {
        return (benchPanel != null && benchPanel.contains(x, y)) || (picker != null && picker.contains(x, y));
    }

    public boolean isOpen() {
        return !closing;
    }

    public BenchMode mode() {
        return benchMode;
    }

    public Item craftedItem() {
        return craftedItem;
    }

    public boolean escape() {
        if (closing) {
            return false;
        }
        if (benchPanel != null && benchPanel.escape()) {
            return true;
        }
        if (picker != null && picker.escape()) {
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
