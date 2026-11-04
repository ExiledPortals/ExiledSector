package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

abstract class TargetDamageBonusListener extends ShipCombatListener implements DamageDealtModifier {

    private final String damagePercentKey;

    TargetDamageBonusListener(ShipAPI ownerShip, String modIdPrefix, String damagePercentKey) {
        super(ownerShip, modIdPrefix);
        this.damagePercentKey = damagePercentKey;
    }

    abstract boolean qualifies(ShipAPI target);

    @Override
    public final String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float percent = magnitude(damagePercentKey);
        if (percent == 0f || !qualifies(targetShip)) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
