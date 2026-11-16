package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartCraftingPools;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.MaterialItems;
import exiledsector.loot.Loot;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ExcavationLoot {

    private ExcavationLoot() {
    }

    public static CargoAPI raidCargo(ChartRun run, float share, Random random) {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        ChartLootContext loot = ChartLootContext.of(run);
        float expectedInhibitors = expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, loot.effects(), share);
        tagInhibitors(expectedInhibitors, loot, random).forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        return cargo;
    }

    static float quantityMult(ChartGenContext context) {
        return ChartGenContext.orNone(context).anomalyMult(ExcavationSites.LOOT_KEY);
    }

    static float expectedInhibitors(float perShaft, ChartGenContext context, float share) {
        return perShaft * Math.max(0f, quantityMult(context)) * Math.max(0f, share);
    }

    static Map<String, Integer> tagInhibitors(float expectedCount, ChartLootContext loot, Random random) {
        return loot.craftingItems(expectedCount, ChartCraftingPools.TAG_INHIBITORS, ChartLootContext.PlayerFindBonus.SKIPPED, random);
    }

    public static Loot fleetItems(CampaignFleetAPI fleet, ChartLootContext loot, float share, Random random) {
        if (!ExcavationFleets.isExcavationFleet(fleet)) {
            return Loot.NONE;
        }
        Map<String, Integer> inhibitors = tagInhibitors(expectedInhibitors(ExcavationRules.INHIBITORS_PER_GUARDIAN_FLEET, loot.effects(), share),
                loot, random);
        return inhibitors.isEmpty() ? Loot.NONE : new Loot(List.of(), List.of(), inhibitors);
    }
}
