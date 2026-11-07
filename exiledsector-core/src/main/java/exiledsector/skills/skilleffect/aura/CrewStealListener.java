package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.NearbyDestructionListener;
import exiledsector.skills.skilleffect.logistics.FleetCrewLedger;

import java.util.List;

final class CrewStealListener extends NearbyDestructionListener {

    static final String RANGE_KEY = "exiledSector_crewStealRange";
    static final String SKELETON_CREW_PERCENT_KEY = "exiledSector_crewStealSkeletonCrewPercent";

    private final FleetCrewLedger.Lazy crewLedger = new FleetCrewLedger.Lazy();

    CrewStealListener(ShipAPI ownerShip) {
        super(ownerShip, other -> CombatQueries.isHostile(ownerShip, other) && isCrewedHull(other));
    }

    @Override
    protected boolean canReact() {
        return FleetCrewLedger.usesFleetCrew(ownerShip) && super.canReact();
    }

    @Override
    protected float activeRange() {
        return magnitude(SKELETON_CREW_PERCENT_KEY) > 0f ? magnitude(RANGE_KEY) : 0f;
    }

    @Override
    protected void onDestroyed(List<ShipAPI> wrecks) {
        float percent = magnitude(SKELETON_CREW_PERCENT_KEY);
        for (ShipAPI wreck : wrecks) {
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
