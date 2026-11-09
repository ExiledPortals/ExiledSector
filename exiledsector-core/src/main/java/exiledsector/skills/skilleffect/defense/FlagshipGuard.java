package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.Flagships;

final class FlagshipGuard {

    static final float RANGE = 1500f;

    private FlagshipGuard() {
    }

    static boolean isGuarding(ShipAPI ship) {
        if (!CombatQueries.isAliveNotHulk(ship)) {
            return false;
        }
        ShipAPI flagship = Flagships.deployedFlagshipOf(ship);
        return flagship != null && CombatQueries.withinRadius(ship.getLocation(), flagship.getLocation(), RANGE);
    }
}
