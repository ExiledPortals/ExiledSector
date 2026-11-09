package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class CollisionDeathListener extends ShipCombatListener implements AdvanceableListener {

    static final String LARGE_ENEMIES_ONLY_KEY = "exiledSector_deathOnCollisionLargeEnemiesOnly";

    private static final float COLLISION_CHECK_GRACE_PERIOD = 1.5f;
    private static final float BROAD_PHASE_MARGIN = 1.1f;
    private static final float LETHAL_DAMAGE = 999999f;

    private final boolean largeEnemiesOnly;
    private boolean triggered;
    private float aliveTime;

    CollisionDeathListener(ShipAPI ownerShip) {
        super(ownerShip);
        largeEnemiesOnly = magnitude(LARGE_ENEMIES_ONLY_KEY) > 0f;
    }

    @Override
    public void advance(float amount) {
        if (triggered || !ownerIsAliveNotHulk()) {
            return;
        }
        aliveTime += amount;
        if (aliveTime < COLLISION_CHECK_GRACE_PERIOD || ownerShip.getCollisionClass() == CollisionClass.NONE) {
            return;
        }
        if (isHullTouchingAnotherShip()) {
            triggered = true;
            Global.getCombatEngine().applyDamage(ownerShip, ownerShip.getLocation(), LETHAL_DAMAGE,
                    DamageType.HIGH_EXPLOSIVE, 0f, true, false, ownerShip);
        }
    }

    private boolean isHullTouchingAnotherShip() {
        Vector2f loc = ownerShip.getLocation();
        float queryRadius = ownerShip.getCollisionRadius() * BROAD_PHASE_MARGIN;
        CombatEngineAPI engine = Global.getCombatEngine();
        return CombatQueries.anyNear(engine.getShipGrid(), loc, queryRadius,
                candidate -> candidate instanceof ShipAPI other && isCollidableShip(other) && isTouching(other));
    }

    private boolean isCollidableShip(ShipAPI other) {
        return other != ownerShip && !other.isFighter() && !other.isHulk() && !other.isShuttlePod()
                && other.getCollisionClass() != CollisionClass.NONE
                && !CombatQueries.isSameStation(ownerShip, other)
                && (!largeEnemiesOnly || isLargeEnemy(other));
    }

    private boolean isLargeEnemy(ShipAPI other) {
        HullSize hullSize = other.getHullSize();
        return CombatQueries.isHostile(ownerShip, other) && (hullSize == HullSize.CRUISER || hullSize == HullSize.CAPITAL_SHIP);
    }

    private boolean isTouching(CombatEntityAPI other) {
        return isBroadPhaseNear(ownerShip.getLocation(), ownerShip.getCollisionRadius(), other)
                && CombatQueries.boundsOverlap(ownerShip, other);
    }

    private boolean isBroadPhaseNear(Vector2f loc, float myRadius, CombatEntityAPI other) {
        float triggerRadius = (myRadius + other.getCollisionRadius()) * BROAD_PHASE_MARGIN;
        return CombatQueries.withinRadius(other.getLocation(), loc, triggerRadius);
    }
}
