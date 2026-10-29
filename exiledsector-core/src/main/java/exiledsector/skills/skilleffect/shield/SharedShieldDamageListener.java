package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class SharedShieldDamageListener extends ShipCombatListener implements DamageTakenModifier, DamageListener {

    static final String SHARED_PERCENT_KEY = "exiledLib_shieldDamageSharedPercent";
    static final String PASSED_PERCENT_KEY = "exiledLib_shieldDamagePassedPercent";
    static final float RANGE = 1000f;
    private static final String SHARE_MOD_ID = "exiledLib_shieldDamageShared";
    static final float MAX_SHARED_PERCENT = 90f;
    private static final float ALLY_SEARCH_INTERVAL_SECONDS = 0.25f;
    private static final float ALLY_SEARCH_MARGIN = 200f;

    private List<ShipAPI> candidateAllies = List.of();
    private final List<ShipAPI> eligibleAllies = new ArrayList<>();
    private float candidatesFoundAt = Float.NEGATIVE_INFINITY;
    private float eligibleCheckedAt = Float.NEGATIVE_INFINITY;
    private boolean sharePending;
    private Object pendingSource;
    private float pendingSharedShare;
    private float pendingPassedShare;
    private ShipAPI pendingPassTarget;

    SharedShieldDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        sharePending = false;
        if (!shieldHit) return null;

        float sharedShare = Math.max(magnitude(SHARED_PERCENT_KEY), 0f) / 100f;
        float passedShare = Math.max(magnitude(PASSED_PERCENT_KEY), 0f) / 100f;
        if (sharedShare + passedShare <= 0f || nearbyAllies().isEmpty()) return null;

        ShipAPI passTarget = passedShare > 0f ? nearestAllyWithFluxRoom() : null;
        if (passTarget == null) {
            passedShare = 0f;
        }
        float totalShare = sharedShare + passedShare;
        if (totalShare <= 0f) return null;
        float capScale = Math.min(1f, MAX_SHARED_PERCENT / 100f / totalShare);

        damage.getModifier().modifyMult(SHARE_MOD_ID, 1f - totalShare * capScale);
        sharePending = true;
        pendingSource = param;
        pendingSharedShare = sharedShare * capScale;
        pendingPassedShare = passedShare * capScale;
        pendingPassTarget = passTarget;
        return SHARE_MOD_ID;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!sharePending || ownerShip.getParamAboutToApplyDamage() != pendingSource) return;
        sharePending = false;

        float originalShieldDamage = result.getDamageToShields() / (1f - pendingSharedShare - pendingPassedShare);
        float shared = originalShieldDamage * pendingSharedShare;
        if (shared > 0f && !eligibleAllies.isEmpty()) {
            float perAlly = shared / eligibleAllies.size();
            for (ShipAPI ally : eligibleAllies) {
                ally.getFluxTracker().increaseFlux(perAlly, true);
            }
        }
        float passed = originalShieldDamage * pendingPassedShare;
        if (passed > 0f && pendingPassTarget != null) {
            float taken = Math.max(Math.min(passed, fluxRoom(pendingPassTarget)), 0f);
            pendingPassTarget.getFluxTracker().increaseFlux(taken, false);
            if (passed > taken) {
                ownerShip.getFluxTracker().increaseFlux(passed - taken, true);
            }
        }
        pendingPassTarget = null;
    }

    private ShipAPI nearestAllyWithFluxRoom() {
        ShipAPI nearest = null;
        float nearestDistanceSquared = Float.MAX_VALUE;
        for (ShipAPI ally : eligibleAllies) {
            float distanceSquared = MathUtils.getDistanceSquared(ally.getLocation(), ownerShip.getLocation());
            if (fluxRoom(ally) > 0f && distanceSquared < nearestDistanceSquared) {
                nearest = ally;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    private static float fluxRoom(ShipAPI ship) {
        return ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux();
    }

    private List<ShipAPI> nearbyAllies() {
        float now = Global.getCombatEngine().getTotalElapsedTime(false);
        if (now == eligibleCheckedAt) {
            return eligibleAllies;
        }
        eligibleCheckedAt = now;
        if (now < candidatesFoundAt || now - candidatesFoundAt >= ALLY_SEARCH_INTERVAL_SECONDS) {
            candidatesFoundAt = now;
            candidateAllies = CombatQueries.shipsNear(ownerShip.getLocation(), RANGE + ALLY_SEARCH_MARGIN,
                    this::isAlliedWarship);
        }
        eligibleAllies.clear();
        for (ShipAPI ally : candidateAllies) {
            if (canTakeSharedDamageNow(ally)) {
                eligibleAllies.add(ally);
            }
        }
        return eligibleAllies;
    }

    private boolean isAlliedWarship(ShipAPI other) {
        return other != ownerShip && other.getOwner() == ownerShip.getOwner() && !other.isFighter()
                && !CombatQueries.isSameStation(ownerShip, other);
    }

    private boolean canTakeSharedDamageNow(ShipAPI ally) {
        return CombatQueries.isAliveNotHulk(ally)
                && CombatQueries.withinRadius(ally.getLocation(), ownerShip.getLocation(), RANGE)
                && !ally.getFluxTracker().isOverloadedOrVenting();
    }
}
