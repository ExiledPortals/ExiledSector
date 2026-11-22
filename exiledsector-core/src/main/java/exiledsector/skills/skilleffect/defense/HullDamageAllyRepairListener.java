package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.DealtHullDamage;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.TickTimer;

import java.util.List;

final class HullDamageAllyRepairListener extends ShipCombatListener implements AdvanceableListener, DealtHullDamage.Receiver {

    static final String REPAIR_PERCENT_KEY = "exiledSector_hullDamageAllyRepairPercent";
    static final float RANGE = 2000f;
    private static final float REPAIR_SECONDS = 0.5f;

    private final TickTimer repairTick = new TickTimer(REPAIR_SECONDS);
    private float pendingRepair;

    HullDamageAllyRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
        DealtHullDamage.register(ownerShip, this);
    }

    float pendingRepair() {
        return pendingRepair;
    }

    @Override
    public void onHullDamageDealt(ShipAPI target, float hullDamage) {
        if (!target.isHulk() && CombatQueries.isHostile(ownerShip, target) && ownerIsAliveNotHulk()) {
            pendingRepair += hullDamage * magnitude(REPAIR_PERCENT_KEY) / 100f;
        }
    }

    @Override
    public void advance(float amount) {
        if (!repairTick.advance(amount) || pendingRepair <= 0f) {
            return;
        }
        float repair = pendingRepair;
        pendingRepair = 0f;
        if (ownerIsAliveNotHulk()) {
            repairMostDamaged(CombatQueries.shipsWithin(ownerShip.getLocation(), RANGE, this::isRepairable), repair);
        }
    }

    private boolean isRepairable(ShipAPI ship) {
        return CombatQueries.isAlliedShipOfSide(ownerShip.getOwner(), ship) && !ship.isStationModule();
    }

    static void repairMostDamaged(List<ShipAPI> allies, float repair) {
        ShipAPI mostDamaged = null;
        float mostMissingHull = 0f;
        for (ShipAPI ally : allies) {
            float missingHull = ally.getMaxHitpoints() - ally.getHitpoints();
            if (missingHull > mostMissingHull) {
                mostMissingHull = missingHull;
                mostDamaged = ally;
            }
        }
        if (mostDamaged != null) {
            mostDamaged.setHitpoints(mostDamaged.getHitpoints() + Math.min(repair, mostMissingHull));
        }
    }
}
