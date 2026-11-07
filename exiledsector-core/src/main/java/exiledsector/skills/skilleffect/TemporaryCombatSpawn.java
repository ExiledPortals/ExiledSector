package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lwjgl.util.vector.Vector2f;

public final class TemporaryCombatSpawn {

    public interface SettlingFailure {
        void report(RuntimeException error, ShipAPI settledShip);
    }

    private TemporaryCombatSpawn() {
    }

    public static ShipAPI spawnQuietly(CombatFleetManagerAPI fleetManager, FleetMemberAPI temporaryMember, Vector2f location, float facing,
                                       SettlingFailure onSettlingFailure) {
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);
        try {
            return fleetManager.spawnFleetMember(temporaryMember, new Vector2f(location), facing, 0f);
        } catch (RuntimeException e) {
            ShipAPI settledShip = fleetManager.getShipFor(temporaryMember);
            onSettlingFailure.report(e, settledShip);
            return settledShip;
        } finally {
            fleetManager.setSuppressDeploymentMessages(wasSuppressed);
        }
    }

    public static void dismiss(CombatEngineAPI engine, CombatFleetManagerAPI fleetManager, ShipAPI ship, FleetMemberAPI temporaryMember) {
        fleetManager.removeDeployed(ship, false);
        engine.removeEntity(ship);
        DeploymentRecordPruner.forget(fleetManager, temporaryMember);
    }
}
