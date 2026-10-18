package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.chart.StarChartBrowser;

import java.util.List;
import java.util.function.Function;

public final class ModificationBay {

    public interface Listener {

        void closed();

        void pressedInside();

        default void wormholeOpened(String message) {
        }
    }

    static final float PANEL_GAP = 12f;
    static final float SIDE_BY_SIDE_MIN_SCALE = 3f;

    private final CustomPanelAPI hostPanel;
    private final Listener bayListener;
    private final StarChartBrowser chartBrowser = new StarChartBrowser();
    private Function<Item, String> installedShipLookup;
    private SocketWorkbenchPanel benchPanel;
    private ItemStoragePanel storagePanel;
    private StarChartStoragePanel chartStoragePanel;
    private SocketPanelPlacement drawerPlacement;
    private SocketPanelPlacement sidePlacement;
    private BenchMode benchMode = BenchMode.HULL_ITEMS;
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
            sidePlacement = new SocketPanelPlacement(groupLeft, groupTop, ItemStoragePanel.COMPACT_WIDTH, benchHeight, area.instantly());
            openPicker(sidePlacement);
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
                if (chartStoragePanel != null) {
                    ItemStore chartStore = ItemStore.starChartsIfCreated();
                    chartBrowser.reload(chartStore == null ? List.of() : chartStore.owned());
                    chartBrowser.select(loaded);
                    chartStoragePanel.refresh();
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
            public boolean offersModeSwitch() {
                return !hullItemsOnly;
            }

            @Override
            public void modeChanged(BenchMode mode) {
                switchPicker(mode);
            }

            @Override
            public boolean offersWormholeOpening() {
                return !hullItemsOnly;
            }

            @Override
            public void wormholeOpened(String message) {
                bayListener.wormholeOpened(message);
                close();
            }
        };
    }

    private void switchPicker(BenchMode mode) {
        benchMode = mode;
        swapOutPickers();
        if (!drawer && !closing) {
            openPicker(new SocketPanelPlacement(sidePlacement.panelLeft(), sidePlacement.panelTop(), sidePlacement.panelWidth(),
                    sidePlacement.panelHeight(), true));
        }
    }

    private void swapOutPickers() {
        if (storagePanel != null) {
            ItemStoragePanel swapped = storagePanel;
            storagePanel = null;
            swapped.swapOut();
        }
        if (chartStoragePanel != null) {
            StarChartStoragePanel swapped = chartStoragePanel;
            chartStoragePanel = null;
            swapped.swapOut();
        }
    }

    private boolean pickerOpen() {
        return storagePanel != null || chartStoragePanel != null;
    }

    private void closePicker() {
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (chartStoragePanel != null) {
            chartStoragePanel.close();
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
        if (benchMode == BenchMode.STAR_CHARTS) {
            openChartStorage(placement);
        } else {
            openStorage(placement);
        }
    }

    private void openChartStorage(SocketPanelPlacement placement) {
        int moved = StarChartBrowser.prepare(Global.getSector(), chartBrowser);
        if (benchPanel != null && benchPanel.loaded() != null) {
            chartBrowser.select(benchPanel.loaded());
        }
        chartStoragePanel = StarChartStoragePanel.open(hostPanel, placement, chartBrowser, new StarChartStoragePanel.Listener() {
            @Override
            public void chartClicked(Item chart) {
                if (benchPanel == null) {
                    return;
                }
                chartBrowser.select(chart);
                benchPanel.load(chart);
                if (drawer && chartStoragePanel != null) {
                    chartStoragePanel.close();
                } else if (chartStoragePanel != null) {
                    chartStoragePanel.refresh();
                }
            }

            @Override
            public void closed() {
                chartStoragePanel = null;
                if (!drawer) {
                    close();
                }
            }

            @Override
            public void pressedInside() {
                bayListener.pressedInside();
            }
        }, StarChartBrowser.movedNotice(moved));
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
        if (chartStoragePanel != null) {
            chartStoragePanel.refresh();
        }
        if (benchPanel != null) {
            benchPanel.refresh();
        }
    }

    public boolean contains(float x, float y) {
        return (benchPanel != null && benchPanel.contains(x, y)) || (storagePanel != null && storagePanel.contains(x, y))
                || (chartStoragePanel != null && chartStoragePanel.contains(x, y));
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
