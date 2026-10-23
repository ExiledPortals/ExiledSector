package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.NearbyWrecks;
import exiledlib.skills.skilleffect.ShipCombatListener;
import exiledlib.skills.skilleffect.logistics.FleetCrewLedger;

final class CrewStealListener extends ShipCombatListener implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_crewStealRange";
    static final String SKELETON_CREW_PERCENT_KEY = "exiledSector_crewStealSkeletonCrewPercent";
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;
    private final FleetCrewLedger.Lazy crewLedger = new FleetCrewLedger.Lazy();

    CrewStealListener(ShipAPI ownerShip) {
        super(ownerShip);
        this.wrecks = new NearbyWrecks(ownerShip, other -> CombatQueries.isHostile(ownerShip, other) && isCrewedHull(other));
    }

    @Override
    public void advance(float amount) {
        if (!FleetCrewLedger.usesFleetCrew(ownerShip) || !ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float range = magnitude(RANGE_KEY);
        float percent = magnitude(SKELETON_CREW_PERCENT_KEY);
        if (range <= 0f || percent <= 0f) {
            return;
        }
        for (ShipAPI wreck : wrecks.newWithin(range)) {
            crewLedger.get().credit(wreck, skeletonCrew(wreck) * percent / 100f);
        }
    }

    private static boolean isCrewedHull(ShipAPI other) {
        return !other.isFighter() && !other.isDrone() && !other.isStationModule() && other.getParentStation() == null;
    }

    static float skeletonCrew(ShipAPI wreck) {
        return wreck.getHullSpec() == null ? 0f
                : wreck.getMutableStats().getMinCrewMod().computeEffective(wreck.getHullSpec().getMinCrew());
    }
}
