package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.Engrams;
import exiledsector.items.Engram;
import exiledsector.items.ItemDrops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DormantFleetLoot {

    private DormantFleetLoot() {
    }

    public static ItemDrops.SiteLoot craftingItemFor(CampaignFleetAPI fleet, int tier, ChartGenContext context, Random random) {
        if (!DormantFleets.isCorrupted(fleet)) {
            return ItemDrops.SiteLoot.NONE;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory.getBoolean(DormantFleets.CRAFTING_ITEM_DROPPED_KEY)) {
            return ItemDrops.SiteLoot.NONE;
        }
        memory.set(DormantFleets.CRAFTING_ITEM_DROPPED_KEY, true);
        float quantityMult = (context == null ? ChartGenContext.none() : context).anomalyMult(AnomalousDebris.CRAFTING_ITEM_KEY);
        Map<String, Integer> craftingItems = new LinkedHashMap<>();
        for (int drop = DebrisRules.craftingItemCount(quantityMult, random); drop > 0; drop--) {
            Engram craftingItem = Engrams.pick(tier, random);
            if (craftingItem != null) {
                craftingItems.merge(craftingItem.itemId(), 1, Integer::sum);
            }
        }
        return craftingItems.isEmpty() ? ItemDrops.SiteLoot.NONE : ItemDrops.SiteLoot.NONE.plus(List.of(), List.of(), craftingItems);
    }
}
