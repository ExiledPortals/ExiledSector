package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record AtlasEffects(Map<AtlasModifier, Float> totals) {

    public static final AtlasEffects NONE = new AtlasEffects(Map.of());
    static final String EFFECT_SEPARATOR = ";";
    static final String VALUE_SEPARATOR = ":";

    public record Line(AtlasModifier effect, float value) {
    }

    public AtlasEffects {
        totals = Map.copyOf(totals);
    }

    public static AtlasEffects of(Collection<Line> lines) {
        Map<AtlasModifier, Float> totals = new EnumMap<>(AtlasModifier.class);
        lines.forEach(line -> totals.merge(line.effect(), line.value(), Float::sum));
        return new AtlasEffects(totals);
    }

    public float value(AtlasModifier effect) {
        return totals.getOrDefault(effect, 0f);
    }

    public int count(AtlasModifier effect) {
        return Math.round(value(effect));
    }

    public float atlasChancePercent(String anomalyId) {
        float percent = 0f;
        for (Map.Entry<AtlasModifier, Float> total : totals.entrySet()) {
            if (anomalyId.equals(total.getKey().atlasChanceAnomalyId())) {
                percent += total.getValue();
            }
        }
        return percent;
    }

    public String encode() {
        List<String> parts = new ArrayList<>();
        for (AtlasModifier effect : AtlasModifier.values()) {
            Float value = totals.get(effect);
            if (value != null) {
                parts.add(effect.name() + VALUE_SEPARATOR + value);
            }
        }
        return String.join(EFFECT_SEPARATOR, parts);
    }

    public static AtlasEffects decode(String text) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        List<Line> lines = new ArrayList<>();
        for (String part : text.split(EFFECT_SEPARATOR)) {
            int separator = part.indexOf(VALUE_SEPARATOR);
            AtlasModifier effect = separator <= 0 ? null : AtlasModifier.byNameOrNull(part.substring(0, separator));
            Float value = separator <= 0 ? null : parsedValue(part.substring(separator + 1));
            if (effect != null && value != null) {
                lines.add(new Line(effect, value));
            }
        }
        return of(lines);
    }

    private static Float parsedValue(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
