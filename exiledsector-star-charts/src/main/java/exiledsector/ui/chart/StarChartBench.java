package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.charts.StarChartSettings;
import exiledlib.i18n.StyledText;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.StarChartStoragePanel;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class StarChartBench implements BenchModeSupport {

    public static final StarChartBench INSTANCE = new StarChartBench();

    private StarChartBench() {
    }

    @Override
    public BenchMode mode() {
        return BenchMode.STAR_CHARTS;
    }

    @Override
    public boolean enabled() {
        return StarChartSettings.enabled();
    }

    @Override
    public PickerSession pickerSession() {
        return new ChartPicker();
    }

    @Override
    public List<ItemCurrency> currencies() {
        return StarChartCrafting.currencies().stream().sorted(Comparator.comparingDouble(ItemCurrency::dropWeight).reversed()).toList();
    }

    @Override
    public String currencyDescriptionKey(ItemCurrency currency) {
        return StarChartCrafting.currencyDescriptionKey(currency);
    }

    @Override
    public Refusal refusal(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                           Set<CraftingWard> wards) {
        StarChartCrafting.Refusal refusal = StarChartCrafting.refusal(currency, loaded, store, materials, allowedUniques, wards);
        boolean checksWards = refusal != StarChartCrafting.Refusal.NO_CHART && refusal != StarChartCrafting.Refusal.NONE_OWNED;
        return new Refusal(refusal == StarChartCrafting.Refusal.NONE, checksWards, refusal.noticeKey());
    }

    @Override
    public Use use(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
                   Predicate<ItemDefinition> allowedUniques, Set<CraftingWard> wards, String nameBefore) {
        StarChartCrafting.Used used = StarChartCrafting.use(currency, loaded, store, materials, random, allowedUniques, wards);
        return used == null ? null : new Use(used.result(), used.selectedAfter(), StarChartCrafting.usedNotice(currency, nameBefore, used));
    }

    @Override
    public String cannotUseNoticeKey() {
        return StarChartCrafting.Refusal.CANNOT_USE.noticeKey();
    }

    @Override
    public List<StyledText> detailLines(Item item) {
        return item instanceof StarChart chart ? chart.lootTotalLines() : List.of();
    }

    @Override
    public OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded) {
        WormholeOpenCheck.Blocker blocker = WormholeOpenCheck.current(sector, store, loaded);
        return new OpenCheck(blocker.allowsOpening(), blocker.reasonKey());
    }

    @Override
    public int openEntries() {
        return StarChartSettings.wormholeEntries();
    }

    @Override
    public OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random) {
        StarChartOpening.Outcome outcome = StarChartOpening.open(StarChartOpening.FROM_STORE, sector, store, loaded, entries, random);
        return new OpenOutcome(outcome.opened(), outcome.messageKey());
    }

    private static final class ChartPicker implements PickerSession {

        private final StarChartBrowser browser = new StarChartBrowser();
        private StarChartStoragePanel storagePanel;

        @Override
        public void open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Item loaded, PickerListener listener) {
            int moved = StarChartBrowser.prepare(Global.getSector(), browser);
            if (loaded != null) {
                browser.select(loaded);
            }
            storagePanel = StarChartStoragePanel.open(hostPanel, placement, browser, new StarChartStoragePanel.Listener() {
                @Override
                public void chartClicked(Item chart) {
                    if (!listener.accepting()) {
                        return;
                    }
                    browser.select(chart);
                    listener.picked(chart);
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
            }, StarChartBrowser.movedNotice(moved));
        }

        @Override
        public boolean isOpen() {
            return storagePanel != null;
        }

        @Override
        public void changed(Item loaded) {
            if (storagePanel != null) {
                ItemStore chartStore = ItemStore.starChartsIfCreated();
                browser.reload(chartStore == null ? List.of() : chartStore.owned());
                browser.select(loaded);
                storagePanel.refresh();
            }
        }

        @Override
        public void refresh() {
            if (storagePanel != null) {
                storagePanel.refresh();
            }
        }

        @Override
        public void swapOut() {
            if (storagePanel != null) {
                StarChartStoragePanel swapped = storagePanel;
                storagePanel = null;
                swapped.swapOut();
            }
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
}
