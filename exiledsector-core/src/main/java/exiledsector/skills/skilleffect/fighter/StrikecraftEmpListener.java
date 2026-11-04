package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import org.lwjgl.util.vector.Vector2f;

final class StrikecraftEmpListener implements DamageDealtModifier {

    static final String EMP_PERCENT_KEY = "exiledSector_strikecraftEmpDamagePercent";

    private final ShipAPI fighter;
    private final ShipAPI carrier;

    StrikecraftEmpListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the EMP is added for this hit only and restored after it
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(fighter, targetShip)) {
            return null;
        }
        float percent = FighterListenerEffect.carrierValue(carrier, EMP_PERCENT_KEY);
        if (percent > 0f) {
            HitDamageRestorer.addEmpForThisHit(damage, damage.getDamage() * percent / 100f, target);
        }
        return null;
    }
}
