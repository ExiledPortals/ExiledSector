package exiledsector.effects;


import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;

import java.util.List;

public final class ShipEffectSources {

    private static final DomainContributions<ShipEffectSource> SOURCES = new DomainContributions<>(ExiledSectorDomain::shipEffectSources);

    private ShipEffectSources() {
    }

    public static List<ShipEffectSource> all() {
        return SOURCES.all();
    }
}
