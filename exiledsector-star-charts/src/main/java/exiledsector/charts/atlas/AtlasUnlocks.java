package exiledsector.charts.atlas;

import java.util.Set;
import java.util.function.Predicate;

public record AtlasUnlocks(Predicate<String> modEnabled, Set<String> unlockedFlags) {

    public static final AtlasUnlocks NONE = new AtlasUnlocks(modId -> false, Set.of());

    public AtlasUnlocks {
        unlockedFlags = Set.copyOf(unlockedFlags);
    }

    boolean allows(String requiredMod, String unlock) {
        return (requiredMod.isEmpty() || modEnabled.test(requiredMod)) && (unlock.isEmpty() || unlockedFlags.contains(unlock));
    }
}
