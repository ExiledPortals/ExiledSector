package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.CraftingDrops;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ExcavationLoot {

    private ExcavationLoot() {
    }

    public static CargoAPI raidCargo(ChartRun run, float share, Random random) {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        float expectedInhibitors = expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, context, share);
        tagInhibitors(expectedInhibitors, bandShift(context, run.tier()), random).forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        return cargo;
    }

    static float bandShift(ChartGenContext context, int chartTier) {
        return ChartGenContext.orNone(context).craftingBandShift(chartTier);
    }

    static float quantityMult(ChartGenContext context) {
        return ChartGenContext.orNone(context).anomalyMult(ExcavationSites.LOOT_KEY);
    }

    static float expectedInhibitors(float perShaft, ChartGenContext context, float share) {
        return perShaft * Math.max(0f, quantityMult(context)) * Math.max(0f, share);
    }

    static Map<String, Integer> tagInhibitors(float expectedCount, float bandShift, Random random) {
        return CraftingDrops.roll(expectedCount, ExcavationLoot::isTagInhibitor, bandShift, random);
    }

    static boolean isTagInhibitor(String itemId) {
        CraftingInhibitor inhibitor = CraftingInhibitor.byItemId(itemId);
        return inhibitor != null && inhibitor.inhibitsTag();
    }

    public static ItemDrops.SiteLoot fleetItems(CampaignFleetAPI fleet, ChartGenContext context, int chartTier, float share, Random random) {
        if (!ExcavationFleets.isExcavationFleet(fleet)) {
            return null;
        }
        Map<String, Integer> inhibitors = tagInhibitors(expectedInhibitors(ExcavationRules.INHIBITORS_PER_GUARDIAN_FLEET, context, share),
                bandShift(context, chartTier), random);
        return inhibitors.isEmpty() ? null : new ItemDrops.SiteLoot(List.of(), List.of(), inhibitors);
    }
}
