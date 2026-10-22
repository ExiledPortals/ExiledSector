package exiledsector.domain;

import exiledsector.effects.ShipEffectSource;

import java.util.List;

public interface ExiledSectorDomain extends Domain {

    default List<ShipEffectSource> shipEffectSources() {
        return List.of();
    }
}
