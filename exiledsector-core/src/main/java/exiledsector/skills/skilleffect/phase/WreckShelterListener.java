package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class WreckShelterListener extends ShipCombatListener implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_noPhaseUpkeepNearWreckRange";
    static final String CAPPED_RANGE_KEY = "exiledSector_noPhaseUpkeepNearWreckCappedRange";
    static final float CAPPED_SECONDS = 5f;
    private static final String MOD_ID_PREFIX = "exiledSector_noPhaseUpkeepNearWreck_";
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private boolean sheltered;
    private float cappedSecondsUsed;

    WreckShelterListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        if (ownerShip.getPhaseCloak() == null) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        boolean nearWreck = ownerIsAliveNotHulk() && isSheltered(checkInterval.getIntervalDuration());
        if (nearWreck == sheltered) {
            return;
        }
        sheltered = nearWreck;
        StatBonus upkeep = ownerShip.getMutableStats().getPhaseCloakUpkeepCostBonus();
        if (nearWreck) {
            upkeep.modifyMult(modId, 0f);
        } else {
            upkeep.unmodifyMult(modId);
        }
    }

    private boolean isSheltered(float elapsedSeconds) {
        boolean phased = ownerShip.isPhased();
        if (!phased) {
            cappedSecondsUsed = 0f;
        }
        float range = magnitude(RANGE_KEY);
        if (range > 0f && wreckWithin(ownerShip, range)) {
            return true;
        }
        float cappedRange = magnitude(CAPPED_RANGE_KEY);
        if (cappedRange <= 0f || cappedSecondsUsed >= CAPPED_SECONDS || !wreckWithin(ownerShip, cappedRange)) {
            return false;
        }
        if (phased) {
            cappedSecondsUsed += elapsedSeconds;
        }
        return true;
    }

    static boolean wreckWithin(ShipAPI ship, float range) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return false;
        }
        Vector2f location = ship.getLocation();
        return CombatQueries.anyNear(engine.getShipGrid(), location, range, nearby -> nearby instanceof ShipAPI other && other != ship
                && other.isHulk() && !other.isFighter() && CombatQueries.withinRadius(other.getLocation(), location, range));
    }
}
