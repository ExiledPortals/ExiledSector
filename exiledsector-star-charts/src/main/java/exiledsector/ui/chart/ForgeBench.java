package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.PlayerFleet;
import exiledlib.i18n.StyledText;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.forge.ChartForge;
import exiledsector.charts.anomaly.forge.ForgeTexts;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.ForgeChoice;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.UseVerdict;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class ForgeBench implements BenchModeSupport {

    public static final ForgeBench INSTANCE = new ForgeBench();
    static final String RAN_PREFIX = "chart.forge.ran.";
    private static final OpenTexts OPEN_TEXTS = new OpenTexts("chart.forge.bench.run", "chart.forge.bench.run.tooltip", "chart.forge.bench.confirm.title",
            "chart.forge.bench.confirm.body", null, "chart.forge.bench.confirm.yes", "chart.forge.bench.confirm.no");

    private ForgeBench() {
    }

    @Override
    public BenchMode mode() {
        return BenchMode.FORGE;
    }

    @Override
    public ItemType itemType() {
        return null;
    }

    @Override
    public boolean accepts(Item item) {
        return item != null && item.type() == ItemType.HULL_ITEM;
    }

    @Override
    public boolean usesInhibitors() {
        return false;
    }

    @Override
    public boolean enabled() {
        return StarChartSettings.enabled();
    }

    @Override
    public ItemStore store() {
        return ItemStore.get();
    }

    @Override
    public String titleKey() {
        return "chart.forge.bench.title";
    }

    @Override
    public String emptyKey() {
        return "chart.forge.bench.empty";
    }

    @Override
    public String useHintKey() {
        return "chart.forge.bench.empty";
    }

    @Override
    public PickerSession pickerSession() {
        return null;
    }

    @Override
    public List<ItemCurrency> currencies() {
        return List.of();
    }

    @Override
    public String currencyDescriptionKey(ItemCurrency currency) {
        return "chart.forge.bench.empty";
    }

    @Override
    public Refusal refusal(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                           Set<CraftingWard> wards) {
        return new Refusal(cannotUse(), false);
    }

    @Override
    public Use use(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
                   Predicate<ItemDefinition> allowedUniques, Set<CraftingWard> wards, String nameBefore) {
        return null;
    }

    @Override
    public UseVerdict cannotUse() {
        return ChartForge.DARK;
    }

    @Override
    public List<StyledText> detailLines(Item item) {
        return List.of();
    }

    @Override
    public OpenTexts openTexts() {
        return OPEN_TEXTS;
    }

    @Override
    public OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded) {
        return new OpenCheck(false, ChartForge.DARK.refusalKey());
    }

    @Override
    public OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded, Object target) {
        if (!(target instanceof ChartForge.Order order)) {
            return openCheck(sector, store, loaded);
        }
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        UseVerdict verdict = ChartForge.verdict(ChartForge.liveRun(order.runId()), order.program(), loaded, cargo, materials(cargo));
        return new OpenCheck(verdict.allowed(), verdict.refusalKey());
    }

    @Override
    public List<String> confirmLines(Item loaded, OpenTarget target) {
        if (target == null || !(target.target() instanceof ChartForge.Order order)) {
            return List.of();
        }
        ChartRun run = ChartForge.liveRun(order.runId());
        if (run == null) {
            return List.of(ForgeTexts.programDescription(order.program()));
        }
        List<String> lines = new ArrayList<>(List.of(ForgeTexts.programDescription(order.program())));
        if (order.program().operation().picksAChoice()) {
            lines.add(ForgeTexts.choicePrompt(order.program().operation()));
        }
        lines.add(ForgeTexts.costLine(ChartForge.costFor(run, order.program())));
        return lines;
    }

    @Override
    public List<OpenChoice> openChoices(Item loaded, OpenTarget target) {
        if (loaded == null || target == null || !(target.target() instanceof ChartForge.Order order) || !order.program().operation().picksAChoice()) {
            return List.of();
        }
        return order.program().operation().choices(loaded).stream()
                .map(choice -> new OpenChoice(ForgeTexts.choiceLabel(loaded, choice, order.program().operation()), null,
                        new ChartForge.Pick(order, choice)))
                .toList();
    }

    private static CraftingMaterials materials(CargoAPI cargo) {
        return cargo == null ? null : CraftingMaterials.forPlayer(cargo);
    }

    @Override
    public void playOpened() {
        SkillTreeSounds.crafted();
    }

    @Override
    public int openEntries() {
        return 0;
    }

    @Override
    public OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random) {
        return new OpenOutcome(false, ChartForge.DARK.refusalKey());
    }

    @Override
    public OpenOutcome openAt(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random, Object target) {
        ChartForge.Pick pick = target instanceof ChartForge.Pick chosen ? chosen
                : target instanceof ChartForge.Order order ? new ChartForge.Pick(order, ForgeChoice.WHOLE_ITEM) : null;
        if (pick == null) {
            return open(sector, store, loaded, entries, random);
        }
        ChartRun run = ChartForge.liveRun(pick.order().runId());
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        if (ChartForge.run(run, pick.order().program(), loaded, pick.choice(), cargo, materials(cargo), random)) {
            return new OpenOutcome(true, RAN_PREFIX + pick.order().program().operation().id());
        }
        UseVerdict verdict = ChartForge.verdict(run, pick.order().program(), loaded, cargo, materials(cargo));
        return new OpenOutcome(false, verdict.allowed() ? ChartForge.FAILED.refusalKey() : verdict.refusalKey());
    }
}
