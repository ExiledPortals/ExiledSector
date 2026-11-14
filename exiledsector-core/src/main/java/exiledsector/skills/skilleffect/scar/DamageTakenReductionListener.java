package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.defense.CollisionDamageListener;
import org.lwjgl.util.vector.Vector2f;

abstract class DamageTakenReductionListener extends ShipCombatListener implements DamageTakenModifier {

    private final String reductionPercentKey;

    protected DamageTakenReductionListener(ShipAPI ownerShip, String modIdPrefix, String reductionPercentKey) {
        super(ownerShip, modIdPrefix);
        this.reductionPercentKey = reductionPercentKey;
    }

    protected abstract boolean isReducedHit(Object param, DamageAPI damage);

    @Override
    public final String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target != ownerShip) {
            return null;
        }
        float percent = magnitude(reductionPercentKey);
        if (percent <= 0f || !isReducedHit(param, damage)) {
            return null;
        }
        damage.getModifier().modifyMult(modId, CollisionDamageListener.takenMult(percent));
        return modId;
    }
}
