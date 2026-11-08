package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ParameterBonus;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.TaperedBonus;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

final class PhaseExitDamageListener extends PhaseTransitionListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_phaseExitWeaponDamagePercent";
    static final String DURATION_PERCENT_KEY = "exiledSector_phaseExitWeaponDamageDurationPercent";
    static final float TAPER_SECONDS = 3f;
    private static final String MOD_ID_PREFIX = "exiledSector_phaseExitWeaponDamage_";
    private static final ScaledBonus AMBUSH_BONUS = new ScaledBonus(
            ScaledBonus.percent(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL), DAMAGE_PERCENT_KEY));

    private final TaperedBonus ambushBonus;

    PhaseExitDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        ambushBonus = new TaperedBonus(AMBUSH_BONUS, ownerShip.getMutableStats(), modId);
    }

    static float taperSeconds(float durationIncreasePercent) {
        return ParameterBonus.increasedBy(TAPER_SECONDS, durationIncreasePercent);
    }

    @Override
    void onPhaseExit() {
        if (ownerIsAliveNotHulk()) {
            ambushBonus.start(taperSeconds(magnitude(DURATION_PERCENT_KEY)));
        }
    }

    @Override
    void afterTransitionCheck(float amount) {
        if (ownerIsAliveNotHulk()) {
            ambushBonus.advance(amount);
        } else {
            ambushBonus.stop();
        }
    }
}
