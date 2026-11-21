package exiledsector.charts.phasing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record PhasedPlanetRecord(String runId, String planetId, String name, String typeId, float radius, String texture,
                                 List<String> conditionIds, String originName, String regionId, List<String> anomalyIds, int tier,
                                 List<String> traitConditionIds) {

    private static final String RUN_KEY = "run";
    private static final String PLANET_KEY = "planet";
    private static final String NAME_KEY = "name";
    private static final String TYPE_KEY = "type";
    private static final String RADIUS_KEY = "radius";
    private static final String TEXTURE_KEY = "texture";
    private static final String CONDITIONS_KEY = "conditions";
    private static final String ORIGIN_KEY = "origin";
    private static final String REGION_KEY = "region";
    private static final String ANOMALIES_KEY = "anomalies";
    private static final String TIER_KEY = "tier";
    private static final String TRAITS_KEY = "traits";

    public PhasedPlanetRecord {
        conditionIds = conditionIds == null ? List.of() : List.copyOf(conditionIds);
        anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
        traitConditionIds = traitConditionIds == null ? List.of() : List.copyOf(traitConditionIds);
    }

    public String encode() {
        try {
            JSONObject json = new JSONObject()
                    .put(RUN_KEY, runId)
                    .put(PLANET_KEY, planetId)
                    .put(NAME_KEY, name)
                    .put(TYPE_KEY, typeId)
                    .put(RADIUS_KEY, (double) radius)
                    .put(CONDITIONS_KEY, new JSONArray(conditionIds))
                    .put(ANOMALIES_KEY, new JSONArray(anomalyIds))
                    .put(TIER_KEY, tier)
                    .put(TRAITS_KEY, new JSONArray(traitConditionIds));
            putIfPresent(json, TEXTURE_KEY, texture);
            putIfPresent(json, ORIGIN_KEY, originName);
            putIfPresent(json, REGION_KEY, regionId);
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    private static void putIfPresent(JSONObject json, String key, String value) throws JSONException {
        if (value != null && !value.isEmpty()) {
            json.put(key, value);
        }
    }

    public static PhasedPlanetRecord decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(encoded);
            String typeId = json.optString(TYPE_KEY, "");
            if (typeId.isEmpty()) {
                return null;
            }
            return new PhasedPlanetRecord(json.optString(RUN_KEY, ""), json.optString(PLANET_KEY, ""), json.optString(NAME_KEY, ""), typeId,
                    (float) json.optDouble(RADIUS_KEY, 0d), textOrNull(json, TEXTURE_KEY), texts(json.optJSONArray(CONDITIONS_KEY)),
                    textOrNull(json, ORIGIN_KEY), textOrNull(json, REGION_KEY), texts(json.optJSONArray(ANOMALIES_KEY)),
                    json.optInt(TIER_KEY, 1), texts(json.optJSONArray(TRAITS_KEY)));
        } catch (JSONException e) {
            return null;
        }
    }

    private static String textOrNull(JSONObject json, String key) {
        String value = json.optString(key, "");
        return value.isEmpty() ? null : value;
    }

    private static List<String> texts(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int index = 0; index < array.length(); index++) {
            String value = array.optString(index, "");
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }
}
