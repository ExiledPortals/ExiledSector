package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

public final class AllyProximity {

    public static final float ALLY_RANGE = 1000f;
    static final float RECHECK_SECONDS = 0.5f;
    private static final String READING_KEY = "exiledSector_scarAllyProximity";

    private AllyProximity() {
    }

    public static boolean hasAllyNearby(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return false;
        }
        float now = engine.getTotalElapsedTime(false);
        Reading reading = ship.getCustomData().get(READING_KEY) instanceof Reading stored ? stored : null;
        if (reading == null) {
            reading = new Reading();
            ship.setCustomData(READING_KEY, reading);
        }
        if (now >= reading.nextCheckTime) {
            reading.allyNearby = scan(engine, ship);
            reading.nextCheckTime = now + RECHECK_SECONDS;
        }
        return reading.allyNearby;
    }

    static boolean scan(CombatEngineAPI engine, ShipAPI ship) {
        return CombatQueries.anyNear(engine.getShipGrid(), ship.getLocation(), ALLY_RANGE,
                candidate -> candidate instanceof ShipAPI other && isNearbyAlly(ship, other));
    }

    static boolean isNearbyAlly(ShipAPI ship, ShipAPI other) {
        return CombatQueries.isAlliedShip(ship, other) && !CombatQueries.isSameStation(ship, other)
                && CombatQueries.withinRadius(other.getLocation(), ship.getLocation(), ALLY_RANGE);
    }

    private static final class Reading {
        private float nextCheckTime = -1f;
        private boolean allyNearby;
    }
}
