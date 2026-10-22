package exiledsector.effects;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.skills.EffectContribution;

import java.util.List;

public interface ShipEffectSource {

    default float bonusScale(ShipEffectContext context) {
        return 1f;
    }

    default Object cacheKey(ShipEffectContext context) {
        return null;
    }

    default List<EffectContribution> contributions(ShipEffectContext context) {
        return List.of();
    }

    default void beforeStatsApplied(ShipEffectContext context) {
    }

    default void afterStatsApplied(ShipEffectContext context, ResolvedTree resolvedTree) {
    }

    default void afterShipCreation(ShipAPI ship, ResolvedTree resolvedTree) {
    }

    default void applyToShip(MutableShipStatsAPI stats) {
    }

    default void applyToFighter(ShipAPI fighter, ShipAPI ship) {
    }
}
