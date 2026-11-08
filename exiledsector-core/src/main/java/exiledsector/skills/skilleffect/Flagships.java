package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class Flagships {

    private Flagships() {
    }

    public static ShipAPI deployedFlagshipOf(ShipAPI ship) {
        PersonAPI commander = fleetCommanderOf(ship);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (commander == null || engine == null || ship.getCaptain() == commander) {
            return null;
        }
        for (ShipAPI candidate : engine.getShips()) {
            if (candidate.getCaptain() == commander && CombatQueries.isAlliedShip(ship, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static PersonAPI fleetCommanderOf(ShipAPI ship) {
        FleetMemberAPI member = ship.getMutableStats().getFleetMember();
        if (member == null) {
            return null;
        }
        PersonAPI commander = member.getFleetCommanderForStats();
        return commander != null ? commander : member.getFleetCommander();
    }
}
