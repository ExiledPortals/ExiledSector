package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ForgeChoice;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;

import java.util.Random;

public final class ChartForge {

    static final String FEEDSTOCK_ITEM_ID = ItemDisassembly.PARTS_ITEM_ID;
    static final String REFUSAL_PREFIX = "chart.forge.refused.";
    public static final UseVerdict DARK = UseVerdict.refused(REFUSAL_PREFIX + "dark");
    public static final UseVerdict ITEM_TYPE = UseVerdict.refused(REFUSAL_PREFIX + "itemType");
    public static final UseVerdict COST = UseVerdict.refused(REFUSAL_PREFIX + "cost");
    public static final UseVerdict FAILED = UseVerdict.refused(REFUSAL_PREFIX + "failed");

    public record Order(String runId, ForgePrograms.Program program) {
    }

    public record Pick(Order order, ForgeChoice choice) {
    }

    private ChartForge() {
    }

    public static ChartRun liveRun(String runId) {
        ChartRun run = ChartRuns.currentOrNull();
        return run != null && run.runState().isLive() && run.runId().equals(runId) && run.anomalyIds().contains(DerelictForge.ID) ? run : null;
    }

    public static ChartRun liveRunFor(SectorEntityToken forge) {
        ChartRun run = ChartRuns.currentOrNull();
        return run != null && DerelictForge.isForge(forge) && ChartIds.belongsToRun(forge, run.runId()) ? liveRun(run.runId()) : null;
    }

    public static ForgeRules.Cost costFor(ChartRun run, ForgePrograms.Program program) {
        return ForgeRules.costFor(program, run.tier(), ChartRunEffects.contextOf(run).anomalyMult(DerelictForge.COST_KEY));
    }

    public static boolean canAfford(CargoAPI cargo, CraftingMaterials materials, ForgeRules.Cost cost) {
        return cargo != null && materials != null && cargo.getCommodityQuantity(Commodities.METALS) >= cost.metals()
                && cargo.getCommodityQuantity(Commodities.RARE_METALS) >= cost.rareMetals()
                && materials.count(FEEDSTOCK_ITEM_ID) >= cost.feedstock();
    }

    public static UseVerdict verdict(ChartRun run, ForgePrograms.Program program, Item item, CargoAPI cargo, CraftingMaterials materials) {
        if (run == null || program == null || ForgeStates.runsLeft(run) <= 0 || !ForgeStates.programs(run).contains(program)) {
            return DARK;
        }
        if (item != null && !program.takes(item.type())) {
            return ITEM_TYPE;
        }
        UseVerdict operationVerdict = program.operation().verdict(item);
        if (!operationVerdict.allowed()) {
            return operationVerdict;
        }
        return canAfford(cargo, materials, costFor(run, program)) ? UseVerdict.ALLOWED : COST;
    }

    public static boolean run(ChartRun run, ForgePrograms.Program program, Item item, ForgeChoice choice, CargoAPI cargo,
                              CraftingMaterials materials, Random random) {
        if (!verdict(run, program, item, cargo, materials).allowed()) {
            return false;
        }
        ForgeRules.Cost cost = costFor(run, program);
        if (!program.operation().apply(item, choice, random)) {
            return false;
        }
        cargo.removeCommodity(Commodities.METALS, cost.metals());
        cargo.removeCommodity(Commodities.RARE_METALS, cost.rareMetals());
        materials.take(FEEDSTOCK_ITEM_ID, cost.feedstock());
        ForgeStates.spendRun(run);
        SocketCustody.markHostsChanged(item);
        return true;
    }
}
