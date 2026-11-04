package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ForgeChoice;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;

import java.util.Random;

public final class ChartForge {

    static final String FEEDSTOCK_ITEM_ID = ItemDisassembly.FEEDSTOCK_ITEM_ID;
    static final String REFUSAL_PREFIX = "chart.anomaly.derelict_forge.refused.";
    public static final UseVerdict DARK = UseVerdict.refused(REFUSAL_PREFIX + "dark");
    public static final UseVerdict ITEM_TYPE = UseVerdict.refused(REFUSAL_PREFIX + "itemType");
    public static final UseVerdict COST = UseVerdict.refused(REFUSAL_PREFIX + "cost");
    public static final UseVerdict FAILED = UseVerdict.refused(REFUSAL_PREFIX + "failed");

    public record Order(String runId, ForgeSite site, ForgePrograms.Program program) {
    }

    public record Pick(Order order, ForgeChoice choice) {
    }

    private ChartForge() {
    }

    public static ChartRun liveRun(String runId, ForgeSite site) {
        return withSite(ChartRuns.live(runId), site);
    }

    public static ChartRun liveRunFor(SectorEntityToken entity) {
        ForgeSite site = ForgeSite.of(entity);
        return site == null ? null : withSite(ChartRuns.liveOwning(entity), site);
    }

    private static ChartRun withSite(ChartRun run, ForgeSite site) {
        return run != null && site != null && run.anomalyIds().contains(site.anomalyId()) ? run : null;
    }

    public static ForgeRules.Cost costFor(ChartRun run, ForgeSite site, ForgePrograms.Program program) {
        return ForgeRules.costFor(program, site.costMult(run));
    }

    public static boolean canAfford(CargoAPI cargo, CraftingMaterials materials, ForgeRules.Cost cost) {
        return cargo != null && materials != null && cargo.getCommodityQuantity(Commodities.METALS) >= cost.metals()
                && cargo.getCommodityQuantity(Commodities.RARE_METALS) >= cost.rareMetals()
                && materials.count(FEEDSTOCK_ITEM_ID) >= cost.feedstock();
    }

    public static UseVerdict verdict(ChartRun run, ForgeSite site, ForgePrograms.Program program, Item item, CargoAPI cargo,
                                     CraftingMaterials materials) {
        if (run == null || program == null || ForgeStates.runsLeft(run, site) <= 0 || !ForgeStates.programs(run, site).contains(program)) {
            return DARK;
        }
        if (item != null && !program.takes(item.type())) {
            return ITEM_TYPE;
        }
        UseVerdict operationVerdict = program.operation().verdict(item);
        if (!operationVerdict.allowed()) {
            return operationVerdict;
        }
        return canAfford(cargo, materials, costFor(run, site, program)) ? UseVerdict.ALLOW : COST;
    }

    public static boolean run(ChartRun run, ForgeSite site, ForgePrograms.Program program, Item item, ForgeChoice choice, CargoAPI cargo,
                              CraftingMaterials materials, Random random) {
        if (!verdict(run, site, program, item, cargo, materials).allowed()) {
            return false;
        }
        ForgeRules.Cost cost = costFor(run, site, program);
        if (!program.operation().apply(item, choice, random)) {
            return false;
        }
        cargo.removeCommodity(Commodities.METALS, cost.metals());
        cargo.removeCommodity(Commodities.RARE_METALS, cost.rareMetals());
        materials.take(FEEDSTOCK_ITEM_ID, cost.feedstock());
        ForgeStates.spendRun(run, site);
        SocketCustody.markHostsChanged(item);
        return true;
    }
}
