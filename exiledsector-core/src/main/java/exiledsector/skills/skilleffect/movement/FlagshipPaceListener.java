package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class FlagshipPaceListener extends ShipCombatListener implements AdvanceableListener {

    static final String FLAGSHIP_SPEED_SHARE_PERCENT_KEY = "exiledSector_flagshipPaceSpeedSharePercent";
    static final float SCAN_INTERVAL_SECONDS = 1f;
    private static final String MOD_ID = "exiledSector_flagshipPace";

    private float secondsSinceScan = SCAN_INTERVAL_SECONDS;
    private float appliedSpeedBonus;

    FlagshipPaceListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        secondsSinceScan += amount;
        if (secondsSinceScan < SCAN_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceScan = 0f;
        float speedBonus = ownerIsAliveNotHulk() ? speedBonus() : 0f;
        if (speedBonus == appliedSpeedBonus) {
            return;
        }
        appliedSpeedBonus = speedBonus;
        if (speedBonus > 0f) {
            ownerShip.getMutableStats().getMaxSpeed().modifyFlat(MOD_ID, speedBonus);
        } else {
            ownerShip.getMutableStats().getMaxSpeed().unmodifyFlat(MOD_ID);
        }
    }

    float speedBonus() {
        ShipAPI flagship = deployedFlagship();
        if (flagship == null || flagship.getHullSpec() == null || flagship.getHullSpec().getEngineSpec() == null) {
            return 0f;
        }
        return flagship.getHullSpec().getEngineSpec().getMaxSpeed() * magnitude(FLAGSHIP_SPEED_SHARE_PERCENT_KEY) / 100f;
    }

    private ShipAPI deployedFlagship() {
        PersonAPI commander = fleetCommander();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (commander == null || engine == null || ownerShip.getCaptain() == commander) {
            return null;
        }
        for (ShipAPI ship : engine.getShips()) {
            if (ship != ownerShip && ship.getOwner() == ownerShip.getOwner() && ship.getCaptain() == commander && !ship.isFighter()
                    && CombatQueries.isAliveNotHulk(ship)) {
                return ship;
            }
        }
        return null;
    }

    private PersonAPI fleetCommander() {
        FleetMemberAPI member = ownerShip.getMutableStats().getFleetMember();
        if (member == null) {
            return null;
        }
        PersonAPI commander = member.getFleetCommanderForStats();
        return commander != null ? commander : member.getFleetCommander();
    }
}
