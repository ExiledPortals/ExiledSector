package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ItemCodec {

    public static final String NPC_PREFIX = "npc:";
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
    private static final String TIER_KEY = "t";
    private static final String QUANTITY_KEY = "q";
    private static final String CORRUPTED_KEY = "c";
    private static final String IMPLICITS_KEY = "i";
    private static final String MIRRORED_KEY = "m";
    private static final String IMMUTABLE_FLAG = "i";
    private static final String OVERCHARGED_FLAG = "o";

    private ItemCodec() {
    }

    public static boolean isNpc(String encoded) {
        return encoded != null && encoded.startsWith(NPC_PREFIX);
    }

    static String cargo(ItemData item, FrozenName frozenName) {
        if (item.explicits() == null && item.traits() == null) {
            return seeded(item, CARGO_SEED_SEPARATOR);
        }
        try {
            JSONObject cargoJson = new JSONObject().put(DEFINITION_KEY, item.definitionId()).put(SEED_KEY, item.seed());
            if (item.explicits() != null) {
                cargoJson.put(EFFECTS_KEY, modifiers(item.explicits()));
            }
            putTraits(cargoJson, item.traits());
            if (frozenName != null && item.explicits() != null) {
                cargoJson.put(NAMED_KEY, true);
                putText(cargoJson, PREFIX_KEY, frozenName.prefixModifierId());
                putText(cargoJson, SUFFIX_KEY, frozenName.suffixModifierId());
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

    public static String npc(String definitionId, long seed) {
        return NPC_PREFIX + definitionId + NPC_SEPARATOR + seed;
    }

    public static String npc(ItemData item) {
        String npcId = npc(item.definitionId(), item.seed());
        return item.explicits() == null ? npcId : npcId + NPC_SEPARATOR + modifiers(item.explicits());
    }

    public static ItemData decode(String encoded) {
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

    public static String modifiers(List<RolledModifier> modifiers) {
        StringBuilder encoded = new StringBuilder();
        for (RolledModifier modifier : modifiers) {
            if (!encoded.isEmpty()) {
                encoded.append(EFFECT_SEPARATOR);
            }
            encoded.append(slotCode(modifier.slot())).append(flags(modifier)).append(MODIFIER_FIELD_SEPARATOR);
            if (!isCompact(modifier)) {
                encoded.append(modifier.modifierId()).append(MODIFIER_FIELD_SEPARATOR);
            }
            boolean firstLine = true;
            for (RolledLine line : modifier.lines()) {
                if (!firstLine) {
                    encoded.append(LINE_SEPARATOR);
                }
                firstLine = false;
                encoded.append(effect(line));
            }
        }
        return encoded.toString();
    }

    private static String flags(RolledModifier modifier) {
        return (modifier.immutable() ? IMMUTABLE_FLAG : "") + (modifier.overcharged() ? OVERCHARGED_FLAG : "");
    }

    private static boolean isCompact(RolledModifier modifier) {
        return modifier.lines().size() == 1 && modifier.lines().get(0).effectName().equals(modifier.modifierId());
    }

    private static String effect(RolledLine effect) {
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

    public static List<RolledModifier> decodeModifiers(String encoded, String definitionId) {
        if (encoded == null) {
            return null;
        }
        List<RolledModifier> modifiers = new ArrayList<>();
        List<RolledLine> legacyEffects = new ArrayList<>();
        for (String entry : encoded.split(EFFECT_SEPARATOR)) {
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.contains(MODIFIER_FIELD_SEPARATOR)) {
                RolledModifier modifier = decodeModifier(entry);
                if (modifier == null) {
                    return null;
                }
                modifiers.add(modifier);
            } else {
                RolledLine effect = decodeEffect(entry);
                if (effect == null) {
                    return null;
                }
                legacyEffects.add(effect);
            }
        }
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        modifiers.addAll(RolledModifier.fromLegacyEffects(definition, legacyEffects));
        return definition == null ? modifiers : definition.withCurrentIds(modifiers);
    }

    private static RolledModifier decodeModifier(String entry) {
        String[] fields = entry.split(MODIFIER_FIELD_SEPARATOR, 3);
        ModifierSlot slot = fields[0].isEmpty() ? null : slotOf(fields[0].substring(0, 1));
        if (slot == null) {
            return null;
        }
        String flags = fields[0].substring(1);
        boolean immutable = flags.contains(IMMUTABLE_FLAG);
        boolean overcharged = flags.contains(OVERCHARGED_FLAG);
        if (fields.length == 2) {
            RolledLine line = decodeEffect(fields[1]);
            return line == null ? null : new RolledModifier(line.effectName(), slot, List.of(line), immutable, overcharged);
        }
        if (!ItemModifiers.ID_PATTERN.matcher(fields[1]).matches()) {
            return null;
        }
        List<RolledLine> lines = new ArrayList<>();
        for (String lineText : fields[2].split(Pattern.quote(LINE_SEPARATOR))) {
            RolledLine line = decodeEffect(lineText);
            if (line == null) {
                return null;
            }
            lines.add(line);
        }
        return new RolledModifier(RolledLine.currentName(fields[1]), slot, lines, immutable, overcharged);
    }

    private static RolledLine decodeEffect(String entry) {
        int separator = entry.lastIndexOf(MAGNITUDE_SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        try {
            return new RolledLine(entry.substring(0, separator), Float.parseFloat(entry.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String seeded(ItemData item, String separator) {
        return item.definitionId() + separator + item.seed();
    }

    private static ItemData decodeSeeded(String encoded, String separator) {
        int separatorAt = encoded.lastIndexOf(separator);
        if (separatorAt <= 0) {
            return null;
        }
        try {
            return new ItemData(encoded.substring(0, separatorAt), Long.parseLong(encoded.substring(separatorAt + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ItemData decodeNpc(String body) {
        int separator = body.lastIndexOf(NPC_SEPARATOR);
        if (separator > 0) {
            String lastSegment = body.substring(separator + 1);
            if (lastSegment.isEmpty() || lastSegment.contains(MAGNITUDE_SEPARATOR)) {
                ItemData seededItem = decodeNpc(body.substring(0, separator));
                if (seededItem == null) {
                    return null;
                }
                List<RolledModifier> modifiers = decodeModifiers(lastSegment, seededItem.definitionId());
                return modifiers == null ? null : new ItemData(seededItem.definitionId(), seededItem.seed(), modifiers, null);
            }
        }
        return decodeSeeded(body, NPC_SEPARATOR);
    }

    private static ItemData decodeFrozen(String encoded) {
        try {
            JSONObject frozenJson = new JSONObject(encoded);
            String definitionId = frozenJson.optString(DEFINITION_KEY, "");
            if (definitionId.isEmpty() || !frozenJson.has(SEED_KEY)) {
                return null;
            }
            List<RolledModifier> modifiers = frozenJson.has(EFFECTS_KEY) ? decodeModifiers(frozenJson.getString(EFFECTS_KEY), definitionId) : null;
            FrozenName decodedName = modifiers == null || !frozenJson.optBoolean(NAMED_KEY, false) ? null : frozenName(frozenJson);
            return new ItemData(definitionId, frozenJson.getLong(SEED_KEY), modifiers, decodedName, traits(frozenJson, definitionId));
        } catch (JSONException e) {
            return null;
        }
    }

    private static void putTraits(JSONObject json, ItemTraits traits) throws JSONException {
        if (traits == null) {
            return;
        }
        json.put(TIER_KEY, traits.tier());
        if (traits.quantity() != 0) {
            json.put(QUANTITY_KEY, traits.quantity());
        }
        if (traits.corruption() != null) {
            json.put(CORRUPTED_KEY, traits.corruption().name());
        }
        if (traits.implicits() != null) {
            json.put(IMPLICITS_KEY, modifiers(traits.implicits()));
        }
        if (traits.mirrored()) {
            json.put(MIRRORED_KEY, true);
        }
    }

    private static ItemTraits traits(JSONObject json, String definitionId) throws JSONException {
        if (!json.has(TIER_KEY) && !json.has(QUANTITY_KEY) && !json.has(CORRUPTED_KEY) && !json.has(IMPLICITS_KEY) && !json.has(MIRRORED_KEY)) {
            return null;
        }
        List<RolledModifier> implicits = json.has(IMPLICITS_KEY) ? decodeModifiers(json.getString(IMPLICITS_KEY), definitionId) : null;
        return new ItemTraits(json.optInt(TIER_KEY, ItemTraits.LOWEST_TIER), json.optInt(QUANTITY_KEY, 0), corruption(json), implicits,
                json.optBoolean(MIRRORED_KEY, false));
    }

    private static CorruptionOutcome corruption(JSONObject json) {
        Object stored = json.opt(CORRUPTED_KEY);
        if (stored == null || Boolean.FALSE.equals(stored)) {
            return null;
        }
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            if (outcome.name().equals(String.valueOf(stored))) {
                return outcome;
            }
        }
        return CorruptionOutcome.NOTHING;
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
