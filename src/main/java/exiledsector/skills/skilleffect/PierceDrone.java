package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

final class PierceDrone implements DamageDealtModifier, AdvanceableListener {

    static final float MAX_DEVIATION_DEGREES = 8f;
    static final float FIRE_TIMEOUT_SECONDS = 0.5f;
    static final float SHOT_SEARCH_SECONDS = 0.25f;
    static final float IDLE_REMOVE_SECONDS = 3f;
    static final String RANGE_MATCH_MOD_ID = "exiledSector_pierceRange";

    private static final float CLEARANCE = 10f;
    private static final float RANGE_TOLERANCE = 1f;
    private static final float MIN_TRAVEL_SPEED = 100f;

    private enum State {IDLE, CROSSING, FIRING}

    private final ShipAPI drone;
    private final WeaponAPI weapon;
    private final PierceDrones pool;
    private final String weaponId;
    private final Vector2f origin = new Vector2f();
    private final Vector2f localImpact = new Vector2f();
    private final Vector2f direction = new Vector2f();
    private ShipAPI hitShip;
    private State state = State.IDLE;
    private float crossingSeconds;
    private float travelFacing;
    private float fireFacing;
    private boolean shotSeen;
    private boolean removed;
    private boolean markersMirrored;
    private float stateSeconds;
    private float firedSeconds;
    private float idleSeconds;

    PierceDrone(ShipAPI drone, PierceDrones pool) {
        this.drone = drone;
        this.weapon = drone.getAllWeapons().get(0);
        this.pool = pool;
        this.weaponId = weapon.getSpec().getWeaponId();
    }

    String weaponId() {
        return weaponId;
    }

    boolean isReady() {
        return state == State.IDLE && !removed;
    }

    float idleSeconds() {
        return idleSeconds;
    }

    boolean isInPlay() {
        return !removed && Global.getCombatEngine().isEntityInPlay(drone);
    }

    void launch(ShipAPI hitShip, Vector2f impactPoint, float travelFacing, float sourceRange) {
        WeaponDroneStats.mirror(pool.firingShip().getMutableStats(), drone.getMutableStats());
        matchRange(sourceRange);
        if (weapon.getChargeLevel() > 0f) {
            rearm();
        }
        this.hitShip = hitShip;
        direction.set(Misc.getUnitVectorAtDegreeAngle(travelFacing));
        toShipFrame(hitShip, impactPoint, localImpact);
        updateOrigin();
        float speed = Math.max(MIN_TRAVEL_SPEED, weapon.getProjectileSpeed());
        crossingSeconds = MathUtils.getDistance(impactPoint, origin) / speed;
        this.travelFacing = travelFacing;
        fireFacing = travelFacing;
        park();
        state = State.CROSSING;
        stateSeconds = 0f;
        idleSeconds = 0f;
    }

    @Override
    public void advance(float amount) {
        if (removed) {
            return;
        }
        if (!markersMirrored) {
            WeaponDroneFactory.mirrorMarkerHullMods(pool.firingShip(), drone);
            markersMirrored = true;
        }
        switch (state) {
            case CROSSING -> advanceCrossing(amount);
            case FIRING -> advanceFiring(amount);
            case IDLE -> {
                idleSeconds += amount;
                if (idleSeconds > IDLE_REMOVE_SECONDS) {
                    remove();
                }
            }
        }
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI projectile && projectile.getWeapon() == weapon && !BallisticPierceListener.isPierced(projectile)) {
            handBack(projectile);
        }
        return null;
    }

    void remove() {
        removed = true;
        state = State.IDLE;
        hitShip = null;
        Global.getCombatEngine().removeEntity(drone);
        pool.forget(this);
    }

    private void advanceCrossing(float amount) {
        stateSeconds += amount;
        updateOrigin();
        if (stateSeconds < crossingSeconds) {
            park();
            return;
        }
        fireFacing = travelFacing + deviationAwayFrom(hitShip);
        hitShip = null;
        state = State.FIRING;
        stateSeconds = 0f;
        firedSeconds = 0f;
        shotSeen = false;
        aim();
        weapon.setForceFireOneFrame(true);
    }

    private void advanceFiring(float amount) {
        stateSeconds += amount;
        boolean fired = weapon.getChargeLevel() > 0f || weapon.getCooldownRemaining() > 0f;
        tagShots();
        if (fired) {
            firedSeconds += amount;
        }
        if ((fired && (shotSeen || firedSeconds >= SHOT_SEARCH_SECONDS)) || stateSeconds >= FIRE_TIMEOUT_SECONDS) {
            state = State.IDLE;
            idleSeconds = 0f;
            rearm();
        } else if (!fired) {
            aim();
            weapon.setForceFireOneFrame(true);
        }
    }

    private float deviationAwayFrom(ShipAPI ship) {
        float deviation = MathUtils.getRandomNumberInRange(0f, MAX_DEVIATION_DEGREES);
        if (ship == null) {
            return deviation;
        }
        Vector2f fromCentre = Vector2f.sub(origin, ship.getLocation(), null);
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
        origin.set(currentImpact.x + direction.x * travel, currentImpact.y + direction.y * travel);
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

    private void tagShots() {
        for (DamagingProjectileAPI projectile : Global.getCombatEngine().getProjectiles()) {
            if (projectile.getWeapon() == weapon && !BallisticPierceListener.isPierced(projectile)) {
                handBack(projectile);
            }
        }
    }

    private void handBack(DamagingProjectileAPI projectile) {
        BallisticPierceListener.markPierced(projectile);
        projectile.setSource(pool.firingShip());
        shotSeen = true;
    }

    private void rearm() {
        if (weapon.isInBurst()) {
            weapon.stopFiring();
        }
        if (weapon.getCooldown() > 0f) {
            weapon.setRemainingCooldownTo(0f);
        }
        if (weapon.usesAmmo()) {
            weapon.resetAmmo();
        }
    }

    private void park() {
        drone.getLocation().set(origin);
        drone.getVelocity().set(0f, 0f);
    }

    private void aim() {
        park();
        drone.setFacing(fireFacing);
        drone.setShipTarget(null);
        Vector2f mouseTarget = drone.getMouseTarget();
        if (mouseTarget != null) {
            Vector2f direction = Misc.getUnitVectorAtDegreeAngle(fireFacing);
            mouseTarget.set(origin.x + direction.x * weapon.getRange(), origin.y + direction.y * weapon.getRange());
        }
        weapon.setFacing(fireFacing);
    }

    private void matchRange(float sourceRange) {
        StatBonus rangeBonus = drone.getMutableStats().getBallisticWeaponRangeBonus();
        rangeBonus.unmodify(RANGE_MATCH_MOD_ID);
        float current = weapon.getRange();
        float missing = sourceRange - current;
        if (missing <= RANGE_TOLERANCE) {
            return;
        }
        rangeBonus.modifyFlat(RANGE_MATCH_MOD_ID, missing);
        float gained = weapon.getRange() - current;
        if (gained > 0f) {
            rangeBonus.modifyFlat(RANGE_MATCH_MOD_ID, missing * missing / gained);
        }
    }
}
