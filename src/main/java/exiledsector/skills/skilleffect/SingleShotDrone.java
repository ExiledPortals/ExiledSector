package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

abstract class SingleShotDrone implements DamageDealtModifier, AdvanceableListener {

    static final float FIRE_TIMEOUT_SECONDS = 0.5f;
    static final float SHOT_SEARCH_SECONDS = 0.25f;
    static final float IDLE_REMOVE_SECONDS = 3f;
    static final float CLEARANCE = 10f;

    private static final float RANGE_TOLERANCE = 1f;

    private final ShipAPI drone;
    private final WeaponAPI weapon;
    private final SingleShotDrones<?> pool;
    private final String weaponId;
    private final String rangeMatchModId;
    private final Vector2f origin = new Vector2f();
    private boolean firing;
    private boolean shotSeen;
    private boolean removed;
    private boolean markersMirrored;
    private float firingSeconds;
    private float firedSeconds;
    private float idleSeconds;

    protected SingleShotDrone(ShipAPI drone, SingleShotDrones<?> pool, String rangeMatchModId) {
        this.drone = drone;
        this.weapon = drone.getAllWeapons().get(0);
        this.pool = pool;
        this.weaponId = weapon.getSpec().getWeaponId();
        this.rangeMatchModId = rangeMatchModId;
    }

    abstract StatBonus rangeBonus(MutableShipStatsAPI stats);

    abstract boolean isTagged(DamagingProjectileAPI projectile);

    abstract void tag(DamagingProjectileAPI projectile);

    abstract void shotHit(DamagingProjectileAPI projectile, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit);

    abstract void aim();

    String weaponId() {
        return weaponId;
    }

    boolean isReady() {
        return !firing && !removed;
    }

    float idleSeconds() {
        return idleSeconds;
    }

    boolean isInPlay() {
        return !removed && Global.getCombatEngine().isEntityInPlay(drone);
    }

    WeaponAPI weapon() {
        return weapon;
    }

    Vector2f origin() {
        return origin;
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
        advanceState(amount);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI projectile && projectile.getWeapon() == weapon) {
            shotHit(projectile, hitTarget, damage, point, shieldHit);
        }
        return null;
    }

    void remove() {
        removed = true;
        firing = false;
        Global.getCombatEngine().removeEntity(drone);
        pool.forget(this);
    }

    void prepare(float sourceRange) {
        WeaponDroneStats.mirror(pool.firingShip().getMutableStats(), drone.getMutableStats());
        matchRange(sourceRange);
        if (weapon.getChargeLevel() > 0f) {
            rearm();
        }
        idleSeconds = 0f;
    }

    void fire() {
        firing = true;
        shotSeen = false;
        firingSeconds = 0f;
        firedSeconds = 0f;
        aim();
        weapon.setForceFireOneFrame(true);
    }

    void advanceState(float amount) {
        if (firing) {
            advanceFiring(amount);
            return;
        }
        idleSeconds += amount;
        if (idleSeconds > IDLE_REMOVE_SECONDS) {
            remove();
        }
    }

    void handBack(DamagingProjectileAPI projectile) {
        tag(projectile);
        projectile.setSource(pool.firingShip());
        shotSeen = true;
    }

    void park() {
        drone.getLocation().set(origin);
        drone.getVelocity().set(0f, 0f);
    }

    void aimAlong(float facing, ShipAPI shipTarget, float mouseX, float mouseY) {
        park();
        drone.setFacing(facing);
        drone.setShipTarget(shipTarget);
        Vector2f mouseTarget = drone.getMouseTarget();
        if (mouseTarget != null) {
            mouseTarget.set(mouseX, mouseY);
        }
        weapon.setFacing(facing);
    }

    private void advanceFiring(float amount) {
        firingSeconds += amount;
        boolean fired = weapon.getChargeLevel() > 0f || weapon.getCooldownRemaining() > 0f;
        tagShots();
        if (fired) {
            firedSeconds += amount;
        }
        if ((fired && (shotSeen || firedSeconds >= SHOT_SEARCH_SECONDS)) || firingSeconds >= FIRE_TIMEOUT_SECONDS) {
            firing = false;
            idleSeconds = 0f;
            rearm();
        } else if (!fired) {
            aim();
            weapon.setForceFireOneFrame(true);
        }
    }

    private void tagShots() {
        for (DamagingProjectileAPI projectile : Global.getCombatEngine().getProjectiles()) {
            if (projectile.getWeapon() == weapon && !isTagged(projectile)) {
                handBack(projectile);
            }
        }
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

    private void matchRange(float sourceRange) {
        StatBonus rangeBonus = rangeBonus(drone.getMutableStats());
        rangeBonus.unmodify(rangeMatchModId);
        float current = weapon.getRange();
        float missing = sourceRange - current;
        if (missing <= RANGE_TOLERANCE) {
            return;
        }
        rangeBonus.modifyFlat(rangeMatchModId, missing);
        float gained = weapon.getRange() - current;
        if (gained > 0f) {
            rangeBonus.modifyFlat(rangeMatchModId, missing * missing / gained);
        }
    }
}
