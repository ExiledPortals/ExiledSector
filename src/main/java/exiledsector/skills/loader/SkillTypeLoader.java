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

    public record LoadedTypes(Map<String, SkillType> typesById, int declaredTypeCount) {
    }

    public static LoadedTypes loadAll() {
        try {
            JSONObject typesJson = Global.getSettings().loadJSON(DATA_PATH);
            JSONArray declaredTypes = typesJson.optJSONArray("skillTypes");
            return new LoadedTypes(parseSkillTypes(typesJson), declaredTypes == null ? 0 : declaredTypes.length());
        } catch (IOException | JSONException e) {
            Logger.getLogger(SkillTypeLoader.class).error("Failed to load " + DATA_PATH, e);
            return new LoadedTypes(new LinkedHashMap<>(), 0);
        }
    }

    public static Map<String, SkillType> parseSkillTypes(JSONObject typesJson) throws JSONException {
        Map<String, SkillType> typesById = new LinkedHashMap<>();
        JSONArray typeArray = typesJson.getJSONArray("skillTypes");
        for (int i = 0; i < typeArray.length(); i++) {
            SkillType skillType;
            try {
                skillType = parseSkillType(typeArray.getJSONObject(i));
            } catch (JSONException | IllegalArgumentException e) {
                Logger.getLogger(SkillTypeLoader.class).error("Skipping skill type " + entryLabel(typeArray, i)
                        + " in " + DATA_PATH + ": " + e.getMessage());
                continue;
            }
            if (typesById.containsKey(skillType.getId())) {
                Logger.getLogger(SkillTypeLoader.class).error("Duplicate skill type id \"" + skillType.getId()
                        + "\" in " + DATA_PATH + " - the earlier definition was overwritten.");
            }
            typesById.put(skillType.getId(), skillType);
        }
        return typesById;
    }

    private static SkillType parseSkillType(JSONObject typeJson) throws JSONException {
        SkillTier tier = SkillTier.valueOf(typeJson.optString("tier", "SMALL"));
        List<SkillTypeEffect> effects = parseEffects(typeJson.optJSONArray("effects"));
        List<HullSizeSkillEffect> hullSizeEffects = parseHullSizeEffects(typeJson.optJSONArray("hullSizeEffects"));
        List<String> optionalOptionIds = parseStringArray(typeJson.optJSONArray("optionalOptions"));
        List<String> exclusiveHullModIds = parseStringArray(typeJson.optJSONArray("exclusiveHullMods"));
        List<String> phantomHullModIds = parseStringArray(typeJson.optJSONArray("phantomHullMods"));
        List<String> exclusiveSkillTypeIds = parseStringArray(typeJson.optJSONArray("exclusiveSkillTypes"));
        List<UnlockCondition> unlockConditions = parseUnlockConditions(typeJson.optJSONArray("unlockConditions"));
        List<String> tags = parseStringArray(typeJson.optJSONArray("tags"));
        SkillItemCost itemCost = parseItemCost(typeJson.optJSONObject("itemCost"));
        String typeId = typeJson.getString("id");
        Float temporaryAfterDeploymentSeconds = validateTemporaryGating(typeId,
                parseTemporaryAfterDeploymentSeconds(typeJson), effects, hullSizeEffects,
                !phantomHullModIds.isEmpty());
        Set<HullSize> requiredHullSizes = parseRequiredHullSizes(typeId, typeJson.optJSONArray("requiredHullSizes"));

        return new SkillType.Builder(typeId, typeJson.getString("name"), typeJson.getString("icon"), tier)
                .effects(effects)
                .hullSizeEffects(hullSizeEffects)
                .vanillaHullModId(typeJson.optString("vanillaHullMod", null))
                .itemCost(itemCost)
                .temporaryAfterDeploymentSeconds(temporaryAfterDeploymentSeconds)
                .requiredHullSizes(requiredHullSizes)
                .descriptionOverride(typeJson.optString("description", null))
                .flavourOverride(typeJson.optString("flavour", null))
                .todo(typeJson.optString("todo", null))
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

    private static Set<HullSize> parseRequiredHullSizes(String typeId, JSONArray hullSizesArray) throws JSONException {
        Set<HullSize> hullSizes = EnumSet.noneOf(HullSize.class);
        for (String hullSizeName : parseStringArray(hullSizesArray)) {
            HullSize hullSize = SHIP_HULL_SIZES.stream().filter(size -> size.name().equals(hullSizeName)).findFirst().orElse(null);
            if (hullSize == null) {
                logTypeError(typeId, "lists unknown hull size \"" + hullSizeName + "\" in requiredHullSizes - ignoring it.");
            } else {
                hullSizes.add(hullSize);
            }
        }
        return hullSizes;
    }

    private static Float parseTemporaryAfterDeploymentSeconds(JSONObject typeJson) throws JSONException {
        if (!typeJson.has("temporaryAfterDeploymentSeconds")) {
            return null;
        }
        return (float) typeJson.getDouble("temporaryAfterDeploymentSeconds");
    }

    private static Float validateTemporaryGating(String typeId, Float temporaryAfterDeploymentSeconds,
                                                   List<SkillTypeEffect> effects, List<HullSizeSkillEffect> hullSizeEffects,
                                                   boolean placesHullMods) {
        if (temporaryAfterDeploymentSeconds == null) {
            return null;
        }
        if (placesHullMods) {
            logTypeError(typeId, "sets temporaryAfterDeploymentSeconds "
                    + "but also installs hull mods, which can't be removed mid-combat - ignoring the "
                    + "temporaryAfterDeploymentSeconds field.");
            return null;
        }
        for (SkillTypeEffect effect : effects) {
            if (!effect.effect().supportsTemporaryGating()) {
                logUnsupportedTemporaryGating(typeId, effect.effect());
                return null;
            }
        }
        for (HullSizeSkillEffect effect : hullSizeEffects) {
            if (!effect.effect().supportsTemporaryGating()) {
                logUnsupportedTemporaryGating(typeId, effect.effect());
                return null;
            }
        }
        return temporaryAfterDeploymentSeconds;
    }

    private static void logUnsupportedTemporaryGating(String typeId, SkillEffect effect) {
        logTypeError(typeId, "sets temporaryAfterDeploymentSeconds "
                + "but includes effect \"" + effect.name() + "\", which doesn't support temporary gating - "
                + "ignoring the temporaryAfterDeploymentSeconds field.");
    }

    private static void logTypeError(String typeId, String problem) {
        Logger.getLogger(SkillTypeLoader.class).error("Skill type \"" + typeId + "\" " + problem);
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

    private static UnlockCondition parseUnlockCondition(JSONObject conditionJson) throws JSONException {
        UnlockConditionType conditionType = UnlockConditionType.valueOf(toEnumName(conditionJson.getString("type")));
        return switch (conditionType) {
            case BLUEPRINT -> {
                BlueprintCategory category = BlueprintCategory.valueOf(toEnumName(conditionJson.getString("category")));
                yield UnlockCondition.blueprint(category, conditionJson.getString("id"));
            }
            case CHARACTER_STAT -> UnlockCondition.characterStat(conditionJson.getString("statId"));
            case MIN_SHIP_LEVEL -> UnlockCondition.minShipLevel(conditionJson.getInt("level"));
            case MEMORY_FLAG -> UnlockCondition.memoryFlag(conditionJson.getString("key"));
        };
    }

    private static String toEnumName(String jsonValue) {
        StringBuilder enumName = new StringBuilder();
        for (int i = 0; i < jsonValue.length(); i++) {
            char c = jsonValue.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                enumName.append('_');
            }
            enumName.append(Character.toUpperCase(c));
        }
        return enumName.toString();
    }

    static String entryLabel(JSONArray entries, int entryIndex) {
        JSONObject entry = entries.optJSONObject(entryIndex);
        String entryId = entry == null ? null : entry.optString("id", null);
        return entryId != null ? "\"" + entryId + "\"" : "#" + entryIndex;
    }

    static List<String> parseStringArray(JSONArray stringArray) throws JSONException {
        List<String> values = new ArrayList<>();
        if (stringArray == null) {
            return values;
        }
        for (int i = 0; i < stringArray.length(); i++) {
            values.add(stringArray.getString(i));
        }
        return values;
    }

    private static List<SkillTypeEffect> parseEffects(JSONArray effectsArray) throws JSONException {
        List<SkillTypeEffect> effects = new ArrayList<>();
        if (effectsArray == null) {
            return effects;
        }
        for (int i = 0; i < effectsArray.length(); i++) {
            JSONObject effectJson = effectsArray.getJSONObject(i);
            SkillEffect effect = SkillEffect.byName(effectJson.getString("effect"));
            float magnitude = (float) effectJson.optDouble("magnitude", 0.0);
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
            JSONObject effectJson = effectsArray.getJSONObject(i);
            SkillEffect effect = SkillEffect.byName(effectJson.getString("effect"));
            float frigate = (float) effectJson.getDouble("frigate");
            float destroyer = (float) effectJson.getDouble("destroyer");
            float cruiser = (float) effectJson.getDouble("cruiser");
            float capitalShip = (float) effectJson.getDouble("capitalShip");
            effects.add(new HullSizeSkillEffect(effect, frigate, destroyer, cruiser, capitalShip));
        }
        return effects;
    }
}
