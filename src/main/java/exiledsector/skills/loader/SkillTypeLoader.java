package exiledsector.skills.loader;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.HullSizeSkillEffect;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.unlock.BlueprintCategory;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionType;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTypeLoader {

    private static final String DATA_PATH = "data/skilltrees/skill_types.json";
    private static final Set<HullSize> SHIP_HULL_SIZES = EnumSet.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    private SkillTypeLoader() {
    }

    public record LoadedTypes(Map<String, SkillType> types, int declaredCount) {
    }

    public static LoadedTypes loadAll() {
        try {
            JSONObject root = Global.getSettings().loadJSON(DATA_PATH);
            JSONArray declared = root.optJSONArray("skillTypes");
            return new LoadedTypes(parseSkillTypes(root), declared == null ? 0 : declared.length());
        } catch (IOException | JSONException e) {
            Logger.getLogger(SkillTypeLoader.class).error("Failed to load " + DATA_PATH, e);
            return new LoadedTypes(new LinkedHashMap<>(), 0);
        }
    }

    public static Map<String, SkillType> parseSkillTypes(JSONObject root) throws JSONException {
        Map<String, SkillType> types = new LinkedHashMap<>();
        JSONArray typeArray = root.getJSONArray("skillTypes");
        for (int i = 0; i < typeArray.length(); i++) {
            SkillType type;
            try {
                type = parseSkillType(typeArray.getJSONObject(i));
            } catch (JSONException | IllegalArgumentException e) {
                Logger.getLogger(SkillTypeLoader.class).error("Skipping skill type " + entryLabel(typeArray, i)
                        + " in " + DATA_PATH + ": " + e.getMessage());
                continue;
            }
            if (types.containsKey(type.getId())) {
                Logger.getLogger(SkillTypeLoader.class).error("Duplicate skill type id \"" + type.getId()
                        + "\" in " + DATA_PATH + " - the earlier definition was overwritten.");
            }
            types.put(type.getId(), type);
        }
        return types;
    }

    private static SkillType parseSkillType(JSONObject json) throws JSONException {
        SkillTier tier = SkillTier.valueOf(json.optString("tier", "SMALL"));
        List<SkillTypeEffect> effects = parseEffects(json.optJSONArray("effects"));
        List<HullSizeSkillEffect> hullSizeEffects = parseHullSizeEffects(json.optJSONArray("hullSizeEffects"));
        List<String> optionalOptionIds = parseStringArray(json.optJSONArray("optionalOptions"));
        List<String> exclusiveHullModIds = parseStringArray(json.optJSONArray("exclusiveHullMods"));
        List<String> phantomHullModIds = parseStringArray(json.optJSONArray("phantomHullMods"));
        List<String> exclusiveSkillTypeIds = parseStringArray(json.optJSONArray("exclusiveSkillTypes"));
        List<UnlockCondition> unlockConditions = parseUnlockConditions(json.optJSONArray("unlockConditions"));
        List<String> tags = parseStringArray(json.optJSONArray("tags"));
        SkillItemCost itemCost = parseItemCost(json.optJSONObject("itemCost"));
        String id = json.getString("id");
        Float temporaryAfterDeploymentSeconds = validateTemporaryGating(id,
                parseTemporaryAfterDeploymentSeconds(json), effects, hullSizeEffects,
                !phantomHullModIds.isEmpty());
        Set<HullSize> requiredHullSizes = parseRequiredHullSizes(id, json.optJSONArray("requiredHullSizes"));

        return new SkillType.Builder(id, json.getString("name"), json.getString("icon"), tier)
                .effects(effects)
                .hullSizeEffects(hullSizeEffects)
                .vanillaHullModId(json.optString("vanillaHullMod", null))
                .itemCost(itemCost)
                .temporaryAfterDeploymentSeconds(temporaryAfterDeploymentSeconds)
                .requiredHullSizes(requiredHullSizes)
                .descriptionOverride(json.optString("description", null))
                .flavourOverride(json.optString("flavour", null))
                .todo(json.optString("todo", null))
                .optionalOptionIds(optionalOptionIds)
                .exclusiveHullModIds(exclusiveHullModIds)
                .phantomHullModIds(phantomHullModIds)
                .exclusiveSkillTypeIds(exclusiveSkillTypeIds)
                .unlockConditions(unlockConditions)
                .tags(tags)
                .build();
    }

    private static SkillItemCost parseItemCost(JSONObject itemCostJson) throws JSONException {
        if (itemCostJson == null) {
            return null;
        }
        return new SkillItemCost(itemCostJson.getString("itemId"), (float) itemCostJson.getDouble("quantity"));
    }

    private static Set<HullSize> parseRequiredHullSizes(String id, JSONArray array) throws JSONException {
        Set<HullSize> hullSizes = EnumSet.noneOf(HullSize.class);
        for (String name : parseStringArray(array)) {
            HullSize hullSize = SHIP_HULL_SIZES.stream().filter(size -> size.name().equals(name)).findFirst().orElse(null);
            if (hullSize == null) {
                logTypeError(id, "lists unknown hull size \"" + name + "\" in requiredHullSizes - ignoring it.");
            } else {
                hullSizes.add(hullSize);
            }
        }
        return hullSizes;
    }

    private static Float parseTemporaryAfterDeploymentSeconds(JSONObject json) throws JSONException {
        if (!json.has("temporaryAfterDeploymentSeconds")) {
            return null;
        }
        return (float) json.getDouble("temporaryAfterDeploymentSeconds");
    }

    private static Float validateTemporaryGating(String id, Float temporaryAfterDeploymentSeconds,
                                                   List<SkillTypeEffect> effects, List<HullSizeSkillEffect> hullSizeEffects,
                                                   boolean placesHullMods) {
        if (temporaryAfterDeploymentSeconds == null) {
            return null;
        }
        if (placesHullMods) {
            logTypeError(id, "sets temporaryAfterDeploymentSeconds "
                    + "but also installs hull mods, which can't be removed mid-combat - ignoring the "
                    + "temporaryAfterDeploymentSeconds field.");
            return null;
        }
        for (SkillTypeEffect effect : effects) {
            if (!effect.effect().supportsTemporaryGating()) {
                logUnsupportedTemporaryGating(id, effect.effect());
                return null;
            }
        }
        for (HullSizeSkillEffect effect : hullSizeEffects) {
            if (!effect.effect().supportsTemporaryGating()) {
                logUnsupportedTemporaryGating(id, effect.effect());
                return null;
            }
        }
        return temporaryAfterDeploymentSeconds;
    }

    private static void logUnsupportedTemporaryGating(String id, SkillEffect effect) {
        logTypeError(id, "sets temporaryAfterDeploymentSeconds "
                + "but includes effect \"" + effect.name() + "\", which doesn't support temporary gating - "
                + "ignoring the temporaryAfterDeploymentSeconds field.");
    }

    private static void logTypeError(String id, String problem) {
        Logger.getLogger(SkillTypeLoader.class).error("Skill type \"" + id + "\" " + problem);
    }

    private static List<UnlockCondition> parseUnlockConditions(JSONArray conditionsArray) throws JSONException {
        List<UnlockCondition> conditions = new ArrayList<>();
        if (conditionsArray == null) {
            return conditions;
        }
        for (int i = 0; i < conditionsArray.length(); i++) {
            conditions.add(parseUnlockCondition(conditionsArray.getJSONObject(i)));
        }
        return conditions;
    }

    private static UnlockCondition parseUnlockCondition(JSONObject json) throws JSONException {
        UnlockConditionType type = UnlockConditionType.valueOf(toEnumName(json.getString("type")));
        return switch (type) {
            case BLUEPRINT -> {
                BlueprintCategory category = BlueprintCategory.valueOf(toEnumName(json.getString("category")));
                yield UnlockCondition.blueprint(category, json.getString("id"));
            }
            case CHARACTER_STAT -> UnlockCondition.characterStat(json.getString("statId"));
            case MIN_SHIP_LEVEL -> UnlockCondition.minShipLevel(json.getInt("level"));
            case MEMORY_FLAG -> UnlockCondition.memoryFlag(json.getString("key"));
        };
    }

    private static String toEnumName(String jsonValue) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < jsonValue.length(); i++) {
            char c = jsonValue.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toUpperCase(c));
        }
        return result.toString();
    }

    static String entryLabel(JSONArray array, int index) {
        JSONObject entry = array.optJSONObject(index);
        String id = entry == null ? null : entry.optString("id", null);
        return id != null ? "\"" + id + "\"" : "#" + index;
    }

    static List<String> parseStringArray(JSONArray array) throws JSONException {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }

    private static List<SkillTypeEffect> parseEffects(JSONArray effectsArray) throws JSONException {
        List<SkillTypeEffect> effects = new ArrayList<>();
        if (effectsArray == null) {
            return effects;
        }
        for (int i = 0; i < effectsArray.length(); i++) {
            JSONObject entry = effectsArray.getJSONObject(i);
            SkillEffect effect = SkillEffect.byName(entry.getString("effect"));
            float magnitude = (float) entry.optDouble("magnitude", 0.0);
            effects.add(new SkillTypeEffect(effect, magnitude));
        }
        return effects;
    }

    private static List<HullSizeSkillEffect> parseHullSizeEffects(JSONArray effectsArray) throws JSONException {
        List<HullSizeSkillEffect> effects = new ArrayList<>();
        if (effectsArray == null) {
            return effects;
        }
        for (int i = 0; i < effectsArray.length(); i++) {
            JSONObject entry = effectsArray.getJSONObject(i);
            SkillEffect effect = SkillEffect.byName(entry.getString("effect"));
            float frigate = (float) entry.getDouble("frigate");
            float destroyer = (float) entry.getDouble("destroyer");
            float cruiser = (float) entry.getDouble("cruiser");
            float capitalShip = (float) entry.getDouble("capitalShip");
            effects.add(new HullSizeSkillEffect(effect, frigate, destroyer, cruiser, capitalShip));
        }
        return effects;
    }
}
