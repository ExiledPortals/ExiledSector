package exiledsector.ui.inspect;


import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;

import java.util.List;

public final class ShipSummaryParts {

    private static final DomainContributions<ShipSummaryPart> PARTS = new DomainContributions<>(ExiledSectorDomain::shipSummaryParts);

    private ShipSummaryParts() {
    }

    public static List<ShipSummaryPart> all() {
        return PARTS.all();
    }
}
