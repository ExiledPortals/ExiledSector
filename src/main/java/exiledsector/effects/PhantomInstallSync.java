package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.InstalledHullMods;

import java.util.ArrayList;
import java.util.Set;

final class PhantomInstallSync {

    private PhantomInstallSync() {
    }

    static boolean sync(Set<String> wantedHullModIds, ShipVariantAPI variant) {
        if (variant == null) return false;

        boolean changed = installWanted(wantedHullModIds, variant);
        changed |= removeUnwanted(wantedHullModIds, variant);
        return changed;
    }

    private static boolean installWanted(Set<String> wantedHullModIds, ShipVariantAPI variant) {
        boolean changed = false;
        for (String hullModId : wantedHullModIds) {
            if (!variant.hasHullMod(hullModId)) {
                variant.addPermaMod(hullModId);
                if (!InstalledHullMods.isInstalledBySkillTree(variant, hullModId)) {
                    variant.addTag(InstalledHullMods.tag(hullModId));
                }
                changed = true;
            } else if (InstalledHullMods.isInstalledBySkillTree(variant, hullModId) && !variant.getPermaMods().contains(hullModId)) {
                variant.addPermaMod(hullModId);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean removeUnwanted(Set<String> wantedHullModIds, ShipVariantAPI variant) {
        boolean changed = false;
        for (String tag : new ArrayList<>(variant.getTags())) {
            String hullModId = installedHullModId(tag);
            if (hullModId != null && !wantedHullModIds.contains(hullModId)) {
                if (!variant.getSMods().contains(hullModId)) {
                    variant.removePermaMod(hullModId);
                }
                variant.removeTag(tag);
                changed = true;
            }
        }
        return changed;
    }

    private static String installedHullModId(String tag) {
        return tag.startsWith(InstalledHullMods.TAG_PREFIX) ? tag.substring(InstalledHullMods.TAG_PREFIX.length()) : null;
    }
}
