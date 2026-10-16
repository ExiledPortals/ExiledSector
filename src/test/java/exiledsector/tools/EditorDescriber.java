package exiledsector.tools;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.loader.SkillTypeLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
