package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.DamagePercentListener;

public abstract class TargetDamageBonusListener extends DamagePercentListener {

    private final String damagePercentKey;

    protected TargetDamageBonusListener(ShipAPI ownerShip, String modIdPrefix, String damagePercentKey) {
        this(ownerShip, ownerShip, modIdPrefix, damagePercentKey);
    }

    protected TargetDamageBonusListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String modIdPrefix, String damagePercentKey) {
        super(ownerShip, magnitudeHolder, modIdPrefix);
        this.damagePercentKey = damagePercentKey;
    }

    protected abstract boolean qualifies(ShipAPI target);

    @Override
    protected final float damagePercent(Object param, CombatEntityAPI target) {
        if (!(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return 0f;
        }
        float percent = magnitude(damagePercentKey);
        return percent != 0f && qualifies(targetShip) ? percent : 0f;
    }
}
