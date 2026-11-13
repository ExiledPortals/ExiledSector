package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ExcavationLoot {

    private ExcavationLoot() {
    }

    public static CargoAPI raidCargo(ChartRun run, float share, Random random) {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        float expectedInhibitors = expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, ChartRunEffects.contextOf(run), share);
        tagInhibitors(expectedInhibitors, random).forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        return cargo;
    }

    static float quantityMult(ChartGenContext context) {
        return (context == null ? ChartGenContext.none() : context).anomalyMult(ExcavationSites.LOOT_KEY);
    }

    static float expectedInhibitors(float perShaft, ChartGenContext context, float share) {
        return perShaft * Math.max(0f, quantityMult(context)) * Math.max(0f, share);
    }

    static Map<String, Integer> tagInhibitors(float expectedCount, Random random) {
        List<CraftingInhibitor> kinds = List.copyOf(CraftingInhibitor.tagInhibitors());
        Map<String, Integer> found = new LinkedHashMap<>();
        for (int left = ItemDrops.wholeFeedstock(expectedCount, random); left > 0; left--) {
            found.merge(kinds.get(random.nextInt(kinds.size())).itemId(), 1, Integer::sum);
        }
        return found;
    }

    public static ItemDrops.SiteLoot fleetItems(CampaignFleetAPI fleet, ChartGenContext context, float share, Random random) {
        if (!ExcavationFleets.isExcavationFleet(fleet)) {
            return null;
        }
        Map<String, Integer> inhibitors = tagInhibitors(expectedInhibitors(ExcavationRules.INHIBITORS_PER_GUARDIAN_FLEET, context, share), random);
        return inhibitors.isEmpty() ? null : new ItemDrops.SiteLoot(List.of(), List.of(), inhibitors);
    }
}
