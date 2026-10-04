package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.ui.util.FallbackSupport;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class RefractionDrones {

    static final int MAX_DRONES = 8;

    private static final Logger LOG = Logger.getLogger(RefractionDrones.class);

    private final ShipAPI firingShip;
    private final List<RefractionDrone> drones = new ArrayList<>();

    RefractionDrones(ShipAPI firingShip) {
        this.firingShip = firingShip;
    }

    ShipAPI firingShip() {
        return firingShip;
    }

    void launch(EnergyChainListener chain, WeaponAPI weapon, ShipAPI hitShip, Vector2f impactPoint, ShipAPI target, ChainLink link) {
        RefractionDrone drone = acquire(chain, weapon);
        if (drone != null) {
            drone.launch(hitShip, impactPoint, target, link, weapon.getRange());
        }
    }

    void forget(RefractionDrone drone) {
        drones.remove(drone);
    }

    private RefractionDrone acquire(EnergyChainListener chain, WeaponAPI weapon) {
        String weaponId = weapon.getSpec().getWeaponId();
        drones.removeIf(drone -> !drone.isInPlay());
        RefractionDrone longestIdle = null;
        for (RefractionDrone drone : drones) {
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
        return create(chain, weapon);
    }

    private RefractionDrone create(EnergyChainListener chain, WeaponAPI weapon) {
        RefractionDrone created = FallbackSupport.getOrFallback(
                () -> WeaponDroneFactory.createSingleShot(firingShip, weapon, drone -> new RefractionDrone(drone, this, chain)),
                null, LOG, "Refracting Projectiles could not build a drone for weapon " + weapon.getSpec().getWeaponId()
                        + "; that weapon won't refract for the rest of the session");
        if (created == null) {
            WeaponDroneFactory.markProjectileUnsupported(weapon);
            return null;
        }
        drones.add(created);
        return created;
    }
}
