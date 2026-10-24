package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class SharedShieldDamageListener extends ShipCombatListener implements DamageTakenModifier, DamageListener {

    static final String SHARED_PERCENT_KEY = "exiledLib_shieldDamageSharedPercent";
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
    private float pendingShare;

    SharedShieldDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        sharePending = false;
        if (!shieldHit) return null;

        float share = Math.min(magnitude(SHARED_PERCENT_KEY), MAX_SHARED_PERCENT) / 100f;
        if (share <= 0f || nearbyAllies().isEmpty()) return null;

        damage.getModifier().modifyMult(SHARE_MOD_ID, 1f - share);
        sharePending = true;
        pendingSource = param;
        pendingShare = share;
        return SHARE_MOD_ID;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!sharePending || ownerShip.getParamAboutToApplyDamage() != pendingSource) return;
        sharePending = false;

        float moved = result.getDamageToShields() * pendingShare / (1f - pendingShare);
        if (moved <= 0f || eligibleAllies.isEmpty()) return;

        float perAlly = moved / eligibleAllies.size();
        for (ShipAPI ally : eligibleAllies) {
            ally.getFluxTracker().increaseFlux(perAlly, true);
        }
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
