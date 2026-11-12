package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.AnomalyLoot;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.DropType;
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

    static final AnomalyLoot LOOT = new AnomalyLoot(ExcavationRules.LOOT_SCOPE, ExcavationRules.LOOT_SITE, ExcavationRules.LOOT_STEPS,
            ExcavationRules.LOOT_STEPS_PER_TIER, ExcavationRules.ITEM_MULT, ExcavationSites.LOOT_KEY, ExcavationRules.LEAN_PERCENT);

    private ExcavationLoot() {
    }

    static List<DropType> leaning(DropType lean) {
        return lean == null ? List.of() : List.of(lean);
    }

    public static CargoAPI raidCargo(SectorAPI sector, ChartRun run, ExcavationSite site, float share, Random random) {
        CargoAPI cargo = LOOT.roll(sector, run, leaning(site.lean()), share, random);
        float expectedInhibitors = expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, ChartRunEffects.contextOf(run), share);
        tagInhibitors(expectedInhibitors, random).forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        return cargo;
    }

    static float expectedInhibitors(float perShaft, ChartGenContext context, float share) {
        return perShaft * LOOT.items(LOOT.quantityMult(context)) * Math.max(0f, share);
    }

    static Map<String, Integer> tagInhibitors(float expectedCount, Random random) {
        List<CraftingInhibitor> kinds = List.copyOf(CraftingInhibitor.tagInhibitors());
        Map<String, Integer> found = new LinkedHashMap<>();
        for (int left = ItemDrops.wholeFeedstock(expectedCount, random); left > 0; left--) {
            found.merge(kinds.get(random.nextInt(kinds.size())).itemId(), 1, Integer::sum);
        }
        return found;
    }

    static ItemDrops.SiteLoot withInhibitors(ItemDrops.SiteLoot loot, Map<String, Integer> inhibitors) {
        if (inhibitors.isEmpty()) {
            return loot;
        }
        ItemDrops.SiteLoot base = loot == null ? ItemDrops.SiteLoot.NONE : loot;
        Map<String, Integer> materials = new LinkedHashMap<>(base.materials());
        inhibitors.forEach((itemId, quantity) -> materials.merge(itemId, quantity, Integer::sum));
        return new ItemDrops.SiteLoot(base.items(), base.upgrades(), materials);
    }

    public static void addToFleet(CampaignFleetAPI fleet, DropType lean, ChartGenContext runContext, int tier) {
        LOOT.addVanillaDrops(fleet, runContext, leaning(lean), tier);
    }

    public static ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share,
                                                Random random) {
        DropType lean = ExcavationFleets.leanOf(fleet);
        if (lean == null) {
            return null;
        }
        ItemDrops.SiteLoot loot = LOOT.fleetItems(sector, run, context, leaning(lean), share, random);
        return withInhibitors(loot, tagInhibitors(expectedInhibitors(ExcavationRules.INHIBITORS_PER_GUARDIAN_FLEET, context, share), random));
    }
}
