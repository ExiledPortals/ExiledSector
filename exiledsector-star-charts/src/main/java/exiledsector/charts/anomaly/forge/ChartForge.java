package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.UseVerdict;
import exiledsector.items.crafting.BiomeCraft;
import exiledsector.items.crafting.ForgeChoice;
import exiledsector.items.crafting.ForgeInfusions;
import exiledsector.items.crafting.ItemDisassembly;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.custody.SocketCustody;
import exiledsector.items.modifier.RolledModifier;

import java.util.List;
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
        return site == null ? null : site.liveRunFor(entity);
    }

    private static ChartRun withSite(ChartRun run, ForgeSite site) {
        return run != null && site != null && run.anomalyIds().contains(site.anomalyId()) ? run : null;
    }

    public static ForgeRules.Cost costFor(ChartRun run, ForgeSite site, ForgePrograms.Program program) {
        return ForgeRules.costFor(program, site.costMult(run));
    }

    static List<String> infusedFeedstockIds() {
        return BiomeCraft.feedstockIds();
    }

    public static boolean canAfford(CraftingMaterials materials, ForgeRules.Cost cost) {
        return materials != null && materials.count(FEEDSTOCK_ITEM_ID) >= cost.feedstock()
                && infusedFeedstockIds().stream().allMatch(feedstockId -> materials.count(feedstockId) >= cost.infusedFeedstockEach());
    }

    static ForgeInfusions.Source infusionFor(ChartRun run, ForgeSite site) {
        String biomeId = ForgeStates.infusionBiome(run, site);
        return biomeId == null ? ForgeInfusions.Source.NONE : new BiomeInfusion(biomeId);
    }

    public record BiomeInfusion(String biomeId) implements ForgeInfusions.Source {

        @Override
        public String modifierFor(ItemDefinition definition) {
            return BiomeModifiers.modifierFor(biomeId, definition.kind());
        }

        @Override
        public boolean hasRoomFor(List<RolledModifier> present, String modifierId) {
            return BiomeModifiers.hasRoomFor(present, modifierId);
        }
    }

    public static UseVerdict verdict(ChartRun run, ForgeSite site, ForgePrograms.Program program, Item item, CraftingMaterials materials) {
        if (run == null || program == null || ForgeStates.runsLeft(run, site) <= 0 || !ForgeStates.programs(run, site).contains(program)) {
            return DARK;
        }
        if (item != null && !program.takes(item.type())) {
            return ITEM_TYPE;
        }
        UseVerdict operationVerdict = program.operation().verdict(item, infusionFor(run, site));
        if (!operationVerdict.allowed()) {
            return operationVerdict;
        }
        return canAfford(materials, costFor(run, site, program)) ? UseVerdict.ALLOW : COST;
    }

    public static boolean run(ChartRun run, ForgeSite site, ForgePrograms.Program program, Item item, ForgeChoice choice, CraftingMaterials materials,
                              Random random) {
        if (!verdict(run, site, program, item, materials).allowed()) {
            return false;
        }
        ForgeRules.Cost cost = costFor(run, site, program);
        if (!program.operation().apply(item, choice, infusionFor(run, site), random)) {
            return false;
        }
        materials.take(FEEDSTOCK_ITEM_ID, cost.feedstock());
        infusedFeedstockIds().forEach(feedstockId -> materials.take(feedstockId, cost.infusedFeedstockEach()));
        ForgeStates.spendRun(run, site);
        SocketCustody.markHostsChanged(item);
        return true;
    }
}
