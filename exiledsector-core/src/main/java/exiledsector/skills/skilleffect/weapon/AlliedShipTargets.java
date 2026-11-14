package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class AlliedShipTargets {

    static final float REFRESH_INTERVAL_SECONDS = 0.5f;
    private static final String TARGETS_BY_SIDE_KEY = "exiledSector_alliedShipTargets";

    private final int side;
    private final Map<ShipAPI, List<ShipAPI>> targetersByTarget = new IdentityHashMap<>();
    private float nextRefreshSeconds = Float.NEGATIVE_INFINITY;

    private AlliedShipTargets(int side) {
        this.side = side;
    }

    static AlliedShipTargets forSide(CombatEngineAPI engine, int side) {
        Map<String, Object> customData = engine.getCustomData();
        TargetsBySide targetsBySide = customData.get(TARGETS_BY_SIDE_KEY) instanceof TargetsBySide stored ? stored : null;
        if (targetsBySide == null) {
            targetsBySide = new TargetsBySide();
            customData.put(TARGETS_BY_SIDE_KEY, targetsBySide);
        }
        AlliedShipTargets targets = targetsBySide.snapshotsBySide.computeIfAbsent(side, AlliedShipTargets::new);
        targets.refreshIfDue(engine);
        return targets;
    }

    private void refreshIfDue(CombatEngineAPI engine) {
        float nowSeconds = engine.getTotalElapsedTime(false);
        if (nowSeconds < nextRefreshSeconds) {
            return;
        }
        nextRefreshSeconds = nowSeconds + REFRESH_INTERVAL_SECONDS;
        refresh(engine.getShips());
    }

    private void refresh(List<ShipAPI> ships) {
        targetersByTarget.clear();
        for (int i = 0; i < ships.size(); i++) {
            ShipAPI ally = ships.get(i);
            ShipAPI allyTarget = CombatQueries.isAlliedShipOfSide(side, ally) ? ally.getShipTarget() : null;
            if (allyTarget != null && CombatQueries.isHostileToSide(side, allyTarget) && CombatQueries.isAliveNotHulk(allyTarget)) {
                targetersByTarget.computeIfAbsent(allyTarget, ignored -> new ArrayList<>(1)).add(ally);
            }
        }
    }

    boolean isTargetedByAllyOf(ShipAPI ownerShip, ShipAPI hitShip) {
        ShipAPI parentStation = hitShip.getParentStation();
        return hasLinkedTargeter(ownerShip, hitShip) || (parentStation != null && hasLinkedTargeter(ownerShip, parentStation));
    }

    private boolean hasLinkedTargeter(ShipAPI ownerShip, ShipAPI target) {
        List<ShipAPI> targeters = targetersByTarget.get(target);
        if (targeters == null) {
            return false;
        }
        for (int i = 0; i < targeters.size(); i++) {
            ShipAPI targeter = targeters.get(i);
            if (targeter != ownerShip && !CombatQueries.isSameStation(ownerShip, targeter)) {
                return true;
            }
        }
        return false;
    }

    private static final class TargetsBySide {
        private final Map<Integer, AlliedShipTargets> snapshotsBySide = new HashMap<>();
    }
}
