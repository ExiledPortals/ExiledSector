package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipRecords;

import java.util.ArrayList;
import java.util.List;

public final class ModuleLinks {

    public static final String PARENT_TAG_PREFIX = "exiledSector_moduleOf_";

    private ModuleLinks() {
    }

    public static void sync(FleetMemberAPI parentMember, ShipVariantAPI editedVariant) {
        ShipVariantAPI ownVariant = parentMember.getVariant();
        if (ownVariant == null) {
            return;
        }
        if (linkTag(ownVariant) != null && !ShipRecords.isOutsideFleet(parentMember)) {
            unlink(ownVariant);
        }
        if (parentMember.getFleetData() == null) {
            return;
        }
        linkModules(parentMember.getId(), ownVariant);
        if (editedVariant != null && editedVariant != ownVariant) {
            linkModules(parentMember.getId(), editedVariant);
        }
    }

    public static String parentId(ShipVariantAPI moduleVariant) {
        String linkTag = moduleVariant == null ? null : linkTag(moduleVariant);
        return linkTag == null ? null : linkTag.substring(PARENT_TAG_PREFIX.length());
    }

    static boolean hasModules(ShipVariantAPI variant) {
        return variant != null && variant.getStationModules() != null && !variant.getStationModules().isEmpty();
    }

    private static void linkModules(String parentMemberId, ShipVariantAPI parentVariant) {
        if (parentVariant.getSource() == VariantSource.STOCK || !hasModules(parentVariant)) {
            return;
        }
        for (String slotId : parentVariant.getModuleSlots()) {
            ShipVariantAPI moduleVariant = parentVariant.getModuleVariant(slotId);
            if (moduleVariant == null) {
                continue;
            }
            if (ShipRecords.taggedTree(moduleVariant) != null) {
                unlink(moduleVariant);
            } else if (!isLinked(moduleVariant, parentMemberId)) {
                link(ownedModuleVariant(parentVariant, slotId, moduleVariant), parentMemberId);
            }
        }
    }

    private static ShipVariantAPI ownedModuleVariant(ShipVariantAPI parentVariant, String slotId, ShipVariantAPI moduleVariant) {
        if (moduleVariant.getSource() == VariantSource.REFIT) {
            return moduleVariant;
        }
        ShipVariantAPI owned = moduleVariant.clone();
        owned.setSource(VariantSource.REFIT);
        parentVariant.setModuleVariant(slotId, owned);
        return owned;
    }

    private static boolean isLinked(ShipVariantAPI moduleVariant, String parentMemberId) {
        return moduleVariant.getPermaMods().contains(ModuleHullMod.ID) && (PARENT_TAG_PREFIX + parentMemberId).equals(linkTag(moduleVariant));
    }

    private static void link(ShipVariantAPI moduleVariant, String parentMemberId) {
        removeLinkTags(moduleVariant);
        moduleVariant.addTag(PARENT_TAG_PREFIX + parentMemberId);
        if (!moduleVariant.getPermaMods().contains(ModuleHullMod.ID)) {
            moduleVariant.addPermaMod(ModuleHullMod.ID);
        }
    }

    private static void unlink(ShipVariantAPI variant) {
        removeLinkTags(variant);
        if (variant.getPermaMods().contains(ModuleHullMod.ID)) {
            variant.removePermaMod(ModuleHullMod.ID);
        }
    }

    private static void removeLinkTags(ShipVariantAPI variant) {
        List<String> linkTags = new ArrayList<>();
        for (String tag : variant.getTags()) {
            if (tag.startsWith(PARENT_TAG_PREFIX)) {
                linkTags.add(tag);
            }
        }
        linkTags.forEach(variant::removeTag);
    }

    private static String linkTag(ShipVariantAPI variant) {
        for (String tag : variant.getTags()) {
            if (tag.startsWith(PARENT_TAG_PREFIX)) {
                return tag;
            }
        }
        return null;
    }
}
