package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.List;

final class ChartEnemyModuleGrant implements AdvanceableListener {

    private final ShipAPI parentShip;
    private final ChartEnemyShipEffects.Granted granted;
    private final List<String> nodeIdsGrantedWithoutTree;

    ChartEnemyModuleGrant(ShipAPI parentShip, ChartEnemyShipEffects.Granted granted, List<String> nodeIdsGrantedWithoutTree) {
        this.parentShip = parentShip;
        this.granted = granted;
        this.nodeIdsGrantedWithoutTree = List.copyOf(nodeIdsGrantedWithoutTree);
    }

    @Override
    public void advance(float amount) {
        parentShip.removeListener(this);
        for (ShipAPI module : parentShip.getChildModulesCopy()) {
            ChartEnemyShipEffects.applyToModule(module, granted, nodeIdsGrantedWithoutTree);
        }
    }
}
