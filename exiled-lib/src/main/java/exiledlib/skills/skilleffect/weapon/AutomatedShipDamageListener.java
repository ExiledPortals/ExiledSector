package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class AutomatedShipDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledLib_damageToAutomatedShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledLib_automatedShipDamage_";

    AutomatedShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !Misc.isAutomated(targetShip) || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float percent = magnitude(DAMAGE_PERCENT_KEY);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
