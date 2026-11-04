package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class SlowerShipDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToSlowerShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_slowerShipDamage_";

    SlowerShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float currentTopSpeed(ShipAPI ship) {
        ShipEngineControllerAPI engines = ship.getEngineController();
        if (engines != null && engines.isFlamedOut()) {
            return 0f;
        }
        return ship.getMaxSpeed() * ship.getMutableStats().getTimeMult().getModifiedValue();
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || targetShip.isFighter() || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float percent = magnitude(DAMAGE_PERCENT_KEY);
        if (percent == 0f || currentTopSpeed(targetShip) >= currentTopSpeed(ownerShip)) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
