package exiledsector.charts.atlas;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public record AtlasEffects(Map<AtlasModifier, Float> totals) {

    public static final AtlasEffects NONE = new AtlasEffects(Map.of());

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
}
