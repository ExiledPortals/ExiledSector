package exiledsector.ui.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.BallisticEnergyMirror;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SocketedItem;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.effects.ResolvedTree;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.RadiusLineOrigin;
import exiledsector.items.SubroutineOrigin;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.HullModNames;
import exiledsector.skills.ShipSkillData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StatSources {

    private static final List<String> OWN_MOD_ID_PREFIXES = List.of("exiledSector_");
    private static final Map<String, String> VANILLA_SOURCE_KEYS = Map.of(
            "flux_capacitors", "ui.stats.source.fluxCapacitors",
            "flux_vents", "ui.stats.source.fluxVents",
            "cr_effect", "ui.stats.source.combatReadiness");
    private static final Map<String, String> VANILLA_SKILL_MODIFIER_IDS = Map.of(
            "bt_cargo_cap_mod", "bulk_transport",
            "bt_fuel_cap_mod", "bulk_transport",
            "bt_crew_cap_mod", "bulk_transport",
            "sal_cargo_cap_mod", "salvaging",
            "nav_fuel_use_mod", "navigation",
            "sp_fuel_use_mod", "containment_procedures",
            "surveying_supply_use_mod", "makeshift_equipment",
            "coord_maneuvers_bonus", "coordinated_maneuvers");
    private static final Map<String, String> LOADED_SOURCE_NAMES = new HashMap<>();

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
        String vanillaKey = VANILLA_SOURCE_KEYS.get(modId);
        if (vanillaKey != null) {
            return Translation.text(vanillaKey);
        }
        String secondInCommandSkill = SecondInCommandCompat.skillModifierName(modId);
        if (secondInCommandSkill != null) {
            return secondInCommandSkill;
        }
        return LOADED_SOURCE_NAMES.computeIfAbsent(modId, StatSources::loadedSourceName);
    }

    private static String loadedSourceName(String modId) {
        String vanillaSkillId = VANILLA_SKILL_MODIFIER_IDS.get(modId);
        if (vanillaSkillId != null) {
            String skillName = loadedSkillName(vanillaSkillId);
            if (skillName != null) {
                return skillName;
            }
        }
        for (String candidate = modId; candidate != null; candidate = shorterPrefix(candidate)) {
            String hullModName = loadedHullModName(candidate);
            if (hullModName != null) {
                return hullModName;
            }
            String skillName = loadedSkillName(candidate);
            if (skillName != null) {
                return skillName;
            }
        }
        return modId;
    }

    private static String shorterPrefix(String id) {
        int lastSeparator = id.lastIndexOf('_');
        return lastSeparator > 0 ? id.substring(0, lastSeparator) : null;
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
