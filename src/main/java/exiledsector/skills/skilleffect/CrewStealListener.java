package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

final class CrewStealListener implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_crewStealRange";
    static final String SKELETON_CREW_PERCENT_KEY = "exiledSector_crewStealSkeletonCrewPercent";
    private static final float CHECK_SECONDS = 0.25f;

    private final ShipAPI ship;
    private final IntervalUtil interval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;
    private FleetCrewLedger ledger;

    CrewStealListener(ShipAPI ship) {
        this.ship = ship;
        this.wrecks = new NearbyWrecks(ship, other -> CombatQueries.isHostile(ship, other) && isCrewedHull(other));
    }

    @Override
    public void advance(float amount) {
        if (!stealsForTheFleet() || !ship.isAlive() || ship.isHulk()) {
            return;
        }
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            return;
        }
        float range = ship.getMutableStats().getDynamic().getValue(RANGE_KEY, 0f);
        float percent = ship.getMutableStats().getDynamic().getValue(SKELETON_CREW_PERCENT_KEY, 0f);
        if (range <= 0f || percent <= 0f) {
            return;
        }
        for (ShipAPI wreck : wrecks.newWithin(range)) {
            if (ledger == null) {
                ledger = FleetCrewLedger.forCurrentCombat();
            }
            ledger.credit(wreck, skeletonCrew(wreck) * percent / 100f);
        }
    }

    private boolean stealsForTheFleet() {
        return ship.getOwner() == 0 && !ship.isAlly();
    }

    private static boolean isCrewedHull(ShipAPI other) {
        return !other.isFighter() && !other.isDrone() && !other.isStationModule() && other.getParentStation() == null;
    }

    static float skeletonCrew(ShipAPI wreck) {
        return wreck.getHullSpec() == null ? 0f
                : wreck.getMutableStats().getMinCrewMod().computeEffective(wreck.getHullSpec().getMinCrew());
    }
}
