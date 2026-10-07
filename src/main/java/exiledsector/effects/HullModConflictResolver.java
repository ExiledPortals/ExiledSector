package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.compat.MagicLibCompat;
import exiledsector.i18n.I18n;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.PhantomHullModStatus;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillType;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.List;

public final class HullModConflictResolver {

    private HullModConflictResolver() {
    }

    public static void removeConflicts(FleetMemberAPI member, ShipVariantAPI variant) {
        if (member == null || SkillDataResolver.isNpcTree(variant)) return;

        removeConflicts(AllocatedNode.of(ShipSkillDataManager.get(member.getId())), variant);
    }

    static void removeConflicts(List<AllocatedNode> allocatedNodes, ShipVariantAPI variant) {
        if (variant == null) return;

        boolean conflictFound = removeHullModsThatTriedToStripAPhantom(allocatedNodes, variant, true);
        for (AllocatedNode allocated : allocatedNodes) {
            SkillType allocatedType = allocated.effectiveType();
            for (String hullModId : allocated.exclusiveHullModIds()) {
                if (isRemovableConflict(variant, hullModId)) {
                    MagicIncompatibleHullmods.removeHullmodWithWarning(variant, hullModId, SkillConflictWarningHullMod.ID);
                    variant.removeMod(MagicLibCompat.WARNING_HULLMOD_ID);
                    variant.addMod(SkillConflictWarningHullMod.ID);
                    SkillConflictWarnings.recordRemoval(variant, hullModId, I18n.forGameText(allocatedType::getDisplayName));
                    conflictFound = true;
                }
            }
        }

        if (!conflictFound) {
            if (variant.hasHullMod(SkillConflictWarningHullMod.ID)) {
                variant.removeMod(SkillConflictWarningHullMod.ID);
            }
            SkillConflictWarnings.clear(variant);
        }
    }

    static boolean removeHullModsThatTriedToStripAPhantom(List<AllocatedNode> allocatedNodes, ShipVariantAPI variant, boolean warn) {
        if (variant == null || !variant.hasHullMod(MagicLibCompat.WARNING_HULLMOD_ID)) return false;

        List<String> stripAttempt = MagicIncompatibleHullmods.getReason(variant);
        if (stripAttempt == null || stripAttempt.size() < 2) return false;
        String phantomId = stripAttempt.get(0);
        SkillType phantomProviderType = phantomProvider(allocatedNodes, phantomId);
        if (phantomProviderType == null || !InstalledHullMods.isInstalledBySkillTree(variant, phantomId)) return false;

        variant.removeMod(MagicLibCompat.WARNING_HULLMOD_ID);
        String causeId = stripAttempt.get(1);
        if (causeId == null || Global.getSettings().getHullModSpec(causeId) == null || !isRemovableConflict(variant, causeId)
                || variant.getPermaMods().contains(causeId)) {
            return false;
        }
        variant.removeMod(causeId);
        if (warn) {
            variant.addMod(SkillConflictWarningHullMod.ID);
            SkillConflictWarnings.recordRemoval(variant, causeId, I18n.forGameText(phantomProviderType::getDisplayName));
        }
        return true;
    }

    private static boolean isRemovableConflict(ShipVariantAPI variant, String hullModId) {
        boolean builtIn = variant.getHullSpec() != null && variant.getHullSpec().isBuiltInMod(hullModId);
        return variant.hasHullMod(hullModId) && !builtIn && !InstalledHullMods.isInstalledBySkillTree(variant, hullModId);
    }

    private static SkillType phantomProvider(List<AllocatedNode> allocatedNodes, String hullModId) {
        if (!PhantomHullModStatus.isActive(hullModId)) return null;
        for (AllocatedNode allocated : allocatedNodes) {
            if (allocated.effectiveType().getPhantomHullModIds().contains(hullModId)) {
                return allocated.effectiveType();
            }
        }
        return null;
    }
}
