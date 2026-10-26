package exiledsector.charts;

import java.util.ArrayList;
import java.util.List;

public record AtlasRunNode(long cell, String factionId, List<ChartGenContext.AppliedEffect> effects, List<String> anomalyIds,
                           boolean guaranteedBossTierUp) {

    static final String FIELD_SEPARATOR = "|";
    static final String EFFECT_SEPARATOR = ";";
    static final String VALUE_SEPARATOR = ":";
    static final String ID_SEPARATOR = ",";
    private static final int FIELDS = 5;
    private static final int FIELDS_BEFORE_ANOMALIES = 6;

    public AtlasRunNode {
        factionId = factionId == null ? "" : factionId;
        effects = effects == null ? List.of() : List.copyOf(effects);
        anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
    }

    public String encode() {
        List<String> parts = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : effects) {
            parts.add(effect.effect().name() + VALUE_SEPARATOR + effect.value());
        }
        return cell + FIELD_SEPARATOR + factionId + FIELD_SEPARATOR + String.join(EFFECT_SEPARATOR, parts) + FIELD_SEPARATOR
                + String.join(ID_SEPARATOR, anomalyIds) + FIELD_SEPARATOR + guaranteedBossTierUp;
    }

    public static AtlasRunNode decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] fields = text.split("\\" + FIELD_SEPARATOR, -1);
        if (fields.length != FIELDS && fields.length != FIELDS_BEFORE_ANOMALIES) {
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
            List<String> anomalyIds = new ArrayList<>();
            for (int field = 3; field < fields.length - 1; field++) {
                for (String id : fields[field].split(ID_SEPARATOR)) {
                    if (!id.isBlank() && !anomalyIds.contains(id)) {
                        anomalyIds.add(id);
                    }
                }
            }
            return new AtlasRunNode(Long.parseLong(fields[0]), fields[1], effects, anomalyIds, Boolean.parseBoolean(fields[fields.length - 1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
