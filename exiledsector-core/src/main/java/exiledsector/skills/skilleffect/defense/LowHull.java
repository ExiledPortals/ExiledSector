package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;

final class LowHull {

    static final float HULL_LEVEL = 0.5f;

    private LowHull() {
    }

    static boolean isBelowThreshold(ShipAPI ship) {
        return ship.getHullLevel() < HULL_LEVEL;
    }
}
