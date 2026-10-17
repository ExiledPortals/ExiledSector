package exiledsector.hullitems;

import exiledsector.modifiers.ModifierSlot;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class HullItemCodec {

    static final String NPC_PREFIX = "npc:";
    private static final String FROZEN_START = "{";
    private static final String CARGO_SEED_SEPARATOR = "|";
    private static final String NPC_SEPARATOR = "/";
    private static final String EFFECT_SEPARATOR = ";";
    private static final String MAGNITUDE_SEPARATOR = ":";
    private static final String MODIFIER_FIELD_SEPARATOR = "=";
    private static final String LINE_SEPARATOR = "+";
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

    private HullItemCodec() {
    }

    static boolean isNpc(String encoded) {
        return encoded != null && encoded.startsWith(NPC_PREFIX);
    }

    static String cargo(HullItemData item, FrozenName frozenName) {
        if (item.explicits() == null) {
            return seeded(item, CARGO_SEED_SEPARATOR);
        }
        try {
            JSONObject cargoJson = new JSONObject().put(DEFINITION_KEY, item.definitionId()).put(SEED_KEY, item.seed())
                    .put(EFFECTS_KEY, modifiers(item.explicits()));
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

    static String npc(HullItemData item) {
        String npcId = npc(item.definitionId(), item.seed());
        return item.explicits() == null ? npcId : npcId + NPC_SEPARATOR + modifiers(item.explicits());
    }

    static HullItemData decode(String encoded) {
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

    static String modifiers(List<RolledModifier> modifiers) {
        StringBuilder encoded = new StringBuilder();
        for (RolledModifier modifier : modifiers) {
            if (!encoded.isEmpty()) {
                encoded.append(EFFECT_SEPARATOR);
            }
            encoded.append(slotCode(modifier.slot())).append(MODIFIER_FIELD_SEPARATOR);
            if (!isCompact(modifier)) {
                encoded.append(modifier.modifierId()).append(MODIFIER_FIELD_SEPARATOR);
            }
            boolean firstLine = true;
            for (RolledEffect line : modifier.lines()) {
                if (!firstLine) {
                    encoded.append(LINE_SEPARATOR);
                }
                firstLine = false;
                encoded.append(effect(line));
            }
        }
        return encoded.toString();
    }

    private static boolean isCompact(RolledModifier modifier) {
        return modifier.lines().size() == 1 && modifier.lines().get(0).effectName().equals(modifier.modifierId());
    }

    private static String effect(RolledEffect effect) {
        String magnitude = Float.toString(effect.magnitude());
        return effect.effectName() + MAGNITUDE_SEPARATOR + (magnitude.endsWith(".0") ? magnitude.substring(0, magnitude.length() - 2) : magnitude);
    }

    private static String slotCode(ModifierSlot slot) {
        return slot.name().substring(0, 1);
    }

    private static ModifierSlot slotOf(String code) {
        for (ModifierSlot slot : ModifierSlot.values()) {
            if (slotCode(slot).equals(code)) {
                return slot;
            }
        }
        return null;
    }

    static List<RolledModifier> decodeModifiers(String encoded, String definitionId) {
        if (encoded == null) {
            return null;
        }
        HullItemDefinition definition = HullItemDefinitions.get(definitionId);
        List<RolledModifier> modifiers = new ArrayList<>();
        for (String entry : encoded.split(EFFECT_SEPARATOR)) {
            if (entry.isEmpty()) {
                continue;
            }
            RolledModifier modifier = entry.contains(MODIFIER_FIELD_SEPARATOR) ? decodeModifier(entry) : decodeLegacy(entry, definition);
            if (modifier == null) {
                return null;
            }
            modifiers.add(modifier);
        }
        return modifiers;
    }

    private static RolledModifier decodeModifier(String entry) {
        String[] fields = entry.split(MODIFIER_FIELD_SEPARATOR, 3);
        ModifierSlot slot = slotOf(fields[0]);
        if (slot == null) {
            return null;
        }
        if (fields.length == 2) {
            RolledEffect line = decodeEffect(fields[1]);
            return line == null ? null : RolledModifier.single(slot, line);
        }
        if (fields[1].isEmpty()) {
            return null;
        }
        List<RolledEffect> lines = new ArrayList<>();
        for (String lineText : fields[2].split(Pattern.quote(LINE_SEPARATOR))) {
            RolledEffect line = decodeEffect(lineText);
            if (line == null) {
                return null;
            }
            lines.add(line);
        }
        return new RolledModifier(fields[1], slot, lines);
    }

    private static RolledModifier decodeLegacy(String entry, HullItemDefinition definition) {
        RolledEffect effect = decodeEffect(entry);
        return effect == null ? null : RolledModifier.fromLegacyEffects(definition, List.of(effect)).get(0);
    }

    private static RolledEffect decodeEffect(String entry) {
        int separator = entry.lastIndexOf(MAGNITUDE_SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        try {
            return new RolledEffect(entry.substring(0, separator), Float.parseFloat(entry.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String seeded(HullItemData item, String separator) {
        return item.definitionId() + separator + item.seed();
    }

    private static HullItemData decodeSeeded(String encoded, String separator) {
        int separatorAt = encoded.lastIndexOf(separator);
        if (separatorAt <= 0) {
            return null;
        }
        try {
            return new HullItemData(encoded.substring(0, separatorAt), Long.parseLong(encoded.substring(separatorAt + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static HullItemData decodeNpc(String body) {
        int separator = body.lastIndexOf(NPC_SEPARATOR);
        if (separator > 0) {
            String lastSegment = body.substring(separator + 1);
            if (lastSegment.isEmpty() || lastSegment.contains(MAGNITUDE_SEPARATOR)) {
                HullItemData seededItem = decodeNpc(body.substring(0, separator));
                if (seededItem == null) {
                    return null;
                }
                List<RolledModifier> modifiers = decodeModifiers(lastSegment, seededItem.definitionId());
                return modifiers == null ? null : new HullItemData(seededItem.definitionId(), seededItem.seed(), modifiers, null);
            }
        }
        return decodeSeeded(body, NPC_SEPARATOR);
    }

    private static HullItemData decodeFrozen(String encoded) {
        try {
            JSONObject frozenJson = new JSONObject(encoded);
            String definitionId = frozenJson.optString(DEFINITION_KEY, "");
            if (definitionId.isEmpty() || !frozenJson.has(SEED_KEY)) {
                return null;
            }
            List<RolledModifier> modifiers = frozenJson.has(EFFECTS_KEY) ? decodeModifiers(frozenJson.getString(EFFECTS_KEY), definitionId) : null;
            FrozenName decodedName = modifiers == null || !frozenJson.optBoolean(NAMED_KEY, false) ? null : frozenName(frozenJson);
            return new HullItemData(definitionId, frozenJson.getLong(SEED_KEY), modifiers, decodedName);
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
