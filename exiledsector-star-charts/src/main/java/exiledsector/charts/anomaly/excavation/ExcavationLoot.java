package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.AnomalyLoot;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;

import java.util.List;
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
        return LOOT.roll(sector, run, leaning(site.lean()), share, random);
    }

    public static void addToFleet(CampaignFleetAPI fleet, DropType lean, ChartGenContext runContext, int tier) {
        LOOT.addVanillaDrops(fleet, runContext, leaning(lean), tier);
    }

    public static ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share,
                                                Random random) {
        DropType lean = ShaftFleets.leanOf(fleet);
        return lean == null ? null : LOOT.fleetItems(sector, run, context, leaning(lean), share, random);
    }
}
