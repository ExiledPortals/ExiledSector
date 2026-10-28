package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.ChartLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class VaultLoot {

    private VaultLoot() {
    }

    static ChartGenContext contextFor(ChartGenContext runContext) {
        ChartGenContext context = runContext == null ? ChartGenContext.none() : runContext;
        List<ChartGenContext.AppliedEffect> leans = new ArrayList<>();
        for (DropType lean : HeistRules.LEANS) {
            ChartEffect effect = effectFor(lean);
            if (effect != null) {
                leans.add(new ChartGenContext.AppliedEffect(effect, HeistRules.LEAN_PERCENT));
            }
        }
        return leans.isEmpty() ? context : context.withEffects(leans);
    }

    static ChartEffect effectFor(DropType lean) {
        for (ChartEffect effect : ChartEffect.values()) {
            if (effect.dropType() == lean) {
                return effect;
            }
        }
        return null;
    }

    static float quantityMult(ChartGenContext context) {
        return (context == null ? ChartGenContext.none() : context).anomalyMult(ListeningPost.LOOT_KEY);
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, float share, Random random) {
        ChartGenContext runContext = ChartRunEffects.contextOf(run);
        float quantity = quantityMult(runContext);
        return ChartCargoLoot.roll(sector, run, contextFor(runContext), ChartLoot.LISTENING_POST_SCOPE, HeistRules.LOOT_SITE,
                HeistRules.lootSteps(run.tier(), quantity) * share, HeistRules.itemMult(quantity) * share, random);
    }
}
