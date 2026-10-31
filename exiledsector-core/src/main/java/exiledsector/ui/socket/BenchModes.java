package exiledsector.ui.socket;

import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.ItemType;

public final class BenchModes {

    private static final DomainContributions<BenchModeSupport> SUPPORTS = new DomainContributions<>(ExiledSectorDomain::benchModes);

    private BenchModes() {
    }

    public static BenchModeSupport of(BenchMode mode) {
        return SUPPORTS.firstOr(support -> support.mode() == mode ? support : null, HullBench.INSTANCE);
    }

    static BenchMode modeFor(ItemType type) {
        return SUPPORTS.firstOr(support -> support.itemType() == type ? support.mode() : null, BenchMode.HULL_ITEMS);
    }
}
