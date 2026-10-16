package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.compat.MagicLibCompat;
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
                    SkillConflictWarnings.recordRemoval(variant, hullModId, allocatedType.getId());
                    conflictFound = true;
                }
            }
        }

        if (!conflictFound) {
            keepWarningOnlyWhileRecorded(variant);
        }
    }

    private static void keepWarningOnlyWhileRecorded(ShipVariantAPI variant) {
        boolean warningInstalled = variant.hasHullMod(SkillConflictWarningHullMod.ID);
        if (!warningInstalled) {
            SkillConflictWarnings.clear(variant);
        } else if (SkillConflictWarnings.get(variant) == null) {
            variant.removeMod(SkillConflictWarningHullMod.ID);
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
        if (causeId == null || Global.getSettings().getHullModSpec(causeId) == null || !isRemovableConflict(variant, causeId)) {
            return false;
        }
        variant.removeMod(causeId);
        if (warn) {
            variant.addMod(SkillConflictWarningHullMod.ID);
            SkillConflictWarnings.recordRemoval(variant, causeId, phantomProviderType.getId());
        }
        return true;
    }

    private static boolean isRemovableConflict(ShipVariantAPI variant, String hullModId) {
        return variant.hasHullMod(hullModId) && InstalledHullMods.isRemovable(variant, hullModId)
                && !InstalledHullMods.isInstalledBySkillTree(variant, hullModId);
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
