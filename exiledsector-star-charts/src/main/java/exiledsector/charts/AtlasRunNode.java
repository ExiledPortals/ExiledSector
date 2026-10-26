package exiledsector.charts;

import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;

import java.util.ArrayList;
import java.util.List;

public record AtlasRunNode(long cell, String factionId, List<ChartGenContext.AppliedEffect> effects, List<String> anomalyIds,
                           boolean guaranteedBossTierUp, String skirmishAttackerId, SkirmishSide skirmishSide) {

    static final String FIELD_SEPARATOR = "|";
    static final String EFFECT_SEPARATOR = ";";
    static final String VALUE_SEPARATOR = ":";
    static final String ID_SEPARATOR = ",";
    private static final int FIELDS_BEFORE_SKIRMISH = 5;
    private static final int FIELDS_BEFORE_ANOMALIES = 6;
    private static final int FIELDS = 7;

    public AtlasRunNode {
        factionId = factionId == null ? "" : factionId;
        effects = effects == null ? List.of() : List.copyOf(effects);
        anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
        skirmishAttackerId = skirmishAttackerId == null || skirmishAttackerId.isEmpty() || skirmishSide == null ? null : skirmishAttackerId;
        skirmishSide = skirmishAttackerId == null ? null : skirmishSide;
    }

    public AtlasRunNode(long cell, String factionId, List<ChartGenContext.AppliedEffect> effects, List<String> anomalyIds, boolean guaranteedBossTierUp) {
        this(cell, factionId, effects, anomalyIds, guaranteedBossTierUp, null, null);
    }

    public AtlasRunNode withSkirmish(String attackerId, SkirmishSide side) {
        return new AtlasRunNode(cell, factionId, effects, anomalyIds, guaranteedBossTierUp, attackerId, side);
    }

    public String encode() {
        List<String> parts = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : effects) {
            parts.add(effect.effect().name() + VALUE_SEPARATOR + effect.value());
        }
        return cell + FIELD_SEPARATOR + factionId + FIELD_SEPARATOR + String.join(EFFECT_SEPARATOR, parts) + FIELD_SEPARATOR
                + String.join(ID_SEPARATOR, anomalyIds) + FIELD_SEPARATOR + guaranteedBossTierUp + FIELD_SEPARATOR
                + (skirmishAttackerId == null ? "" : skirmishAttackerId) + FIELD_SEPARATOR + (skirmishSide == null ? "" : skirmishSide.key());
    }

    public static AtlasRunNode decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] fields = text.split("\\" + FIELD_SEPARATOR, -1);
        if (fields.length != FIELDS && fields.length != FIELDS_BEFORE_SKIRMISH && fields.length != FIELDS_BEFORE_ANOMALIES) {
            return null;
        }
        boolean skirmishFields = fields.length == FIELDS;
        int bossField = skirmishFields ? 4 : fields.length - 1;
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
            for (int field = 3; field < bossField; field++) {
                for (String id : fields[field].split(ID_SEPARATOR)) {
                    if (!id.isBlank() && !anomalyIds.contains(id)) {
                        anomalyIds.add(id);
                    }
                }
            }
            return new AtlasRunNode(Long.parseLong(fields[0]), fields[1], effects, anomalyIds, Boolean.parseBoolean(fields[bossField]),
                    skirmishFields ? fields[5] : null, skirmishFields ? SkirmishSide.parse(fields[6]) : null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
