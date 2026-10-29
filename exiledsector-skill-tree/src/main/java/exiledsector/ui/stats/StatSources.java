package exiledsector.ui.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.BallisticEnergyMirror;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SocketedItem;
import exiledsector.effects.ResolvedTree;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.RadiusLineOrigin;
import exiledsector.items.SubroutineOrigin;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.HullModNames;
import exiledsector.skills.ShipSkillData;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StatSources {

    private static final List<String> OWN_MOD_ID_PREFIXES = List.of("exiledSector_", "exiledLib_");

    private final Map<String, ResolvedTree.AppliedModifier> appliedModifiers;
    private final ShipSkillData skillData;

    StatSources(ResolvedTree resolvedTree, ShipSkillData skillData) {
        this.appliedModifiers = resolvedTree == null ? Map.of() : resolvedTree.appliedModifiers();
        this.skillData = skillData;
    }

    ResolvedTree.AppliedModifier applied(String modId) {
        ResolvedTree.AppliedModifier exact = appliedModifiers.get(modId);
        if (exact != null) {
            return exact;
        }
        String longestPrefix = null;
        for (String appliedModId : appliedModifiers.keySet()) {
            boolean prefixes = modId.startsWith(appliedModId + "_");
            if (prefixes && (longestPrefix == null || appliedModId.length() > longestPrefix.length())) {
                longestPrefix = appliedModId;
            }
        }
        return longestPrefix == null ? null : appliedModifiers.get(longestPrefix);
    }

    String originName(Object origin) {
        if (origin instanceof AllocatedNode node) {
            return node.effectiveType().getDisplayName();
        }
        if (origin instanceof SubroutineOrigin subroutine) {
            return itemName(socketedItemId(subroutine.socket().node().getId()), subroutine.socket().effectiveType().getDisplayName());
        }
        if (origin instanceof RadiusLineOrigin radiusLine) {
            return radiusLine.node().effectiveType().getDisplayName();
        }
        if (origin instanceof BallisticEnergyMirror.Origin mirror) {
            return originName(mirror.mirroredOrigin());
        }
        if (origin instanceof AffinitySteps.Origin || origin instanceof AffinitySteps.ScaledOrigin) {
            return Translation.text("ui.stats.source.affinity");
        }
        if (origin instanceof SocketedItem item && SocketedItem.HULL_ITEM.equals(item.category())) {
            String itemId = skillData == null ? null : skillData.getSectionItems().get(item.slotId());
            return itemName(itemId, Translation.text("ui.stats.source.hullItem"));
        }
        return Translation.text("ui.stats.source.exiledSector");
    }

    static Set<String> nodeIds(Object origin) {
        Set<String> nodeIds = new LinkedHashSet<>();
        addNodeIds(origin, nodeIds);
        return nodeIds;
    }

    static Set<String> nodeIds(List<EffectContribution> contributions) {
        Set<String> nodeIds = new LinkedHashSet<>();
        contributions.forEach(contribution -> addNodeIds(contribution.origin(), nodeIds));
        return nodeIds;
    }

    private static void addNodeIds(Object origin, Set<String> nodeIds) {
        if (origin instanceof AllocatedNode node) {
            nodeIds.add(node.node().getId());
        } else if (origin instanceof SubroutineOrigin subroutine) {
            nodeIds.add(subroutine.socket().node().getId());
        } else if (origin instanceof RadiusLineOrigin radiusLine) {
            nodeIds.add(radiusLine.node().node().getId());
            if (radiusLine.sourceSocketNodeId() != null) {
                nodeIds.add(radiusLine.sourceSocketNodeId());
            }
        } else if (origin instanceof BallisticEnergyMirror.Origin mirror) {
            addNodeIds(mirror.mirroredOrigin(), nodeIds);
        }
    }

    static String outsideSourceName(String modId, String desc) {
        if (desc != null && !desc.isBlank()) {
            return desc;
        }
        for (String ownPrefix : OWN_MOD_ID_PREFIXES) {
            if (modId.startsWith(ownPrefix)) {
                return Translation.text("ui.stats.source.exiledSector");
            }
        }
        String hullModName = loadedHullModName(modId);
        if (hullModName != null) {
            return hullModName;
        }
        String skillName = loadedSkillName(modId);
        return skillName != null ? skillName : modId;
    }

    private String socketedItemId(String socketNodeId) {
        return skillData == null ? null : skillData.getSocketedItems().get(socketNodeId);
    }

    private static String itemName(String itemId, String fallback) {
        Item item = itemId == null ? null : ItemStore.lookup(itemId);
        return item != null ? item.name() : fallback;
    }

    private static String loadedHullModName(String modId) {
        try {
            return HullModNames.loadedDisplayName(modId);
        } catch (RuntimeException unknownHullMod) {
            return null;
        }
    }

    private static String loadedSkillName(String modId) {
        try {
            SkillSpecAPI skillSpec = Global.getSettings().getSkillSpec(modId);
            return skillSpec == null ? null : skillSpec.getName();
        } catch (RuntimeException unknownSkill) {
            return null;
        }
    }
}
