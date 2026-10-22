package exiledsector.domain;

import exiledsector.effects.ShipEffectSource;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;

import java.util.List;

public interface ExiledSectorDomain extends Domain {

    default List<ShipEffectSource> shipEffectSources() {
        return List.of();
    }

    default List<LootSource> lootSources() {
        return List.of();
    }

    default List<FleetDifficulty> fleetDifficulties() {
        return List.of();
    }
}
