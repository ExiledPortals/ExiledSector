package exiledsector.charts;

import java.util.ArrayList;
import java.util.List;

public record AtlasRunNode(long cell, String factionId, List<ChartGenContext.AppliedEffect> effects, String expansionId, String phenomenonId,
                           boolean guaranteedBossTierUp) {

    static final String FIELD_SEPARATOR = "|";
    static final String EFFECT_SEPARATOR = ";";
    static final String VALUE_SEPARATOR = ":";

    public AtlasRunNode {
        factionId = factionId == null ? "" : factionId;
        effects = effects == null ? List.of() : List.copyOf(effects);
        expansionId = expansionId == null ? "" : expansionId;
        phenomenonId = phenomenonId == null ? "" : phenomenonId;
    }

    public String encode() {
        List<String> parts = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : effects) {
            parts.add(effect.effect().name() + VALUE_SEPARATOR + effect.value());
        }
        return cell + FIELD_SEPARATOR + factionId + FIELD_SEPARATOR + String.join(EFFECT_SEPARATOR, parts) + FIELD_SEPARATOR + expansionId
                + FIELD_SEPARATOR + phenomenonId + FIELD_SEPARATOR + guaranteedBossTierUp;
    }

    public static AtlasRunNode decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] fields = text.split("\\" + FIELD_SEPARATOR, -1);
        if (fields.length < 6) {
            return null;
        }
        try {
            List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
            for (String part : fields[2].split(EFFECT_SEPARATOR)) {
                int separator = part.indexOf(VALUE_SEPARATOR);
                if (separator <= 0) {
                    continue;
                }
                ChartEffect effect = ChartEffect.byNameOrNull(part.substring(0, separator));
                if (effect != null) {
                    effects.add(new ChartGenContext.AppliedEffect(effect, Float.parseFloat(part.substring(separator + 1))));
                }
            }
            return new AtlasRunNode(Long.parseLong(fields[0]), fields[1], effects, fields[3], fields[4], Boolean.parseBoolean(fields[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
