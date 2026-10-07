package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.ShipFacts;

public interface SkillEffect {

    void apply(MutableShipStatsAPI stats, String modId, float magnitude);

    StyledText description(float magnitude);

    default StyledText description(float magnitude, ShipAPI.HullSize hullSize) {
        return description(magnitude);
    }

    default boolean advancesInCombat() {
        return false;
    }

    default boolean supportsTemporaryGating() {
        return false;
    }

    default boolean lowerIsBetter() {
        return false;
    }

    default boolean appliesAfterOtherEffects() {
        return false;
    }

    default boolean reshapesDealtDamage() {
        return false;
    }

    default void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
    }

    default void advanceInCombat(ShipAPI ship, String modId, float magnitude, float amount) {
        advanceInCombat(ship, modId, magnitude);
    }

    default void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
    }

    default void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
    }

    default String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
        return null;
    }

    default boolean appliesToNpcShips() {
        return true;
    }

    default StyledText deallocationWarning(float magnitude) {
        return null;
    }

    String name();

    default StatMode statMode() {
        return null;
    }

    default boolean isMultiplicative() {
        return statMode() == StatMode.MULT;
    }

    static float addedMultiplier(float totalMagnitude) {
        return Math.max(totalMagnitude, -100f);
    }

    static SkillEffect byName(String name) {
        return SkillEffectRegistry.byName(name);
    }
}
