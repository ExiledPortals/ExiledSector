package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

final class StrikecraftEmpListener extends HitSideEffectListener {

    static final String EMP_PERCENT_KEY = "exiledSector_strikecraftEmpDamagePercent";

    StrikecraftEmpListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, null);
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || CombatQueries.hostileShip(ownerShip, target) == null) {
            return;
        }
        float percent = magnitude(EMP_PERCENT_KEY);
        if (percent > 0f) {
            HitDamageRestorer.addEmpForThisHit(damage, damage.getDamage() * percent / 100f, target);
        }
    }
}
