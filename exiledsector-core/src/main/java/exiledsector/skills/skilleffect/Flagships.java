package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Flagships {

    private static final String CAPTAINED_SHIPS_KEY = "exiledSector_captainedShips";

    private Flagships() {
    }

    public static PersonAPI fleetCommanderOf(FleetMemberAPI member) {
        if (member == null) {
            return null;
        }
        PersonAPI commander = member.getFleetCommanderForStats();
        return commander != null ? commander : member.getFleetCommander();
    }

    public static boolean isFlagship(ShipAPI ship) {
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            return true;
        }
        PersonAPI commander = fleetCommanderOf(ship.getMutableStats().getFleetMember());
        return commander != null && commander == ship.getCaptain();
    }

    public static ShipAPI deployedFlagshipOf(ShipAPI ship) {
        PersonAPI commander = fleetCommanderOf(ship.getMutableStats().getFleetMember());
        CombatEngineAPI engine = Global.getCombatEngine();
        if (commander == null || engine == null || ship.getCaptain() == commander) {
            return null;
        }
        for (ShipAPI candidate : CaptainedShips.of(engine).captainedBy(commander)) {
            if (CombatQueries.isAlliedShip(ship, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static final class CaptainedShips {

        private final float builtAtSeconds;
        private final Map<PersonAPI, List<ShipAPI>> shipsByCaptain = new IdentityHashMap<>();

        private CaptainedShips(CombatEngineAPI engine, float nowSeconds) {
            builtAtSeconds = nowSeconds;
            for (ShipAPI ship : engine.getShips()) {
                if (ship.getCaptain() != null) {
                    shipsByCaptain.computeIfAbsent(ship.getCaptain(), captain -> new ArrayList<>(1)).add(ship);
                }
            }
        }

        static CaptainedShips of(CombatEngineAPI engine) {
            float nowSeconds = engine.getTotalElapsedTime(false);
            Map<String, Object> customData = engine.getCustomData();
            if (customData != null && customData.get(CAPTAINED_SHIPS_KEY) instanceof CaptainedShips cached
                    && Float.compare(cached.builtAtSeconds, nowSeconds) == 0) {
                return cached;
            }
            CaptainedShips built = new CaptainedShips(engine, nowSeconds);
            if (customData != null) {
                customData.put(CAPTAINED_SHIPS_KEY, built);
            }
            return built;
        }

        List<ShipAPI> captainedBy(PersonAPI captain) {
            return shipsByCaptain.getOrDefault(captain, List.of());
        }
    }
}
