package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.ChartVanillaLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.ItemDrops;

import java.util.List;
import java.util.Random;

public final class ExcavationLoot {

    private ExcavationLoot() {
    }

    public static ChartGenContext contextFor(ChartGenContext runContext, DropType lean) {
        ChartGenContext context = runContext == null ? ChartGenContext.none() : runContext;
        ChartEffect effect = effectFor(lean);
        return effect == null ? context : context.withEffects(List.of(new ChartGenContext.AppliedEffect(effect, ExcavationRules.LEAN_PERCENT)));
    }

    static ChartEffect effectFor(DropType lean) {
        for (ChartEffect effect : ChartEffect.values()) {
            if (lean != null && effect.dropType() == lean) {
                return effect;
            }
        }
        return null;
    }

    static float quantityMult(ChartGenContext context) {
        return (context == null ? ChartGenContext.none() : context).anomalyMult(ExcavationSites.LOOT_KEY);
    }

    public static CargoAPI raidCargo(SectorAPI sector, ChartRun run, ExcavationSite site, float share, Random random) {
        ChartGenContext runContext = ChartRunEffects.contextOf(run);
        float quantity = quantityMult(runContext);
        return ChartCargoLoot.roll(sector, run, contextFor(runContext, site.lean()), ExcavationRules.LOOT_SCOPE, ExcavationRules.LOOT_SITE,
                ExcavationRules.lootSteps(run.tier(), quantity) * share, ExcavationRules.itemMult(quantity) * share, random);
    }

    public static void addToFleet(CampaignFleetAPI fleet, DropType lean, ChartGenContext runContext, int tier) {
        ChartVanillaLoot.SiteDrops drops = ChartVanillaLoot.drops(ExcavationRules.LOOT_SCOPE, tier,
                ExcavationRules.lootSteps(tier, quantityMult(runContext)), contextFor(runContext, lean));
        drops.random().forEach(fleet::addDropRandom);
        drops.value().forEach(fleet::addDropValue);
    }

    public static ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share,
                                                Random random) {
        DropType lean = ShaftFleets.leanOf(fleet);
        if (lean == null) {
            return null;
        }
        return ChartCargoLoot.rollItems(sector, run, contextFor(context, lean), ExcavationRules.LOOT_SITE,
                ExcavationRules.itemMult(quantityMult(context)) * share, random);
    }
}
