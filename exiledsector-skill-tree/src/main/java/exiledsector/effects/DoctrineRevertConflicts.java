package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.items.DoctrineLayer;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.DoctrineRewrite;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.HullModNames;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class DoctrineRevertConflicts {

    private DoctrineRevertConflicts() {
    }

    public static List<String> hullModNamesLostOnRemoval(ShipSkillData data, ShipVariantAPI variant, String socketNodeId) {
        List<String> names = new ArrayList<>();
        if (variant == null) {
            return names;
        }
        for (String hullModId : hullModsLostOnRemoval(data, socketNodeId, hullModId -> HullModConflictResolver.isRemovableConflict(variant, hullModId))) {
            names.add(HullModNames.displayName(hullModId));
        }
        return names;
    }

    public static Set<String> hullModsLostOnRemoval(ShipSkillData data, String socketNodeId, Predicate<String> removableInstalledHullMod) {
        Set<String> lostHullModIds = new LinkedHashSet<>();
        DoctrineLayer layer = DoctrineOverrides.layer(data);
        if (data == null || layer.isEmpty() || socketNodeId == null || !socketNodeId.equals(layer.socketNodeId())) {
            return lostHullModIds;
        }
        for (DoctrineRewrite rewrite : layer.rewrites().values()) {
            SkillNode node = SkillTree.get(rewrite.nodeId());
            if (!rewrite.transformed() || node == null || !data.isAllocated(node.getId())) {
                continue;
            }
            Set<String> keptByReplacement = new LinkedHashSet<>(rewrite.replacementType().getExclusiveAndOwnHullModIds());
            for (String hullModId : new AllocatedNode(node, rewrite.originalType()).exclusiveHullModIds()) {
                if (!keptByReplacement.contains(hullModId) && removableInstalledHullMod.test(hullModId)) {
                    lostHullModIds.add(hullModId);
                }
            }
        }
        return lostHullModIds;
    }
}
