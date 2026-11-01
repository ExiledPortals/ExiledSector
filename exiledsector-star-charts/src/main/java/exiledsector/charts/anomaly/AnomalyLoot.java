package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.ChartVanillaLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record AnomalyLoot(String scope, String siteId, float baseSteps, float stepsPerTier, float itemMult, String quantityKey, float leanPercent) {

    public float quantityMult(ChartGenContext context) {
        return quantityKey == null ? 1f : (context == null ? ChartGenContext.none() : context).anomalyMult(quantityKey);
    }

    public float steps(int tier, float quantityMult) {
        return (baseSteps + stepsPerTier * (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER)) * Math.max(0f, quantityMult);
    }

    public float items(float quantityMult) {
        return itemMult * Math.max(0f, quantityMult);
    }

    public ChartGenContext leaning(ChartGenContext context, List<DropType> leans) {
        ChartGenContext base = context == null ? ChartGenContext.none() : context;
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        for (DropType lean : leans) {
            ChartEffect effect = ChartEffect.forDropType(lean);
            if (effect != null) {
                effects.add(new ChartGenContext.AppliedEffect(effect, leanPercent));
            }
        }
        return effects.isEmpty() ? base : base.withEffects(effects);
    }

    public CargoAPI roll(SectorAPI sector, ChartRun run, List<DropType> leans, float share, Random random) {
        ChartGenContext runContext = ChartRunEffects.contextOf(run);
        float quantity = quantityMult(runContext);
        return ChartCargoLoot.roll(sector, run, leaning(runContext, leans), scope, siteId, steps(run.tier(), quantity) * share, items(quantity) * share,
                random);
    }

    public ItemDrops.SiteLoot fleetItems(SectorAPI sector, ChartRun run, ChartGenContext context, List<DropType> leans, float share, Random random) {
        return ChartCargoLoot.rollItems(sector, run, leaning(context, leans), siteId, items(quantityMult(context)) * share, random);
    }

    public void addVanillaDrops(CampaignFleetAPI fleet, ChartGenContext runContext, List<DropType> leans, int tier) {
        ChartVanillaLoot.SiteDrops drops = ChartVanillaLoot.drops(scope, tier, steps(tier, quantityMult(runContext)), leaning(runContext, leans));
        drops.random().forEach(fleet::addDropRandom);
        drops.value().forEach(fleet::addDropValue);
    }
}
