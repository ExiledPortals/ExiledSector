package exiledsector.ui.socket;

import exiledlib.i18n.StyledText;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.Item;
import exiledsector.items.ItemType;

import java.util.ArrayList;
import java.util.List;

public final class BenchModes {

    private static final DomainContributions<BenchModeSupport> EVERY_SUPPORT = DomainContributions.fromEveryDomain(ExiledSectorDomain::benchModes);
    private static final DomainContributions<BenchModeSupport> ENABLED_SUPPORTS = new DomainContributions<>(ExiledSectorDomain::benchModes);

    private BenchModes() {
    }

    public static BenchModeSupport of(BenchMode mode) {
        return mode == null ? null : EVERY_SUPPORT.all().stream().filter(support -> support.mode() == mode).findFirst().orElse(null);
    }

    public static BenchMode modeFor(ItemType type) {
        return EVERY_SUPPORT.all().stream().filter(support -> support.itemType() == type).map(BenchModeSupport::mode).findFirst()
                .orElse(BenchMode.HULL_ITEMS);
    }

    public static boolean enabled(BenchMode mode) {
        return ENABLED_SUPPORTS.all().stream().anyMatch(support -> support.mode() == mode && support.enabled());
    }

    static List<StyledText> detailLines(Item item) {
        List<StyledText> lines = new ArrayList<>();
        EVERY_SUPPORT.all().forEach(support -> lines.addAll(support.detailLines(item)));
        return lines;
    }
}
