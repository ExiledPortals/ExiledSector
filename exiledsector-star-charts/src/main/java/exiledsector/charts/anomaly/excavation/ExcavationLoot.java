package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartCraftingPools;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.Engram;
import exiledsector.items.Engrams;
import exiledsector.items.MaterialItems;
import exiledsector.loot.Loot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ExcavationLoot {

    private ExcavationLoot() {
    }

    public static CargoAPI raidCargo(ChartRun run, float share, Random random) {
        return raidCargo(run, share, 0f, random);
    }

    static CargoAPI raidCargo(ChartRun run, float share, float deliveryPercent, Random random) {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        ChartLootContext loot = ChartLootContext.of(run);
        ExcavationTuning tuning = ExcavationTuning.of(loot.effects());
        float quantityPercent = ExcavationRules.inhibitorQuantityPercent(tuning, false, DenseNebula.dust(run)) + deliveryPercent;
        float expectedInhibitors = expectedInhibitors(ExcavationRules.raidInhibitors(tuning), loot.effects(), quantityPercent, share);
        tagInhibitors(expectedInhibitors, loot, random).forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        return cargo;
    }

    static float quantityMult(ChartGenContext context) {
        return quantityMult(context, 0f);
    }

    static float quantityMult(ChartGenContext context, float extraSitePercent) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        float sitePercent = lines.anomalyPercent(ExcavationSites.LOOT_KEY) + extraSitePercent;
        return ExcavationRules.multiplier(sitePercent) * lines.dropMult(DropType.CRAFTING_ITEM);
    }

    static float expectedInhibitors(float baseInhibitors, ChartGenContext context, float share) {
        return expectedInhibitors(baseInhibitors, context, 0f, share);
    }

    static float expectedInhibitors(float baseInhibitors, ChartGenContext context, float extraSitePercent, float share) {
        return Math.max(0f, baseInhibitors) * quantityMult(context, extraSitePercent) * Math.max(0f, share);
    }

    static Map<String, Integer> tagInhibitors(float expectedCount, ChartLootContext loot, Random random) {
        return loot.craftingItems(expectedCount, ChartCraftingPools.TAG_INHIBITORS, ChartLootContext.PlayerFindBonus.SKIPPED, random);
    }

    public static Loot fleetItems(CampaignFleetAPI fleet, ChartLootContext loot, int dustStacks, float share, Random random) {
        if (!ExcavationFleets.isExcavationFleet(fleet)) {
            return Loot.NONE;
        }
        ExcavationTuning tuning = ExcavationTuning.of(loot.effects());
        ExcavationGuardians guardians = ExcavationFleets.guardiansOf(fleet);
        float quantityPercent = ExcavationRules.inhibitorQuantityPercent(tuning, true, dustStacks) + ExcavationDeliveries.lootPercent(fleet);
        Map<String, Integer> found = new LinkedHashMap<>(tagInhibitors(
                expectedInhibitors(ExcavationRules.guardianInhibitors(guardians, tuning), loot.effects(), quantityPercent, share), loot, random));
        if (guardians.corrupted() && share > 0f) {
            String engramId = engramOnce(fleet.getMemoryWithoutUpdate(), loot.tier(), random);
            if (engramId != null) {
                found.merge(engramId, 1, Integer::sum);
            }
        }
        return found.isEmpty() ? Loot.NONE : new Loot(List.of(), List.of(), found);
    }

    static String engramOnce(MemoryAPI memory, int chartTier, Random random) {
        if (memory == null || memory.getBoolean(ExcavationFleets.ENGRAM_DROPPED_KEY)) {
            return null;
        }
        Engram engram = Engrams.pick(chartTier, random);
        if (engram == null) {
            return null;
        }
        memory.set(ExcavationFleets.ENGRAM_DROPPED_KEY, true);
        return engram.itemId();
    }
}
