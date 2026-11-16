package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record AnomalyLoot(String scope, String siteId, float baseSteps, float stepsPerTier, float itemMult, String quantityKey, float leanPercent) {

    public float quantityPercent(ChartGenContext context) {
        return quantityKey == null ? 0f : ChartGenContext.orNone(context).anomalyPercent(quantityKey);
    }

    public float steps(int tier) {
        return baseSteps + stepsPerTier * (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER);
    }

    public ChartLootContext withQuantityAndLeans(ChartLootContext loot, List<DropType> leans) {
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        for (DropType lean : leans) {
            ChartModifier effect = ChartModifier.forDropType(lean);
            if (effect != null) {
                effects.add(new ChartGenContext.AppliedEffect(effect, leanPercent));
            }
        }
        return loot.plusQuantityPercent(quantityPercent(loot.effects())).withEffects(effects);
    }

    public CargoAPI roll(SectorAPI sector, ChartRun run, List<DropType> leans, float share, Random random) {
        ChartLootContext runLoot = ChartLootContext.of(run);
        return ChartCargoLoot.roll(sector, withQuantityAndLeans(runLoot, leans), scope, siteId, steps(runLoot.tier()) * share, itemMult * share, random);
    }
}
