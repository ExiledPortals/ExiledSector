package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.defense.CollisionDamageListener;
import org.lwjgl.util.vector.Vector2f;

final class CollisionDamageTakenListener extends ShipCombatListener implements DamageTakenModifier {

    static final String REDUCTION_PERCENT_KEY = "exiledSector_scarCollisionDamageTakenPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_scarCollisionDamageTaken_";

    CollisionDamageTakenListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target != ownerShip || !CollisionDamageListener.isCollisionDamage(param, damage)) {
            return null;
        }
        float percent = magnitude(REDUCTION_PERCENT_KEY);
        if (percent <= 0f || !CollisionDamageListener.touchesAnything(ownerShip)) {
            return null;
        }
        damage.getModifier().modifyMult(modId, CollisionDamageListener.takenMult(percent));
        return modId;
    }
}
