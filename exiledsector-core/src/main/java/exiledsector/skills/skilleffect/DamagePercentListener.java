package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public abstract class DamagePercentListener extends ShipCombatListener implements DamageDealtModifier {

    protected DamagePercentListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String modIdPrefix) {
        super(ownerShip, magnitudeHolder, modIdPrefix);
    }

    protected abstract float damagePercent(Object param, CombatEntityAPI target);

    @Override
    public final String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        float percent = damagePercent(param, target);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
