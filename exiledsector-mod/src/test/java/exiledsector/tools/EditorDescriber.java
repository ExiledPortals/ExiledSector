package exiledsector.tools;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.Catalogue;
import exiledlib.i18n.I18n;
import exiledlib.i18n.LocaleChain;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartEffects;
import exiledlib.items.ItemEffects;
import exiledlib.items.ItemType;
import exiledlib.items.RolledEffect;
import exiledlib.modifiers.ModifierEffects;
import exiledlib.modifiers.ModifierLine;
import exiledlib.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.loader.SkillTypeLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EditorDescriber {

    private String registeredTypesText;

    public String respond(String requestLine) {
        try {
            JSONObject request = new JSONObject(requestLine);
            JSONObject strings = request.optJSONObject("strings");
            return strings == null ? describe(request).toString() : describeWithStrings(request, strings).toString();
        } catch (JSONException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    private JSONObject describeWithStrings(JSONObject request, JSONObject strings) throws JSONException {
        Catalogue previousCatalogue = I18n.catalogue();
        Catalogue requestCatalogue = new Catalogue(LocaleChain.ENGLISH, I18n.flatten(strings));
        I18n.install(requestCatalogue);
        try {
            JSONObject response = describe(request);
            return response.put("keys", new JSONArray(requestCatalogue.requestedKeys()));
        } finally {
            I18n.install(previousCatalogue);
        }
    }

    private JSONObject describe(JSONObject request) throws JSONException {
        JSONArray modifierLines = request.optJSONArray("modifierLines");
        if (modifierLines != null) {
            return describeModifierLines(modifierLines);
        }
        registerTypes(request.getJSONObject("types"));
        SkillType type = SkillTree.getType(request.getString("typeId"));
        if (type == null) {
            return new JSONObject().put("error", "Unknown or invalid type \"" + request.getString("typeId") + "\"");
        }
        HullSize hullSize = request.isNull("hullSize") ? null : HullSize.valueOf(request.getString("hullSize"));
        boolean optionalHint = type.isOptional() && type.getDescriptionOverride() == null;
        NodeDescription description = optionalHint
                ? new NodeDescription(List.of(new DescriptionLine(Translation.styled("ui.node.optionalHint"), false)), List.of())
                : SkillNode.describeType(type, hullSize);
        return new JSONObject()
                .put("title", type.getDisplayName())
                .put("flavour", type.getFlavourText() == null ? JSONObject.NULL : type.getFlavourText())
                .put("effects", markup(description.effects()))
                .put("details", markup(description.details()))
                .put("expandHint", Translation.text("ui.tooltip.expandHint"));
    }

    private static JSONObject describeModifierLines(JSONArray modifierLines) throws JSONException {
        JSONArray described = new JSONArray();
        for (int index = 0; index < modifierLines.length(); index++) {
            StyledText lineText = describeModifierLine(modifierLines.getJSONObject(index));
            described.put(lineText == null ? JSONObject.NULL : lineText.toMarkup());
        }
        return new JSONObject().put("lines", described);
    }

    private static StyledText describeModifierLine(JSONObject lineJson) throws JSONException {
        String effectName = lineJson.getString("effect");
        JSONArray hullValuesJson = lineJson.optJSONArray("hullValues");
        JSONArray tierBandsJson = lineJson.optJSONArray("tierBands");
        ModifierLine line;
        if (tierBandsJson != null) {
            List<Integer> bandStartTiers = new ArrayList<>();
            List<Float> bandLowValues = new ArrayList<>();
            List<Float> bandHighValues = new ArrayList<>();
            for (int index = 0; index < tierBandsJson.length(); index++) {
                JSONObject band = tierBandsJson.getJSONObject(index);
                bandStartTiers.add(band.getInt("tier"));
                bandLowValues.add((float) band.getDouble("min"));
                bandHighValues.add((float) band.getDouble("max"));
            }
            line = ModifierLine.perTierBandRanges(effectName, bandStartTiers, bandLowValues, bandHighValues);
        } else if (hullValuesJson != null) {
            List<Float> hullValues = new ArrayList<>();
            for (int index = 0; index < hullValuesJson.length(); index++) {
                hullValues.add((float) hullValuesJson.getDouble(index));
            }
            line = ModifierLine.perHullSize(effectName, hullValues);
        } else {
            line = ModifierLine.range(effectName, (float) lineJson.getDouble("min"), (float) lineJson.getDouble("max"));
        }
        return new RolledEffect(effectName, line.boundFarthestFromZero()).rangeDescription(effectsFor(lineJson.optString("itemType", null)), line);
    }

    private static ModifierEffects effectsFor(String itemTypeId) {
        ItemType itemType = itemTypeId == null ? null : ItemType.byIdOrNull(itemTypeId);
        if (itemType == ItemType.STAR_CHART) {
            return ChartEffects.RESOLVER;
        }
        return ItemEffects.forType(itemType);
    }

    private void registerTypes(JSONObject typesJson) throws JSONException {
        String typesText = typesJson.toString();
        if (typesText.equals(registeredTypesText)) {
            return;
        }
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(typesJson);
        SkillTree.clearTypes();
        types.values().forEach(SkillTree::registerType);
        registeredTypesText = typesText;
    }

    private static JSONArray markup(List<DescriptionLine> lines) {
        JSONArray markup = new JSONArray();
        for (DescriptionLine line : lines) {
            markup.put(line.display().toMarkup());
        }
        return markup;
    }

    private static String error(String message) {
        try {
            return new JSONObject().put("error", message == null ? "Could not describe this type" : message).toString();
        } catch (JSONException e) {
            return "{\"error\":\"Could not describe this type\"}";
        }
    }
}
