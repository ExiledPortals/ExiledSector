package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.InstalledHullMods;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record NpcHullMods(Set<String> removable, Set<String> permanent) {

    public static final NpcHullMods NONE = new NpcHullMods(Set.of(), Set.of());

    public NpcHullMods {
        removable = sortedCopy(removable);
        permanent = sortedCopy(permanent);
    }

    public static NpcHullMods of(ShipVariantAPI variant) {
        if (variant == null) {
            return NONE;
        }
        Set<String> permanent = new TreeSet<>();
        if (variant.getHullSpec() != null) {
            addAll(permanent, variant.getHullSpec().getBuiltInMods());
        }
        addAll(permanent, variant.getPermaMods());
        addAll(permanent, variant.getSMods());
        Set<String> removable = new TreeSet<>();
        addAll(removable, variant.getHullMods());
        removable.removeAll(permanent);
        permanent.removeIf(hullModId -> InstalledHullMods.isInstalledBySkillTree(variant, hullModId));
        removable.removeIf(hullModId -> InstalledHullMods.isInstalledBySkillTree(variant, hullModId));
        return new NpcHullMods(removable, permanent);
    }

    public Set<String> installed() {
        Set<String> installed = new TreeSet<>(removable);
        installed.addAll(permanent);
        return installed;
    }

    private static void addAll(Set<String> target, Collection<String> source) {
        if (source != null) {
            target.addAll(source);
        }
    }

    private static Set<String> sortedCopy(Set<String> source) {
        return source == null ? Set.of() : Collections.unmodifiableSet(new TreeSet<>(source));
    }
}
