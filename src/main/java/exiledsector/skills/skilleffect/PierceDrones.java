package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

final class PierceDrones extends SingleShotDrones<PierceDrone> {

    private static final Logger LOG = Logger.getLogger(PierceDrones.class);

    PierceDrones(ShipAPI firingShip) {
        super(firingShip, LOG, "Piercing Rounds", "pierce");
    }

    void launch(WeaponAPI weapon, ShipAPI hitShip, Vector2f impactPoint, float travelFacing) {
        PierceDrone drone = acquire(weapon);
        if (drone != null) {
            drone.launch(hitShip, impactPoint, travelFacing, weapon.getRange());
        }
    }

    @Override
    PierceDrone build(ShipAPI drone) {
        return new PierceDrone(drone, this);
    }
}
