package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.ui.util.FallbackSupport;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

abstract class SingleShotDrones<D extends SingleShotDrone> {

    static final int MAX_DRONES = 8;

    private final ShipAPI firingShip;
    private final Logger log;
    private final String effectName;
    private final String effectVerb;
    private final List<D> drones = new ArrayList<>();

    protected SingleShotDrones(ShipAPI firingShip, Logger log, String effectName, String effectVerb) {
        this.firingShip = firingShip;
        this.log = log;
        this.effectName = effectName;
        this.effectVerb = effectVerb;
    }

    abstract D build(ShipAPI drone);

    ShipAPI firingShip() {
        return firingShip;
    }

    void forget(SingleShotDrone drone) {
        drones.remove(drone);
    }

    D acquire(WeaponAPI weapon) {
        String weaponId = weapon.getSpec().getWeaponId();
        drones.removeIf(drone -> !drone.isInPlay());
        D longestIdle = null;
        for (D drone : drones) {
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

    private D create(WeaponAPI weapon) {
        D created = FallbackSupport.getOrFallback(
                () -> WeaponDroneFactory.createSingleShot(firingShip, weapon, this::build),
                null, log, effectName + " could not build a drone for weapon " + weapon.getSpec().getWeaponId()
                        + "; that weapon won't " + effectVerb + " for the rest of the session");
        if (created == null) {
            WeaponDroneFactory.markProjectileUnsupported(weapon);
            return null;
        }
        drones.add(created);
        return created;
    }
}
