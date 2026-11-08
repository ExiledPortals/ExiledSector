package exiledsector.effects;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.List;
import java.util.Map;

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

    default Map<SkillEffect, Float> moduleContributions(ShipEffectContext parentContext, ResolvedTree parentTree, ShipVariantAPI moduleVariant) {
        return Map.of();
    }

    default void applyToModule(ShipEffectContext parentContext, MutableShipStatsAPI moduleStats) {
    }

    default void afterModuleCreation(ShipEffectContext parentContext, ShipAPI module) {
    }
}
