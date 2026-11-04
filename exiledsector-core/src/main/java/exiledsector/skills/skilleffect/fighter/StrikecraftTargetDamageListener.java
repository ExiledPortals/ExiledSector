package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

abstract class StrikecraftTargetDamageListener implements DamageDealtModifier {

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private final String damagePercentKey;
    private final String damageModId;

    StrikecraftTargetDamageListener(ShipAPI fighter, ShipAPI carrier, String damagePercentKey, String damageModId) {
        this.fighter = fighter;
        this.carrier = carrier;
        this.damagePercentKey = damagePercentKey;
        this.damageModId = damageModId;
    }

    abstract boolean qualifies(ShipAPI target);

    @Override
    public final String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !qualifies(targetShip) || !CombatQueries.isHostile(fighter, targetShip)) {
            return null;
        }
        float percent = FighterListenerEffect.carrierValue(carrier, damagePercentKey);
        if (percent <= 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(damageModId, percent);
        return damageModId;
    }
}
