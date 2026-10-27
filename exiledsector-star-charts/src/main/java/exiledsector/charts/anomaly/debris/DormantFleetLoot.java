package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.AnomalousDebrisCurrencies;
import exiledsector.items.AnomalousDebrisCurrency;
import exiledsector.items.ItemDrops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DormantFleetLoot {

    private DormantFleetLoot() {
    }

    public static ItemDrops.SiteLoot currencyFor(CampaignFleetAPI fleet, int tier, ChartGenContext context, Random random) {
        if (!DormantFleets.isCorrupted(fleet)) {
            return ItemDrops.SiteLoot.NONE;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory.getBoolean(DormantFleets.CURRENCY_DROPPED_KEY)) {
            return ItemDrops.SiteLoot.NONE;
        }
        memory.set(DormantFleets.CURRENCY_DROPPED_KEY, true);
        float quantityMult = (context == null ? ChartGenContext.none() : context).anomalyMult(AnomalousDebris.CURRENCY_KEY);
        Map<String, Integer> currencies = new LinkedHashMap<>();
        for (int drop = DebrisRules.currencyCount(quantityMult, random); drop > 0; drop--) {
            AnomalousDebrisCurrency currency = AnomalousDebrisCurrencies.pick(tier, random);
            if (currency != null) {
                currencies.merge(currency.itemId(), 1, Integer::sum);
            }
        }
        return currencies.isEmpty() ? ItemDrops.SiteLoot.NONE : ItemDrops.SiteLoot.NONE.plus(List.of(), List.of(), currencies);
    }
}
