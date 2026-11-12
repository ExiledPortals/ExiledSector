package exiledsector.charts.atlas;

import exiledsector.charts.generation.AppliedEffectCodec;
import exiledsector.charts.generation.ChartGenContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AtlasRunNode(long cell, String factionId, List<ChartGenContext.AppliedEffect> effects, List<String> anomalyIds,
                           boolean guaranteedBossUnique, Map<String, String> anomalyData) {

    static final String FIELD_SEPARATOR = "|";
    static final String ID_SEPARATOR = ",";
    static final String DATA_SEPARATOR = ";";
    static final String DATA_VALUE_SEPARATOR = "=";
    private static final int FIELDS = 6;
    private static final int FIELDS_BEFORE_UNIQUE = 5;
    private static final int FIELDS_WITH_TIER_UP = 7;
    private static final int FIELDS_WITH_SKIRMISH = 8;

    public AtlasRunNode {
        factionId = factionId == null ? "" : factionId;
        effects = effects == null ? List.of() : List.copyOf(effects);
        anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
        anomalyData = anomalyData == null ? Map.of() : Map.copyOf(anomalyData);
    }

    public AtlasRunNode(long cell, String factionId, List<ChartGenContext.AppliedEffect> effects, List<String> anomalyIds) {
        this(cell, factionId, effects, anomalyIds, false, Map.of());
    }

    public String anomalyData(String anomalyId) {
        return anomalyData.get(anomalyId);
    }

    public AtlasRunNode withAnomalyData(String anomalyId, String data) {
        Map<String, String> changed = new LinkedHashMap<>(anomalyData);
        if (data == null || data.isEmpty()) {
            changed.remove(anomalyId);
        } else {
            changed.put(anomalyId, data);
        }
        return new AtlasRunNode(cell, factionId, effects, anomalyIds, guaranteedBossUnique, changed);
    }

    public AtlasRunNode withExtraEffects(List<ChartGenContext.AppliedEffect> extraEffects, boolean bossUnique) {
        List<ChartGenContext.AppliedEffect> combined = new ArrayList<>(effects);
        combined.addAll(extraEffects);
        return new AtlasRunNode(cell, factionId, combined, anomalyIds, guaranteedBossUnique || bossUnique, anomalyData);
    }

    public String encode() {
        List<String> data = new ArrayList<>();
        anomalyData.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> data.add(entry.getKey() + DATA_VALUE_SEPARATOR + entry.getValue()));
        return cell + FIELD_SEPARATOR + factionId + FIELD_SEPARATOR + AppliedEffectCodec.encode(effects) + FIELD_SEPARATOR
                + String.join(ID_SEPARATOR, anomalyIds) + FIELD_SEPARATOR + guaranteedBossUnique
                + FIELD_SEPARATOR + String.join(DATA_SEPARATOR, data);
    }

    public static AtlasRunNode decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] fields = text.split("\\" + FIELD_SEPARATOR, -1);
        if (fields.length != FIELDS && fields.length != FIELDS_BEFORE_UNIQUE && fields.length != FIELDS_WITH_TIER_UP
                && fields.length != FIELDS_WITH_SKIRMISH) {
            return null;
        }
        try {
            boolean bossUnique = switch (fields.length) {
                case FIELDS -> Boolean.parseBoolean(fields[4]);
                case FIELDS_WITH_TIER_UP -> Boolean.parseBoolean(fields[5]);
                case FIELDS_WITH_SKIRMISH -> Boolean.parseBoolean(fields[7]);
                default -> false;
            };
            Map<String, String> data = switch (fields.length) {
                case FIELDS -> decodeData(fields[5]);
                case FIELDS_WITH_TIER_UP -> decodeData(fields[6]);
                default -> Map.of();
            };
            return new AtlasRunNode(Long.parseLong(fields[0]), fields[1], AppliedEffectCodec.decode(fields[2]), decodeIds(fields[3]), bossUnique, data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> decodeIds(String field) {
        List<String> anomalyIds = new ArrayList<>();
        for (String id : field.split(ID_SEPARATOR)) {
            if (!id.isBlank() && !anomalyIds.contains(id)) {
                anomalyIds.add(id);
            }
        }
        return anomalyIds;
    }

    private static Map<String, String> decodeData(String field) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String entry : field.split(DATA_SEPARATOR)) {
            int separator = entry.indexOf(DATA_VALUE_SEPARATOR);
            if (separator > 0 && separator < entry.length() - 1) {
                data.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }
        return data;
    }
}
