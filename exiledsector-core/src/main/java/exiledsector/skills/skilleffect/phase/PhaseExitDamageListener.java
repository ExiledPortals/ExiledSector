package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

final class PhaseExitDamageListener extends PhaseTransitionListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_phaseExitWeaponDamagePercent";
    static final float TAPER_SECONDS = 3f;
    private static final String MOD_ID_PREFIX = "exiledSector_phaseExitWeaponDamage_";
    private static final ScaledBonus AMBUSH_BONUS = new ScaledBonus(
            ScaledBonus.percent(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL), DAMAGE_PERCENT_KEY));

    private final ScaledBonus.Tracker ambushBonus = AMBUSH_BONUS.tracker();
    private float taperSecondsLeft;

    PhaseExitDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float taperShare(float secondsLeft) {
        return Math.max(0f, Math.min(1f, secondsLeft / TAPER_SECONDS));
    }

    @Override
    void onPhaseExit() {
        if (ownerIsAliveNotHulk()) {
            taperSecondsLeft = TAPER_SECONDS;
        }
    }

    @Override
    void afterTransitionCheck(float amount) {
        if (!ownerIsAliveNotHulk()) {
            taperSecondsLeft = 0f;
        } else if (taperSecondsLeft > 0f) {
            taperSecondsLeft = Math.max(0f, taperSecondsLeft - amount);
        }
        ambushBonus.apply(ownerShip.getMutableStats(), modId, taperShare(taperSecondsLeft));
    }
}
