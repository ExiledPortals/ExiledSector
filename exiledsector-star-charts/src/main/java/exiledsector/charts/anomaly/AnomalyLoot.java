package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

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
        ChartGenContext base = ChartGenContext.orNone(context);
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        for (DropType lean : leans) {
            ChartModifier effect = ChartModifier.forDropType(lean);
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
}
