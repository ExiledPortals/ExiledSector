package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public abstract class HitSideEffectListener extends ShipCombatListener implements DamageDealtModifier {

    protected HitSideEffectListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    protected HitSideEffectListener(ShipAPI ownerShip, String modIdPrefix) {
        super(ownerShip, modIdPrefix);
    }

    protected HitSideEffectListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String modIdPrefix) {
        super(ownerShip, magnitudeHolder, modIdPrefix);
    }

    // java:S3516: the engine reads a null return as "leave the damage modifier unchanged"; hits are changed in place or side effects queued instead
    @SuppressWarnings("java:S3516")
    @Override
    public final String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        onHit(param, target, damage, point, shieldHit);
        return null;
    }

    protected abstract void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit);
}
