package exiledsector.effects;

import exiledsector.ExiledSectorModPlugin;

import java.util.List;

public final class ShipEffectSources {

    private static final List<ShipEffectSource> ALL = ExiledSectorModPlugin.DOMAINS.stream()
            .flatMap(domain -> domain.shipEffectSources().stream())
            .toList();

    private ShipEffectSources() {
    }

    public static List<ShipEffectSource> all() {
        return ALL;
    }
}
