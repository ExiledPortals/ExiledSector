package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.PlayerFleet;
import exiledsector.i18n.StyledText;
import exiledsector.charts.anomaly.forge.ChartForge;
import exiledsector.charts.anomaly.forge.ForgeSite;
import exiledsector.charts.anomaly.forge.ForgeTexts;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ForgeChoice;
import exiledsector.items.Item;
import exiledsector.items.CraftingMaterialFamily;
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

public final class ForgeCraftingBench implements BenchModeSupport {

    public static final ForgeCraftingBench DERELICT_FORGE = new ForgeCraftingBench(ForgeSite.DERELICT_FORGE);
    public static final ForgeCraftingBench ABYSSAL_RESEARCH_COMPLEX = new ForgeCraftingBench(ForgeSite.ABYSSAL_RESEARCH_COMPLEX);
    static final String RAN_PREFIX = "chart.anomaly.derelict_forge.ran.";
    private static final OpenTexts OPEN_TEXTS = new OpenTexts("chart.anomaly.derelict_forge.bench.run", "chart.anomaly.derelict_forge.bench.run.tooltip", "chart.anomaly.derelict_forge.bench.confirm.title",
            "chart.anomaly.derelict_forge.bench.confirm.body", null, "chart.anomaly.derelict_forge.bench.confirm.yes", "chart.anomaly.derelict_forge.bench.confirm.no");

    private final ForgeSite forgeSite;

    private ForgeCraftingBench(ForgeSite forgeSite) {
        this.forgeSite = forgeSite;
    }

    @Override
    public BenchMode mode() {
        return forgeSite.benchMode();
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
    public ItemStore store() {
        return ItemStore.get();
    }

    @Override
    public String titleKey() {
        return forgeSite.textKey("bench.title");
    }

    @Override
    public String emptyKey() {
        return forgeSite.textKey("bench.empty");
    }

    @Override
    public String useHintKey() {
        return forgeSite.textKey("bench.empty");
    }

    @Override
    public PickerSession pickerSession() {
        return null;
    }

    @Override
    public List<CraftingMaterialFamily> craftingItems() {
        return List.of();
    }

    @Override
    public String craftingItemDescriptionKey(CraftingMaterialFamily craftingItem) {
        return "chart.anomaly.derelict_forge.bench.empty";
    }

    @Override
    public Refusal refusal(CraftingMaterialFamily craftingItem, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                           Set<CraftingInhibitor> inhibitors) {
        return new Refusal(cannotUse(), false);
    }

    @Override
    public Use use(CraftingMaterialFamily craftingItem, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
                   Predicate<ItemDefinition> allowedUniques, Set<CraftingInhibitor> inhibitors, String nameBefore) {
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
        UseVerdict verdict = ChartForge.verdict(ChartForge.liveRun(order.runId(), order.site()), order.site(), order.program(), loaded, cargo,
                materials(cargo));
        return new OpenCheck(verdict.allowed(), verdict.refusalKey());
    }

    @Override
    public List<String> confirmLines(Item loaded, OpenTarget target) {
        if (target == null || !(target.target() instanceof ChartForge.Order order)) {
            return List.of();
        }
        ChartRun run = ChartForge.liveRun(order.runId(), order.site());
        if (run == null) {
            return List.of(ForgeTexts.programDescription(order.program()));
        }
        List<String> lines = new ArrayList<>(List.of(ForgeTexts.programDescription(order.program())));
        if (order.program().operation().picksAChoice()) {
            lines.add(ForgeTexts.choicePrompt(order.program().operation()));
        }
        lines.add(ForgeTexts.costLine(ChartForge.costFor(run, order.site(), order.program())));
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
    public int openEntries(Item loaded) {
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
        ForgeSite site = pick.order().site();
        ChartRun run = ChartForge.liveRun(pick.order().runId(), site);
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        if (ChartForge.run(run, site, pick.order().program(), loaded, pick.choice(), cargo, materials(cargo), random)) {
            return new OpenOutcome(true, RAN_PREFIX + pick.order().program().operation().id());
        }
        UseVerdict verdict = ChartForge.verdict(run, site, pick.order().program(), loaded, cargo, materials(cargo));
        return new OpenOutcome(false, verdict.allowed() ? ChartForge.FAILED.refusalKey() : verdict.refusalKey());
    }
}
