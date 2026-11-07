package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class StrikecraftEmpListener extends ShipCombatListener implements DamageDealtModifier {

    static final String EMP_PERCENT_KEY = "exiledSector_strikecraftEmpDamagePercent";

    StrikecraftEmpListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, null);
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the EMP is added for this hit only and restored after it
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float percent = magnitude(EMP_PERCENT_KEY);
        if (percent > 0f) {
            HitDamageRestorer.addEmpForThisHit(damage, damage.getDamage() * percent / 100f, target);
        }
        return null;
    }
}
