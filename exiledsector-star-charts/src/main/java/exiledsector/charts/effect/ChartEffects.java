package exiledsector.charts.effect;

import com.fs.starfarer.api.Global;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.loot.ChartLootTotals;
import exiledsector.items.ItemEffects;
import exiledsector.items.ItemType;
import exiledsector.items.StarChart;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;

public final class ChartEffects implements ModifierEffects {

    public static final ChartEffects RESOLVER = new ChartEffects();

    private ChartEffects() {
    }

    public static void register() {
        ItemEffects.register(ItemType.STAR_CHART, RESOLVER);
        StarChart.useLootTotals(ChartLootTotals::lines);
    }

    @Override
    public ModifierEffect byName(String effectName) {
        ChartEffect effect = ChartEffect.byNameOrNull(effectName);
        return effect != null ? effect : ChartFactions.effectByName(effectName);
    }

    @Override
    public boolean canRoll(String effectName) {
        ChartEffect effect = ChartEffect.byNameOrNull(effectName);
        return effect == null || effect.canRoll(Global.getSector());
    }
}
