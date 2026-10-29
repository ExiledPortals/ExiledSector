package exiledsector.ui.node;

import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class SocketWormholes {

    static final float MORPH_SECONDS = 3f;

    private final Map<String, Float> morphBySocketId = new HashMap<>();
    private final Map<String, Float> opennessBySocketId = new HashMap<>();
    private final Map<String, SkillNode> exitBySocketId = new HashMap<>();
    private boolean primed;

    void advance(float amount, ShipSkillData skillData) {
        Map<String, String> linkedExits = ShipTreeLinks.anchorExitsBySocket(skillData);
        for (Map.Entry<String, String> link : linkedExits.entrySet()) {
            SkillNode exit = SkillTree.get(link.getValue());
            if (exit != null && !ExoshipNodes.isExoship(exit)) {
                exitBySocketId.put(link.getKey(), exit);
                morphBySocketId.putIfAbsent(link.getKey(), primed ? 0f : 1f);
            }
        }
        primed = true;
        float morphStep = amount / MORPH_SECONDS;
        float openStep = amount / WormholeOpenness.OPEN_SECONDS;
        Iterator<Map.Entry<String, Float>> morphs = morphBySocketId.entrySet().iterator();
        while (morphs.hasNext()) {
            Map.Entry<String, Float> morph = morphs.next();
            String socketId = morph.getKey();
            SkillNode exit = exitBySocketId.get(socketId);
            boolean linked = exit != null && exit.getId().equals(linkedExits.get(socketId));
            float nextMorph = toward(morph.getValue(), linked ? 1f : 0f, morphStep);
            float targetOpenness = linked && skillData.isAllocated(exit.getId()) ? 1f : 0f;
            float nextOpenness = toward(opennessBySocketId.getOrDefault(socketId, targetOpenness), targetOpenness, openStep);
            if (nextMorph <= 0f && !linked) {
                morphs.remove();
                opennessBySocketId.remove(socketId);
                exitBySocketId.remove(socketId);
                continue;
            }
            morph.setValue(nextMorph);
            opennessBySocketId.put(socketId, nextOpenness);
        }
    }

    float morph(String socketId) {
        Float morph = morphBySocketId.get(socketId);
        return morph == null ? 0f : Fade.smooth(morph);
    }

    float openness(String socketId) {
        return opennessBySocketId.getOrDefault(socketId, 0f);
    }

    SkillNode exit(String socketId) {
        return exitBySocketId.get(socketId);
    }

    private static float toward(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }
}
