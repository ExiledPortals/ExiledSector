package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;

final class HelplessShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToHelplessShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_helplessShipDamage_";

    HelplessShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
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
    protected boolean qualifies(ShipAPI target) {
        return isHelpless(target);
    }
}
