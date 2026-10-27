package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.i18n.StyledText;
import exiledlib.modifiers.ModifierEffect;
import exiledlib.skills.AffinityScaling;
import exiledlib.skills.CombatGate;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.SocketedItemScaling;
import exiledlib.skills.skilleffect.hullmodule.ModuleShare;
import exiledlib.skills.skilleffect.hullmodule.ModuleShares;

public interface SkillEffect extends ModifierEffect {

    void apply(MutableShipStatsAPI stats, String modId, float magnitude);

    @Override
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

    @Override
    default boolean invertsDescriptionColours() {
        return lowerIsBetter();
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

    default AffinityScaling affinityScaling() {
        return null;
    }

    default CombatGate radiusGate() {
        return null;
    }

    default SocketedItemScaling socketedItemScaling() {
        return null;
    }

    default boolean targetsSocketedItem() {
        SocketedItemScaling scaling = socketedItemScaling();
        return scaling != null && scaling.scope() == SocketedItemScaling.Scope.TARGETED;
    }

    default boolean appliesToNpcShips() {
        return true;
    }

    default ModuleShare moduleShare() {
        return ModuleShares.of(this);
    }

    default StyledText deallocationWarning(float magnitude) {
        return null;
    }

    @Override
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
