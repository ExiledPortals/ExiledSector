package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.loader.SkillTypeLoader;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NodeGates {

    public static final String DATA_PATH = "data/config/exiledSector/node_gates.json";
    private static final Logger LOG = Logger.getLogger(NodeGates.class);
    private static final AtomicReference<Map<String, List<UnlockCondition>>> CONDITIONS_BY_GATE_TAG = new AtomicReference<>(Map.of());

    private NodeGates() {
    }

    public static void load() {
        try {
            register(Global.getSettings().loadJSON(DATA_PATH));
        } catch (IOException | JSONException | RuntimeException e) {
            LOG.error("Couldn't load node gates from " + DATA_PATH + "; gated nodes stay open", e);
            CONDITIONS_BY_GATE_TAG.set(Map.of());
        }
    }

    public static void register(JSONObject gatesJson) throws JSONException {
        Map<String, List<UnlockCondition>> conditionsByGateTag = new LinkedHashMap<>();
        Iterator<?> gateTags = gatesJson.keys();
        while (gateTags.hasNext()) {
            String gateTag = (String) gateTags.next();
            JSONObject gateJson = gatesJson.getJSONObject(gateTag);
            conditionsByGateTag.put(gateTag, List.copyOf(SkillTypeLoader.parseUnlockConditions(gateJson.optJSONArray("unlockConditions"))));
        }
        CONDITIONS_BY_GATE_TAG.set(Map.copyOf(conditionsByGateTag));
    }

    public static Map<String, List<UnlockCondition>> all() {
        return CONDITIONS_BY_GATE_TAG.get();
    }

    public static boolean isLocked(SkillNode node, ShipSkillData shipData) {
        Map<String, List<UnlockCondition>> conditionsByGateTag = CONDITIONS_BY_GATE_TAG.get();
        if (node == null || conditionsByGateTag.isEmpty()) {
            return false;
        }
        for (String tag : node.getTags()) {
            List<UnlockCondition> conditions = conditionsByGateTag.get(tag);
            if (conditions != null && SkillTypeUnlockStatus.isLocked(conditions, shipData)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHidden(SkillNode node, ShipSkillData shipData) {
        return isLocked(node, shipData) && !HiddenNodeDisplayConfig.showHiddenNodesByDefault();
    }
}
