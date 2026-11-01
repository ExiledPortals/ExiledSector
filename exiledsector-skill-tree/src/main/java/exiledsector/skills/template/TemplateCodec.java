package exiledsector.skills.template;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.NodeEntry;
import exiledsector.skills.NodeReplacements;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class TemplateCodec {

    static final int VERSION = 1;

    private TemplateCodec() {
    }

    public static String encode(SkillTreeTemplate template) {
        JSONArray encodedSteps = new JSONArray();
        for (TemplateStep step : template.steps()) {
            encodedSteps.put(new NodeEntry(step.nodeId(), step.optionTypeId()).format());
        }
        try {
            JSONObject templateJson = new JSONObject();
            templateJson.put("v", VERSION);
            templateJson.put("id", template.id());
            templateJson.put("name", template.name());
            templateJson.put("root", template.rootNodeId());
            if (template.hullSize() != null) {
                templateJson.put("hull", template.hullSize().name());
            }
            templateJson.put("steps", encodedSteps);
            return templateJson.toString();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Template " + template.id() + " could not be encoded", e);
        }
    }

    public static SkillTreeTemplate decode(String encodedTemplate) {
        if (encodedTemplate == null) {
            return null;
        }
        JSONObject templateJson;
        try {
            templateJson = new JSONObject(encodedTemplate);
        } catch (JSONException e) {
            return null;
        }
        String templateId = templateJson.optString("id", null);
        String templateName = templateJson.optString("name", null);
        String rootNodeId = templateJson.optString("root", null);
        if (isBlank(templateId) || isBlank(templateName) || isBlank(rootNodeId)) {
            return null;
        }
        return new SkillTreeTemplate(templateId, templateName, NodeReplacements.current(rootNodeId), parseHullSize(templateJson.optString("hull", null)), parseSteps(templateJson.optJSONArray("steps")));
    }

    private static HullSize parseHullSize(String hullSizeName) {
        if (hullSizeName == null) {
            return null;
        }
        try {
            return HullSize.valueOf(hullSizeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<TemplateStep> parseSteps(JSONArray stepsArray) {
        List<TemplateStep> steps = new ArrayList<>();
        if (stepsArray == null) {
            return steps;
        }
        for (int i = 0; i < stepsArray.length(); i++) {
            String entry = stepsArray.optString(i, "").trim();
            if (entry.isEmpty()) {
                continue;
            }
            NodeEntry nodeEntry = NodeEntry.parse(entry);
            if (nodeEntry != null) {
                steps.add(new TemplateStep(nodeEntry.nodeId(), nodeEntry.optionId()));
            }
        }
        return steps;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
