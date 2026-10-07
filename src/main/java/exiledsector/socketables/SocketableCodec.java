package exiledsector.socketables;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class SocketableCodec {

    static final String NPC_PREFIX = "npc:";
    private static final String FROZEN_START = "{";
    private static final String CARGO_SEED_SEPARATOR = "|";
    private static final String NPC_SEPARATOR = "/";
    private static final String EFFECT_SEPARATOR = ";";
    private static final String MAGNITUDE_SEPARATOR = ":";
    private static final String DEFINITION_KEY = "d";
    private static final String SEED_KEY = "s";
    private static final String EFFECTS_KEY = "e";
    private static final String NAMED_KEY = "n";
    private static final String PREFIX_KEY = "np";
    private static final String SUFFIX_KEY = "ns";
    private static final String RARE_FIRST_KEY = "r1";
    private static final String RARE_SECOND_KEY = "r2";
    private static final String RARE_BRAND_KEY = "rb";
    private static final String RARE_MODEL_KEY = "rm";
    private static final String PRODUCT_KEY = "rp";

    private SocketableCodec() {
    }

    static boolean isNpc(String encoded) {
        return encoded != null && encoded.startsWith(NPC_PREFIX);
    }

    static String cargo(SocketableItemData item, FrozenName frozenName) {
        if (item.effects() == null) {
            return seeded(item, CARGO_SEED_SEPARATOR);
        }
        try {
            JSONObject cargoJson = new JSONObject().put(DEFINITION_KEY, item.definitionId()).put(SEED_KEY, item.seed())
                    .put(EFFECTS_KEY, effects(item.effects()));
            if (frozenName != null) {
                cargoJson.put(NAMED_KEY, true);
                putText(cargoJson, PREFIX_KEY, frozenName.prefixEffect());
                putText(cargoJson, SUFFIX_KEY, frozenName.suffixEffect());
                putText(cargoJson, RARE_FIRST_KEY, frozenName.rareFirst());
                putText(cargoJson, RARE_SECOND_KEY, frozenName.rareSecond());
                putText(cargoJson, RARE_BRAND_KEY, frozenName.rareBrand());
                putText(cargoJson, RARE_MODEL_KEY, frozenName.rareModel());
                if (frozenName.isProduct()) {
                    cargoJson.put(PRODUCT_KEY, true);
                }
            }
            return cargoJson.toString();
        } catch (JSONException e) {
            return seeded(item, CARGO_SEED_SEPARATOR);
        }
    }

    static String npc(String definitionId, long seed) {
        return NPC_PREFIX + definitionId + NPC_SEPARATOR + seed;
    }

    static String npc(SocketableItemData item) {
        String npcId = npc(item.definitionId(), item.seed());
        return item.effects() == null ? npcId : npcId + NPC_SEPARATOR + effects(item.effects());
    }

    static SocketableItemData decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        if (encoded.startsWith(FROZEN_START)) {
            return decodeFrozen(encoded);
        }
        if (isNpc(encoded)) {
            return decodeNpc(encoded.substring(NPC_PREFIX.length()));
        }
        return decodeSeeded(encoded, CARGO_SEED_SEPARATOR);
    }

    static String effects(List<RolledEffect> effects) {
        StringBuilder encoded = new StringBuilder();
        for (RolledEffect effect : effects) {
            if (!encoded.isEmpty()) {
                encoded.append(EFFECT_SEPARATOR);
            }
            String magnitude = Float.toString(effect.magnitude());
            encoded.append(effect.effectName()).append(MAGNITUDE_SEPARATOR)
                    .append(magnitude.endsWith(".0") ? magnitude.substring(0, magnitude.length() - 2) : magnitude);
        }
        return encoded.toString();
    }

    static List<RolledEffect> decodeEffects(String encoded) {
        if (encoded == null) {
            return null;
        }
        List<RolledEffect> effects = new ArrayList<>();
        for (String entry : encoded.split(EFFECT_SEPARATOR)) {
            if (entry.isEmpty()) {
                continue;
            }
            int separator = entry.lastIndexOf(MAGNITUDE_SEPARATOR);
            if (separator <= 0) {
                return null;
            }
            try {
                effects.add(new RolledEffect(entry.substring(0, separator), Float.parseFloat(entry.substring(separator + 1))));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return effects;
    }

    private static String seeded(SocketableItemData item, String separator) {
        return item.definitionId() + separator + item.seed();
    }

    private static SocketableItemData decodeSeeded(String encoded, String separator) {
        int separatorAt = encoded.lastIndexOf(separator);
        if (separatorAt <= 0) {
            return null;
        }
        try {
            return new SocketableItemData(encoded.substring(0, separatorAt), Long.parseLong(encoded.substring(separatorAt + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SocketableItemData decodeNpc(String body) {
        int separator = body.lastIndexOf(NPC_SEPARATOR);
        if (separator > 0) {
            String lastSegment = body.substring(separator + 1);
            if (lastSegment.isEmpty() || lastSegment.contains(MAGNITUDE_SEPARATOR)) {
                SocketableItemData seededItem = decodeNpc(body.substring(0, separator));
                List<RolledEffect> effects = decodeEffects(lastSegment);
                return seededItem == null || effects == null ? null : new SocketableItemData(seededItem.definitionId(), seededItem.seed(), effects, null);
            }
        }
        return decodeSeeded(body, NPC_SEPARATOR);
    }

    private static SocketableItemData decodeFrozen(String encoded) {
        try {
            JSONObject frozenJson = new JSONObject(encoded);
            String definitionId = frozenJson.optString(DEFINITION_KEY, "");
            if (definitionId.isEmpty() || !frozenJson.has(SEED_KEY)) {
                return null;
            }
            List<RolledEffect> effects = frozenJson.has(EFFECTS_KEY) ? decodeEffects(frozenJson.getString(EFFECTS_KEY)) : null;
            FrozenName decodedName = effects == null || !frozenJson.optBoolean(NAMED_KEY, false) ? null : frozenName(frozenJson);
            return new SocketableItemData(definitionId, frozenJson.getLong(SEED_KEY), effects, decodedName);
        } catch (JSONException e) {
            return null;
        }
    }

    private static FrozenName frozenName(JSONObject json) {
        if (json.optBoolean(PRODUCT_KEY, false)) {
            return FrozenName.product(text(json, RARE_FIRST_KEY), text(json, RARE_BRAND_KEY), text(json, RARE_SECOND_KEY), text(json, RARE_MODEL_KEY));
        }
        return new FrozenName(text(json, PREFIX_KEY), text(json, SUFFIX_KEY), text(json, RARE_FIRST_KEY), text(json, RARE_SECOND_KEY));
    }

    private static String text(JSONObject json, String key) {
        String value = json.optString(key, "");
        return value.isEmpty() ? null : value;
    }

    private static void putText(JSONObject json, String key, String value) throws JSONException {
        if (value != null) {
            json.put(key, value);
        }
    }
}
