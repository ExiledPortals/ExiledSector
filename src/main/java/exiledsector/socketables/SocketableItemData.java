package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SpecialItemData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public record SocketableItemData(String definitionId, long seed, List<RolledEffect> effects, FrozenName name) {

    public static final String ITEM_ID = "exiledSector_socketable";
    private static final String SEPARATOR = "|";
    private static final String DEFINITION_KEY = "d";
    private static final String SEED_KEY = "s";
    private static final String EFFECTS_KEY = "e";
    private static final String NAMED_KEY = "n";
    private static final String PREFIX_KEY = "np";
    private static final String SUFFIX_KEY = "ns";
    private static final String RARE_FIRST_KEY = "r1";
    private static final String RARE_SECOND_KEY = "r2";

    public SocketableItemData {
        effects = effects == null ? null : List.copyOf(effects);
    }

    public SocketableItemData(String definitionId, long seed) {
        this(definitionId, seed, null, null);
    }

    public static SocketableItemData rolled(SocketableDefinition definition, long seed) {
        List<RolledEffect> effects = SocketableRoller.roll(definition, seed);
        return new SocketableItemData(definition.id(), seed, effects, SocketableNames.freeze(definition, seed, effects));
    }

    public static SocketableItemData parse(String data) {
        if (data == null) {
            return null;
        }
        if (data.startsWith("{")) {
            return parseFrozen(data);
        }
        int separator = data.lastIndexOf(SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        try {
            return new SocketableItemData(data.substring(0, separator), Long.parseLong(data.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SocketableItemData parseFrozen(String data) {
        try {
            JSONObject json = new JSONObject(data);
            String definitionId = json.optString(DEFINITION_KEY, "");
            if (definitionId.isEmpty() || !json.has(SEED_KEY)) {
                return null;
            }
            List<RolledEffect> effects = json.has(EFFECTS_KEY) ? RolledEffect.decode(json.getString(EFFECTS_KEY)) : null;
            FrozenName name = effects == null || !json.optBoolean(NAMED_KEY, false) ? null
                    : new FrozenName(text(json, PREFIX_KEY), text(json, SUFFIX_KEY), text(json, RARE_FIRST_KEY), text(json, RARE_SECOND_KEY));
            return new SocketableItemData(definitionId, json.getLong(SEED_KEY), effects, name);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String text(JSONObject json, String key) {
        String value = json.optString(key, "");
        return value.isEmpty() ? null : value;
    }

    public static SocketableItemData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public SpecialItemData toSpecialItem() {
        if (effects == null) {
            return new SpecialItemData(ITEM_ID, definitionId + SEPARATOR + seed);
        }
        FrozenName frozen = name;
        if (frozen == null) {
            frozen = SocketableNames.freeze(definition(), seed, effects);
        }
        try {
            JSONObject json = new JSONObject().put(DEFINITION_KEY, definitionId).put(SEED_KEY, seed)
                    .put(EFFECTS_KEY, RolledEffect.encode(effects));
            if (frozen != null) {
                json.put(NAMED_KEY, true);
                putText(json, PREFIX_KEY, frozen.prefixEffect());
                putText(json, SUFFIX_KEY, frozen.suffixEffect());
                putText(json, RARE_FIRST_KEY, frozen.rareFirst());
                putText(json, RARE_SECOND_KEY, frozen.rareSecond());
            }
            return new SpecialItemData(ITEM_ID, json.toString());
        } catch (JSONException e) {
            return new SpecialItemData(ITEM_ID, definitionId + SEPARATOR + seed);
        }
    }

    private static void putText(JSONObject json, String key, String value) throws JSONException {
        if (value != null) {
            json.put(key, value);
        }
    }

    public SocketableDefinition definition() {
        return SocketableDefinitions.get(definitionId);
    }

    public Socketable preview() {
        return create(null);
    }

    Socketable create(String instanceId) {
        SocketableDefinition definition = definition();
        if (definition == null) {
            return null;
        }
        Socketable socketable = definition.kind().create(instanceId, definitionId, seed,
                effects != null ? effects : SocketableRoller.roll(definition, seed));
        socketable.freezeName(name);
        return socketable;
    }
}
