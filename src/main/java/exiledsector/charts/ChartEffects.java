package exiledsector.charts;

import exiledsector.items.ItemEffects;
import exiledsector.items.ItemType;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;

public final class ChartEffects implements ModifierEffects {

    public static final ChartEffects RESOLVER = new ChartEffects();

    private ChartEffects() {
    }

    public static void register() {
        ItemEffects.register(ItemType.STAR_CHART, RESOLVER);
    }

    @Override
    public ModifierEffect byName(String effectName) {
        ChartEffect effect = ChartEffect.byNameOrNull(effectName);
        return effect != null ? effect : ChartFactions.effectByName(effectName);
    }
}
