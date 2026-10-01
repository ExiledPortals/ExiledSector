package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.ShipFacts;

public interface SkillEffect {

    void apply(MutableShipStatsAPI stats, String modId, float magnitude);

    StyledText description(float magnitude);

    default boolean isConditional() {
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

    default void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
    }

    default void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
    }

    default void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
    }

    default String blockDeallocationReason(FleetMemberAPI member, float magnitude) {
        return null;
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

    static SkillEffect byName(String name) {
        return SkillEffectRegistry.byName(name);
    }
}
