package exiledsector.ui.socket;

import exiledsector.items.CraftingInhibitor;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

final class ActiveInhibitors {

    enum Toggle {ON, OFF, NONE_OWNED, FULL}

    static final int MAX_ACTIVE = 4;

    private final Set<CraftingInhibitor> active = EnumSet.noneOf(CraftingInhibitor.class);
    private final Set<CraftingInhibitor> view = Collections.unmodifiableSet(active);
    private final int maxActive;

    ActiveInhibitors() {
        this(MAX_ACTIVE);
    }

    ActiveInhibitors(int maxActive) {
        this.maxActive = maxActive;
    }

    int maxActive() {
        return maxActive;
    }

    Toggle toggle(CraftingInhibitor inhibitor, int ownedCount) {
        if (active.remove(inhibitor)) {
            return Toggle.OFF;
        }
        if (ownedCount < 1) {
            return Toggle.NONE_OWNED;
        }
        if (active.size() >= maxActive) {
            return Toggle.FULL;
        }
        active.add(inhibitor);
        return Toggle.ON;
    }

    boolean contains(CraftingInhibitor inhibitor) {
        return active.contains(inhibitor);
    }

    boolean isEmpty() {
        return active.isEmpty();
    }

    Set<CraftingInhibitor> view() {
        return view;
    }

    List<CraftingInhibitor> dropMissing(ToIntFunction<String> owned) {
        List<CraftingInhibitor> missing = active.stream().filter(inhibitor -> owned.applyAsInt(inhibitor.itemId()) < 1).toList();
        missing.forEach(active::remove);
        return missing;
    }

    void spend(Collection<CraftingInhibitor> spent) {
        active.removeAll(spent);
    }
}
