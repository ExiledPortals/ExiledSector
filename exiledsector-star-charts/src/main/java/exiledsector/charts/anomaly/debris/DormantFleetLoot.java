package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.items.crafting.material.Engram;
import exiledsector.items.crafting.material.Engrams;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DormantFleetLoot {

    private DormantFleetLoot() {
    }

    public static Loot craftingItemFor(CampaignFleetAPI fleet, ChartLootContext loot, Random random) {
        if (!DormantFleets.isCorrupted(fleet)) {
            return Loot.NONE;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory.getBoolean(DormantFleets.CRAFTING_ITEM_DROPPED_KEY)) {
            return Loot.NONE;
        }
        memory.set(DormantFleets.CRAFTING_ITEM_DROPPED_KEY, true);
        float quantityMult = loot.quantityMult(DropType.ENGRAM, loot.effects().anomalyPercent(AnomalousDebris.CRAFTING_ITEM_KEY));
        int engrams = DebrisTuning.of(loot.effects()).engramsFor(DormantFleets.corruptionsOf(fleet));
        Map<String, Integer> craftingItems = new LinkedHashMap<>();
        for (int drop = DebrisRules.craftingItemCount(engrams, quantityMult, random); drop > 0; drop--) {
            Engram craftingItem = Engrams.pick(loot.tier(), random);
            if (craftingItem != null) {
                craftingItems.merge(craftingItem.itemId(), 1, Integer::sum);
            }
        }
        return craftingItems.isEmpty() ? Loot.NONE : Loot.NONE.plus(List.of(), List.of(), craftingItems);
    }
}
