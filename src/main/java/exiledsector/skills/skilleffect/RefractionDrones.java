package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

final class RefractionDrones extends SingleShotDrones<RefractionDrone> {

    private static final Logger LOG = Logger.getLogger(RefractionDrones.class);

    RefractionDrones(ShipAPI firingShip) {
        super(firingShip, LOG, "Refracting Projectiles", "refract");
    }

    void launch(EnergyChainListener chain, WeaponAPI weapon, ShipAPI hitShip, Vector2f impactPoint, ShipAPI target, ChainLink link) {
        RefractionDrone drone = acquire(weapon);
        if (drone != null) {
            drone.launch(chain, hitShip, impactPoint, target, link, weapon.getRange());
        }
    }

    @Override
    RefractionDrone build(ShipAPI drone) {
        return new RefractionDrone(drone, this);
    }
}
