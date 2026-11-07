package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.loader.SkillTypeLoader;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class NodeGates {

    public static final String DATA_PATH = "data/config/exiledSector/node_gates.json";
    static final String ANCHORS_FIELD = "anchors";
    static final String REWARD_ANCHOR_FIELD = "rewardAnchor";
    static final String CONCEALED_FIELD = "concealed";
    private static final Logger LOG = Logger.getLogger(NodeGates.class);
    private static final AtomicReference<Map<String, Gate>> GATES_BY_TAG = new AtomicReference<>(Map.of());

    public enum AnchorRule {
        WITHHELD("withheld"), FREE("free");

        private final String id;

        AnchorRule(String id) {
            this.id = id;
        }

        static AnchorRule parse(String gateTag, String ruleId) {
            if (ruleId == null || ruleId.isEmpty()) {
                return WITHHELD;
            }
            for (AnchorRule rule : values()) {
                if (rule.id.equals(ruleId)) {
                    return rule;
                }
            }
            LOG.error("Unknown " + ANCHORS_FIELD + " rule '" + ruleId + "' on " + gateTag + " in " + DATA_PATH + "; withholding its anchors");
            return WITHHELD;
        }
    }

    public record Gate(List<UnlockCondition> conditions, AnchorRule anchorRule, boolean rewardAnchor, boolean concealed) {

        public Gate {
            conditions = List.copyOf(conditions);
        }
    }

    private NodeGates() {
    }

    public static void load() {
        try {
            register(Global.getSettings().loadJSON(DATA_PATH));
        } catch (IOException | JSONException | RuntimeException e) {
            LOG.error("Couldn't load node gates from " + DATA_PATH + "; gated nodes stay open", e);
            GATES_BY_TAG.set(Map.of());
        }
    }

    public static void register(JSONObject gatesJson) {
        Map<String, Gate> gatesByTag = new LinkedHashMap<>();
        Iterator<?> gateTags = gatesJson.keys();
        while (gateTags.hasNext()) {
            String gateTag = (String) gateTags.next();
            try {
                JSONObject gateJson = gatesJson.getJSONObject(gateTag);
                gatesByTag.put(gateTag, new Gate(SkillTypeLoader.parseUnlockConditions(gateJson.optJSONArray("unlockConditions")),
                        AnchorRule.parse(gateTag, gateJson.optString(ANCHORS_FIELD, "")), gateJson.optBoolean(REWARD_ANCHOR_FIELD, false),
                        gateJson.optBoolean(CONCEALED_FIELD, false)));
            } catch (JSONException | RuntimeException e) {
                LOG.error("Skipping malformed gate " + gateTag + " in " + DATA_PATH + "; its nodes stay open", e);
            }
        }
        GATES_BY_TAG.set(Collections.unmodifiableMap(gatesByTag));
    }

    public static Map<String, List<UnlockCondition>> all() {
        Map<String, List<UnlockCondition>> conditionsByGateTag = new LinkedHashMap<>();
        GATES_BY_TAG.get().forEach((gateTag, gate) -> conditionsByGateTag.put(gateTag, gate.conditions()));
        return Collections.unmodifiableMap(conditionsByGateTag);
    }

    public static Gate gate(String gateTag) {
        return gateTag == null ? null : GATES_BY_TAG.get().get(gateTag);
    }

    public static boolean isLocked(SkillNode node, ShipSkillData shipData) {
        return lockingGateTag(node, shipData) != null;
    }

    public static String lockingGateTag(SkillNode node, ShipSkillData shipData) {
        return lockingGateTag(node, shipData, gate -> true);
    }

    public static boolean withholdsAnchor(SkillNode exitNode) {
        return lockingGateTag(exitNode, null, gate -> gate.anchorRule() == AnchorRule.WITHHELD) != null;
    }

    public static boolean isHidden(SkillNode node, ShipSkillData shipData) {
        return isLocked(node, shipData) && !HiddenNodeDisplayConfig.showHiddenNodesByDefault();
    }

    public static boolean isConcealed(SkillNode node, ShipSkillData shipData) {
        return lockingGateTag(node, shipData, Gate::concealed) != null && !HiddenNodeDisplayConfig.showHiddenNodesByDefault();
    }

    private static String lockingGateTag(SkillNode node, ShipSkillData shipData, Predicate<Gate> gateFilter) {
        Map<String, Gate> gatesByTag = GATES_BY_TAG.get();
        if (node == null || gatesByTag.isEmpty()) {
            return null;
        }
        for (String tag : node.getTags()) {
            Gate gate = gatesByTag.get(tag);
            if (gate != null && gateFilter.test(gate) && SkillTypeUnlockStatus.isLocked(gate.conditions(), shipData)) {
                return tag;
            }
        }
        return null;
    }
}
