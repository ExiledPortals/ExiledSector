package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;

final class OpeningVolley {

    static final float SECONDS = 30f;

    private OpeningVolley() {
    }

    static boolean isUnderway(ShipAPI ship) {
        return ship.getFullTimeDeployed() < SECONDS;
    }
}
