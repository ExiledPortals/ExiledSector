package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;

import java.util.ArrayList;
import java.util.Set;

public final class PhantomInstallSync {

    private PhantomInstallSync() {
    }

    public static void sync(FleetMemberAPI member, ShipVariantAPI variant) {
        ShipSkillData data = SkillDataResolver.resolve(member, variant);
        if (data == null) return;

        sync(ResolvedTree.phantomHullModIdsOf(AllocatedNode.of(data)), variant);
    }

    static void sync(Set<String> wanted, ShipVariantAPI variant) {
        if (variant == null) return;

        installWanted(wanted, variant);
        removeUnwanted(wanted, variant);
    }

    private static void installWanted(Set<String> wanted, ShipVariantAPI variant) {
        for (String hullModId : wanted) {
            if (!variant.hasHullMod(hullModId)) {
                variant.addPermaMod(hullModId);
                if (!InstalledHullMods.isInstalledBySkillTree(variant, hullModId)) {
                    variant.addTag(InstalledHullMods.tag(hullModId));
                }
            }
        }
    }

    private static void removeUnwanted(Set<String> wanted, ShipVariantAPI variant) {
        for (String tag : new ArrayList<>(variant.getTags())) {
            String hullModId = installedHullModId(tag);
            if (hullModId != null && !wanted.contains(hullModId)) {
                if (!variant.getSMods().contains(hullModId)) {
                    variant.removePermaMod(hullModId);
                }
                variant.removeTag(tag);
            }
        }
    }

    public static boolean restoreInstalledPermaMods(ShipVariantAPI variant) {
        boolean restored = false;
        for (String tag : variant.getTags()) {
            String hullModId = installedHullModId(tag);
            if (hullModId != null && variant.hasHullMod(hullModId) && !variant.getPermaMods().contains(hullModId)) {
                variant.addPermaMod(hullModId);
                restored = true;
            }
        }
        return restored;
    }

    private static String installedHullModId(String tag) {
        return tag.startsWith(InstalledHullMods.TAG_PREFIX) ? tag.substring(InstalledHullMods.TAG_PREFIX.length()) : null;
    }
}
