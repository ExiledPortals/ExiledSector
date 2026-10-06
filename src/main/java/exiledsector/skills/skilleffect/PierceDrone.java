package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

final class PierceDrone extends SingleShotDrone {

    static final float MAX_DEVIATION_DEGREES = 8f;
    static final String RANGE_MATCH_MOD_ID = "exiledSector_pierceRange";

    private static final float MIN_TRAVEL_SPEED = 100f;

    private final Vector2f localImpact = new Vector2f();
    private final Vector2f direction = new Vector2f();
    private final Vector2f fireDirection = new Vector2f();
    private ShipAPI hitShip;
    private boolean crossing;
    private float crossingSeconds;
    private float crossedSeconds;
    private float travelFacing;
    private float fireFacing;

    PierceDrone(ShipAPI drone, PierceDrones pool) {
        super(drone, pool, RANGE_MATCH_MOD_ID);
    }

    void launch(ShipAPI hitShip, Vector2f impactPoint, float travelFacing, float sourceRange) {
        prepare(sourceRange);
        this.hitShip = hitShip;
        direction.set(Misc.getUnitVectorAtDegreeAngle(travelFacing));
        toShipFrame(hitShip, impactPoint, localImpact);
        updateOrigin();
        float speed = Math.max(MIN_TRAVEL_SPEED, weapon().getProjectileSpeed());
        crossingSeconds = MathUtils.getDistance(impactPoint, origin()) / speed;
        this.travelFacing = travelFacing;
        park();
        crossing = true;
        crossedSeconds = 0f;
    }

    @Override
    boolean isReady() {
        return !crossing && super.isReady();
    }

    @Override
    void remove() {
        crossing = false;
        hitShip = null;
        super.remove();
    }

    @Override
    void advanceState(float amount) {
        if (crossing) {
            advanceCrossing(amount);
        } else {
            super.advanceState(amount);
        }
    }

    @Override
    StatBonus rangeBonus(MutableShipStatsAPI stats) {
        return stats.getBallisticWeaponRangeBonus();
    }

    @Override
    boolean isTagged(DamagingProjectileAPI projectile) {
        return BallisticPierceListener.isPierced(projectile);
    }

    @Override
    void tag(DamagingProjectileAPI projectile) {
        BallisticPierceListener.markPierced(projectile);
    }

    @Override
    void shotHit(DamagingProjectileAPI projectile, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!isTagged(projectile)) {
            handBack(projectile);
        }
    }

    @Override
    void aim() {
        Vector2f origin = origin();
        float range = weapon().getRange();
        aimAlong(fireFacing, null, origin.x + fireDirection.x * range, origin.y + fireDirection.y * range);
    }

    private void advanceCrossing(float amount) {
        crossedSeconds += amount;
        updateOrigin();
        if (crossedSeconds < crossingSeconds) {
            park();
            return;
        }
        fireFacing = travelFacing + deviationAwayFrom(hitShip);
        fireDirection.set(Misc.getUnitVectorAtDegreeAngle(fireFacing));
        hitShip = null;
        crossing = false;
        fire();
    }

    private float deviationAwayFrom(ShipAPI ship) {
        float deviation = MathUtils.getRandomNumberInRange(0f, MAX_DEVIATION_DEGREES);
        if (ship == null) {
            return deviation;
        }
        Vector2f fromCentre = Vector2f.sub(origin(), ship.getLocation(), null);
        float side = direction.x * fromCentre.y - direction.y * fromCentre.x;
        return side >= 0f ? deviation : -deviation;
    }

    private void updateOrigin() {
        if (hitShip == null || !Global.getCombatEngine().isEntityInPlay(hitShip)) {
            return;
        }
        Vector2f currentImpact = toWorldFrame(hitShip, localImpact);
        float exit = Refraction.exitDistance(hitShip.getLocation(), hitShip.getCollisionRadius(), currentImpact, direction);
        ShieldAPI shield = hitShip.getShield();
        if (shield != null && shield.isOn()) {
            exit = Math.max(exit, Refraction.exitDistance(hitShip.getShieldCenterEvenIfNoShield(), shield.getRadius(), currentImpact, direction));
        }
        float travel = exit + CLEARANCE;
        origin().set(currentImpact.x + direction.x * travel, currentImpact.y + direction.y * travel);
    }

    private static void toShipFrame(ShipAPI ship, Vector2f world, Vector2f local) {
        float radians = (float) Math.toRadians(-ship.getFacing());
        float dx = world.x - ship.getLocation().x;
        float dy = world.y - ship.getLocation().y;
        local.set(dx * (float) Math.cos(radians) - dy * (float) Math.sin(radians), dx * (float) Math.sin(radians) + dy * (float) Math.cos(radians));
    }

    private static Vector2f toWorldFrame(ShipAPI ship, Vector2f local) {
        float radians = (float) Math.toRadians(ship.getFacing());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector2f(ship.getLocation().x + local.x * cos - local.y * sin, ship.getLocation().y + local.x * sin + local.y * cos);
    }
}
