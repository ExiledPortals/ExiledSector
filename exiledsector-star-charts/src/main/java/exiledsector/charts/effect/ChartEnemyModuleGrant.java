package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

final class ChartEnemyModuleGrant implements AdvanceableListener {

    private final ShipAPI parentShip;
    private final ChartEnemyShipEffects.Granted granted;

    ChartEnemyModuleGrant(ShipAPI parentShip, ChartEnemyShipEffects.Granted granted) {
        this.parentShip = parentShip;
        this.granted = granted;
    }

    @Override
    public void advance(float amount) {
        parentShip.removeListener(this);
        for (ShipAPI module : parentShip.getChildModulesCopy()) {
            ChartEnemyShipEffects.applyToModule(module, granted);
        }
    }
}
