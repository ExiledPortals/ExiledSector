package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.List;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.scaled;
import static exiledsector.skills.skilleffect.StatTarget.stat;

final class HoldFireShieldListener extends ShipCombatListener implements AdvanceableListener {

    static final String REDUCTION_PERCENT_KEY = "exiledSector_holdFireShieldDamageReductionPercent";
    static final float TAPER_SECONDS = 3f;
    static final float CHECK_INTERVAL_SECONDS = 0.1f;
    private static final String MOD_ID_PREFIX = "exiledSector_holdFireShield_";
    private static final ScaledBonus SHIELD_DAMAGE_REDUCTION = new ScaledBonus(
            percent(scaled(stat(MutableShipStatsAPI::getShieldDamageTakenMult), -1f), REDUCTION_PERCENT_KEY));

    private final ScaledBonus.Tracker shieldDamageReduction = SHIELD_DAMAGE_REDUCTION.tracker();
    private float secondsSinceCheck;
    private float holdFireLevel;

    HoldFireShieldListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    float holdFireLevel() {
        return holdFireLevel;
    }

    static float nextHoldFireLevel(float level, boolean holdingFire, float seconds) {
        float step = seconds / TAPER_SECONDS;
        return holdingFire ? Math.min(1f, level + step) : Math.max(0f, level - step);
    }

    @Override
    public void advance(float amount) {
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_INTERVAL_SECONDS) {
            return;
        }
        holdFireLevel = nextHoldFireLevel(holdFireLevel, ownerIsAliveNotHulk() && !anyWeaponFiring(), secondsSinceCheck);
        secondsSinceCheck = 0f;
        shieldDamageReduction.apply(ownerShip.getMutableStats(), modId, holdFireLevel);
    }

    private boolean anyWeaponFiring() {
        List<WeaponAPI> weapons = ownerShip.getAllWeapons();
        for (int i = 0; i < weapons.size(); i++) {
            WeaponAPI weapon = weapons.get(i);
            if (!weapon.isDecorative() && weapon.isFiring()) {
                return true;
            }
        }
        return false;
    }
}
