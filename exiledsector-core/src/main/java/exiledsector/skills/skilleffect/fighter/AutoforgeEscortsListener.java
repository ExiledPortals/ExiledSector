package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class AutoforgeEscortsListener extends ShipCombatListener implements AdvanceableListener {

    static final String DRONES_KEY = "exiledSector_autoforgeEscortDrones";
    static final String DRONE_WING_ID = "terminator_wing";
    static final float REBUILD_SECONDS = 60f;
    static final int MAX_DRONES = 8;
    static final float CHECK_INTERVAL_SECONDS = 0.5f;
    private static final String FEATURE_NAME = "Autoforge Escorts";
    private static final float LAUNCH_SPREAD = 40f;
    private static final Logger LOG = Logger.getLogger(AutoforgeEscortsListener.class);

    interface DroneForge {
        ShipAPI build(ShipAPI ownerShip);
    }

    private final DroneForge forge;
    private final List<ShipAPI> drones = new ArrayList<>();
    private final List<Float> rebuildSecondsLeft = new ArrayList<>();
    private float secondsSinceCheck = CHECK_INTERVAL_SECONDS;
    private boolean firstLaunchDone;
    private boolean forgeFailed;

    AutoforgeEscortsListener(ShipAPI ownerShip) {
        this(ownerShip, AutoforgeEscortsListener::launchDrone);
    }

    AutoforgeEscortsListener(ShipAPI ownerShip, DroneForge forge) {
        super(ownerShip);
        this.forge = forge;
    }

    static int droneCount(float magnitude) {
        return Math.min(MAX_DRONES, Math.max(0, Math.round(magnitude)));
    }

    @Override
    public void advance(float amount) {
        if (forgeFailed || ownerShip.isFighter() || !ownerIsAliveNotHulk()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        secondsSinceCheck += amount;
        if (engine == null || secondsSinceCheck < CHECK_INTERVAL_SECONDS || inDeploymentBurn()) {
            return;
        }
        float elapsed = secondsSinceCheck;
        secondsSinceCheck = 0f;
        if (!firstLaunchDone) {
            firstLaunchDone = true;
            int dronesToForge = droneCount(magnitude(DRONES_KEY));
            for (int i = 0; i < dronesToForge && !forgeFailed; i++) {
                forgeDrone();
            }
            return;
        }
        queueRebuildsForLostDrones(engine);
        for (int i = rebuildSecondsLeft.size() - 1; i >= 0 && !forgeFailed; i--) {
            float secondsLeft = rebuildSecondsLeft.get(i) - elapsed;
            if (secondsLeft > 0f) {
                rebuildSecondsLeft.set(i, secondsLeft);
            } else {
                rebuildSecondsLeft.remove(i);
                forgeDrone();
            }
        }
    }

    int livingDrones() {
        return drones.size();
    }

    int dronesAwaitingRebuild() {
        return rebuildSecondsLeft.size();
    }

    private boolean inDeploymentBurn() {
        ShipSystemAPI travelDrive = ownerShip.getTravelDrive();
        return travelDrive != null && travelDrive.isActive();
    }

    private void queueRebuildsForLostDrones(CombatEngineAPI engine) {
        for (int i = drones.size() - 1; i >= 0; i--) {
            ShipAPI drone = drones.get(i);
            if (!CombatQueries.isAliveNotHulk(drone) || !engine.isEntityInPlay(drone)) {
                drones.remove(i);
                rebuildSecondsLeft.add(REBUILD_SECONDS);
            }
        }
    }

    private void forgeDrone() {
        ShipAPI drone;
        try {
            drone = forge.build(ownerShip);
        } catch (RuntimeException e) {
            LOG.error("Couldn't build an escort drone for " + ownerShip.getName(), e);
            drone = null;
        }
        if (drone == null) {
            forgeFailed = true;
            return;
        }
        drones.add(drone);
    }

    private static ShipAPI launchDrone(ShipAPI ownerShip) {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(ownerShip.getOwner());
        FleetMemberAPI wingMember = Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, DRONE_WING_ID);
        CarrierLaunch.readyTemporaryWingMember(wingMember, ownerShip);
        Vector2f launchLocation = new Vector2f(ownerShip.getLocation());
        launchLocation.translate(spread(), spread());
        float launchFacing = ownerShip.getFacing();
        ShipAPI drone = CarrierLaunch.spawnLoneFighter(engine, fleetManager, wingMember, DRONE_WING_ID, launchLocation, launchFacing, true,
                FEATURE_NAME);
        if (drone == null) {
            return null;
        }
        CarrierLaunch.applyCarrierReadiness(drone, ownerShip);
        drone.setFacing(launchFacing);
        FighterWingAPI escortWing = drone.getWing();
        if (escortWing != null) {
            escortWing.setSourceShip(ownerShip);
        }
        drone.setShipAI(Global.getSettings().pickShipAIPlugin(null, drone));
        CarrierLaunch.applyCarrierBonuses(drone, ownerShip, fleetManager);
        CarrierLaunch.finishLaunch(drone, ownerShip, launchLocation);
        drone.addListener(new EscortTether(drone, ownerShip));
        return drone;
    }

    private static float spread() {
        return ((float) Math.random() - 0.5f) * 2f * LAUNCH_SPREAD;
    }

    static final class EscortTether implements AdvanceableListener {

        private final ShipAPI drone;
        private final ShipAPI mothership;
        private float secondsSinceCheck;

        EscortTether(ShipAPI drone, ShipAPI mothership) {
            this.drone = drone;
            this.mothership = mothership;
        }

        @Override
        public void advance(float amount) {
            secondsSinceCheck += amount;
            if (secondsSinceCheck < CHECK_INTERVAL_SECONDS) {
                return;
            }
            secondsSinceCheck = 0f;
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null && (!CombatQueries.isAliveNotHulk(mothership) || !engine.isEntityInPlay(mothership))) {
                FighterWingAPI escortWing = drone.getWing();
                if (escortWing != null) {
                    escortWing.removeMember(drone);
                }
                engine.removeEntity(drone);
            }
        }
    }
}
