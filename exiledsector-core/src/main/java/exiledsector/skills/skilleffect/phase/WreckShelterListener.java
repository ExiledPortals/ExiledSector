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
    private static final String MOD_ID_PREFIX = "exiledSector_noPhaseUpkeepNearWreck_";
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private boolean sheltered;

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
        float range = magnitude(RANGE_KEY);
        boolean nearWreck = range > 0f && ownerIsAliveNotHulk() && wreckWithin(ownerShip, range);
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
