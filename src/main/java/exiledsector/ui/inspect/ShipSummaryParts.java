package exiledsector.ui.inspect;

import exiledsector.ExiledSectorModPlugin;

import java.util.List;

public final class ShipSummaryParts {

    private static final List<ShipSummaryPart> ALL = ExiledSectorModPlugin.DOMAINS.stream()
            .flatMap(domain -> domain.shipSummaryParts().stream())
            .toList();

    private ShipSummaryParts() {
    }

    public static List<ShipSummaryPart> all() {
        return ALL;
    }
}
