package exiledsector.charts.generation;

import exiledsector.charts.effect.ChartModifier;

import java.util.ArrayList;
import java.util.List;

public final class AppliedEffectCodec {

    static final String EFFECT_SEPARATOR = ";";
    static final String VALUE_SEPARATOR = ":";

    private AppliedEffectCodec() {
    }

    public static String encode(List<ChartGenContext.AppliedEffect> effects) {
        List<String> parts = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : effects) {
            parts.add(effect.effect().name() + VALUE_SEPARATOR + effect.value());
        }
        return String.join(EFFECT_SEPARATOR, parts);
    }

    public static List<ChartGenContext.AppliedEffect> decode(String text) {
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        if (text == null) {
            return effects;
        }
        for (String part : text.split(EFFECT_SEPARATOR)) {
            int separator = part.indexOf(VALUE_SEPARATOR);
            if (separator <= 0) {
                continue;
            }
            ChartModifier effect = ChartModifier.byNameOrNull(part.substring(0, separator));
            if (effect != null) {
                effects.add(new ChartGenContext.AppliedEffect(effect, Float.parseFloat(part.substring(separator + 1))));
            }
        }
        return effects;
    }
}
