package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatTarget;

final class FormationSpeedListener extends ShipCombatListener implements AdvanceableListener {

    static final String SPEED_PERCENT_PER_ALLY_KEY = "exiledLib_formationSpeedPercentPerAlly";
    static final float RANGE = 1000f;
    static final float MAX_SPEED_PERCENT = 10f;
    static final float SCAN_INTERVAL_SECONDS = 0.5f;
    private static final String MOD_ID_PREFIX = "exiledLib_formationSpeed_";
    private static final ScaledBonus FORMATION_SPEED = new ScaledBonus(ScaledBonus.percent(StatTarget.liveStat(MutableShipStatsAPI::getMaxSpeed)));

    private final ScaledBonus.Tracker formationSpeed = FORMATION_SPEED.tracker();
    private float secondsSinceScan = SCAN_INTERVAL_SECONDS;

    FormationSpeedListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float speedPercent(float percentPerAlly, int nearbyAllies) {
        return Math.min(MAX_SPEED_PERCENT, Math.max(0f, percentPerAlly) * nearbyAllies);
    }

    @Override
    public void advance(float amount) {
        secondsSinceScan += amount;
        if (secondsSinceScan < SCAN_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceScan = 0f;
        int nearbyAllies = ownerIsAliveNotHulk() ? CombatQueries.shipsWithin(ownerShip.getLocation(), RANGE, this::isFormationAlly).size() : 0;
        formationSpeed.apply(ownerShip.getMutableStats(), modId, speedPercent(magnitude(SPEED_PERCENT_PER_ALLY_KEY), nearbyAllies));
    }

    private boolean isFormationAlly(ShipAPI ship) {
        return ship != ownerShip && ship.getOwner() == ownerShip.getOwner() && !ship.isFighter() && !ship.isDrone()
                && !ship.isStationModule() && CombatQueries.isAliveNotHulk(ship);
    }
}
