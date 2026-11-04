package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class HelplessShipDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledLib_damageToHelplessShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledLib_helplessShipDamage_";

    HelplessShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static boolean isHelpless(ShipAPI ship) {
        FluxTrackerAPI fluxTracker = ship.getFluxTracker();
        if (fluxTracker != null && (fluxTracker.isOverloaded() || fluxTracker.isVenting())) {
            return true;
        }
        ShipEngineControllerAPI engineController = ship.getEngineController();
        return engineController != null && engineController.isFlamedOut();
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(ownerShip, targetShip) || !isHelpless(targetShip)) {
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
