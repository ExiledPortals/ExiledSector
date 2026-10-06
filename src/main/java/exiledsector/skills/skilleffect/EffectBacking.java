package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

sealed interface EffectBacking permits SimpleStatEffect, ConditionalStatEffect, ListenerEffect, FighterSkillEffect.RoleBacking,
        CompatSkillEffect.WaivedPenalty {

    default void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
    }

    default void applyAfterShipCreation(ShipAPI ship) {
    }

    default void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
    }

    default void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
    }

    default StatMode mode() {
        return null;
    }

    default boolean advancesInCombat() {
        return false;
    }

    boolean supportsTemporaryGating();

    default boolean lowerIsBetter() {
        return false;
    }

    StyledText description(SkillEffect effect, float magnitude);
}
