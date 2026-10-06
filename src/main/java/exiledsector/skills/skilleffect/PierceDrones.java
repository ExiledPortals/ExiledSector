package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.ui.util.FallbackSupport;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class PierceDrones {

    static final int MAX_DRONES = 8;

    private static final Logger LOG = Logger.getLogger(PierceDrones.class);

    private final ShipAPI firingShip;
    private final List<PierceDrone> drones = new ArrayList<>();

    PierceDrones(ShipAPI firingShip) {
        this.firingShip = firingShip;
    }

    ShipAPI firingShip() {
        return firingShip;
    }

    void launch(WeaponAPI weapon, ShipAPI hitShip, Vector2f impactPoint, float travelFacing) {
        PierceDrone drone = acquire(weapon);
        if (drone != null) {
            drone.launch(hitShip, impactPoint, travelFacing, weapon.getRange());
        }
    }

    void forget(PierceDrone drone) {
        drones.remove(drone);
    }

    private PierceDrone acquire(WeaponAPI weapon) {
        String weaponId = weapon.getSpec().getWeaponId();
        drones.removeIf(drone -> !drone.isInPlay());
        PierceDrone longestIdle = null;
        for (PierceDrone drone : drones) {
            if (!drone.isReady()) {
                continue;
            }
            if (drone.weaponId().equals(weaponId)) {
                return drone;
            }
            if (longestIdle == null || drone.idleSeconds() > longestIdle.idleSeconds()) {
                longestIdle = drone;
            }
        }
        if (drones.size() >= MAX_DRONES) {
            if (longestIdle == null) {
                return null;
            }
            longestIdle.remove();
        }
        return create(weapon);
    }

    private PierceDrone create(WeaponAPI weapon) {
        PierceDrone created = FallbackSupport.getOrFallback(
                () -> WeaponDroneFactory.createSingleShot(firingShip, weapon, drone -> new PierceDrone(drone, this)),
                null, LOG, "Piercing Rounds could not build a drone for weapon " + weapon.getSpec().getWeaponId()
                        + "; that weapon won't pierce for the rest of the session");
        if (created == null) {
            WeaponDroneFactory.markProjectileUnsupported(weapon);
            return null;
        }
        drones.add(created);
        return created;
    }
}
