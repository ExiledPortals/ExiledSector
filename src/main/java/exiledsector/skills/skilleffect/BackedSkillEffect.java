package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

interface BackedSkillEffect extends SkillEffect {

    EffectBacking backing();

    @Override
    default void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        if (backing() != null) {
            backing().apply(stats, modId, magnitude);
        }
    }

    @Override
    default void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        if (backing() != null) {
            backing().applyAfterShipCreation(ship);
        }
    }

    @Override
    default void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
        if (backing() != null) {
            backing().advanceInCombat(ship, modId, magnitude);
        }
    }

    @Override
    default void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
        if (backing() != null) {
            backing().applyToFighterSpawnedByShip(fighter, parentShip, modId, magnitude);
        }
    }

    @Override
    default StatMode statMode() {
        return backing() == null ? null : backing().mode();
    }

    @Override
    default boolean advancesInCombat() {
        return backing() != null && backing().advancesInCombat();
    }

    @Override
    default boolean supportsTemporaryGating() {
        return backing() != null && backing().supportsTemporaryGating();
    }

    @Override
    default boolean lowerIsBetter() {
        return backing() != null && backing().lowerIsBetter();
    }

    @Override
    default StyledText description(float magnitude) {
        return backing() != null ? backing().description(this, magnitude) : EffectText.templated(this, magnitude);
    }
}
