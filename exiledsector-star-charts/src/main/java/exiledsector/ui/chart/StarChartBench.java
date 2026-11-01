package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasEntry;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.wormhole.StarChartOpening;
import exiledsector.charts.wormhole.WormholeOpenCheck;
import exiledsector.i18n.StyledText;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartCrafting;
import exiledsector.items.UseVerdict;
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
    static final String NO_SYSTEM_KEY = "chart.atlas.blocked.no_system";
    private static final DebugAction COMPLETE_TARGET = new DebugAction("chart.atlas.debug.complete", "chart.atlas.debug.complete.tooltip");
    private static final OpenTexts OPEN_TEXTS = new OpenTexts("chart.ui.open.button", "chart.ui.open.tooltip", "chart.ui.open.confirm.title",
            "chart.ui.open.confirm.body", "chart.ui.open.confirm.entries", "chart.ui.open.confirm.yes", "chart.ui.open.confirm.no");

    private StarChartBench() {
    }

    @Override
    public BenchMode mode() {
        return BenchMode.STAR_CHARTS;
    }

    @Override
    public ItemType itemType() {
        return ItemType.STAR_CHART;
    }

    @Override
    public ItemStore store() {
        return ItemStore.starChartsIfCreated();
    }

    @Override
    public String titleKey() {
        return "chart.ui.bench.title";
    }

    @Override
    public String emptyKey() {
        return "chart.ui.bench.empty";
    }

    @Override
    public String useHintKey() {
        return "chart.ui.currency.use";
    }

    @Override
    public OpenTexts openTexts() {
        return OPEN_TEXTS;
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
        UseVerdict verdict = StarChartCrafting.refusal(currency, loaded, store, materials, allowedUniques, wards);
        return new Refusal(verdict, !verdict.equals(StarChartCrafting.NO_CHART) && !verdict.equals(StarChartCrafting.NONE_OWNED));
    }

    @Override
    public Use use(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
                   Predicate<ItemDefinition> allowedUniques, Set<CraftingWard> wards, String nameBefore) {
        StarChartCrafting.Used used = StarChartCrafting.use(currency, loaded, store, materials, random, allowedUniques, wards);
        return used == null ? null : new Use(used.result(), used.selectedAfter(), StarChartCrafting.usedNotice(currency, nameBefore, used));
    }

    @Override
    public UseVerdict cannotUse() {
        return StarChartCrafting.CANNOT_USE;
    }

    @Override
    public List<StyledText> detailLines(Item item) {
        return item instanceof StarChart chart ? chart.lootTotalLines() : List.of();
    }

    @Override
    public OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded) {
        return new OpenCheck(false, NO_SYSTEM_KEY);
    }

    @Override
    public OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded, Object target) {
        AtlasCell cell = atlasCell(target);
        if (cell == null || sector == null) {
            return openCheck(sector, store, loaded);
        }
        WormholeOpenCheck.Blocker wormhole = WormholeOpenCheck.replacingRun(sector, store, loaded);
        if (!wormhole.allowsOpening()) {
            return new OpenCheck(false, wormhole.reasonKey());
        }
        AtlasOpening.Blocker blocker = AtlasOpening.check(AtlasRuntime.generator(sector), AtlasState.get(sector), cell,
                loaded instanceof StarChart chart ? chart : null);
        return new OpenCheck(blocker.allowsOpening(), blocker.reasonKey());
    }

    @Override
    public int openEntries() {
        return StarChartSettings.wormholeEntries();
    }

    @Override
    public OpenOutcome openAt(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random, Object target) {
        AtlasCell cell = atlasCell(target);
        if (cell == null) {
            return open(sector, store, loaded, entries, random);
        }
        AtlasRunChoice choice = target instanceof AtlasEntry entry ? entry.choice() : null;
        StarChartOpening.Outcome outcome = StarChartOpening.openReplacingRun(
                (openSector, allowed, openRandom, openStore, chart) -> ChartRunActions.openFromAtlas(openSector, allowed, openRandom, openStore, chart, cell,
                        choice),
                sector, store, loaded, entries, random);
        return new OpenOutcome(outcome.opened(), outcome.messageKey());
    }

    @Override
    public DebugAction debugAction() {
        return StarChartSettings.debugTools() ? COMPLETE_TARGET : null;
    }

    @Override
    public void runDebugAction(SectorAPI sector, Item loaded, Object target) {
        if (sector != null && target instanceof AtlasCell cell) {
            int tier = loaded instanceof StarChart chart ? StarChart.clampTier(chart.tier()) : StarChart.LOWEST_TIER;
            AtlasDevTools.complete(AtlasRuntime.generator(sector), AtlasState.get(sector), cell, tier);
        }
    }

    @Override
    public OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random) {
        return new OpenOutcome(false, NO_SYSTEM_KEY);
    }

    private static AtlasCell atlasCell(Object target) {
        if (target instanceof AtlasEntry entry) {
            return entry.cell();
        }
        return target instanceof AtlasCell cell ? cell : null;
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

                @Override
                public void disassembled() {
                    listener.storeChanged();
                }
            }, StarChartBrowser.movedNotice(moved));
        }

        @Override
        public boolean escape() {
            return storagePanel != null && storagePanel.escape();
        }

        @Override
        public boolean isOpen() {
            return storagePanel != null;
        }

        @Override
        public void changed(Item loaded) {
            if (storagePanel != null) {
                browser.reload(ItemStore.ownedStarCharts());
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
