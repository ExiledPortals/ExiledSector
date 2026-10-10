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
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

abstract class SingleShotDrone implements DamageDealtModifier, AdvanceableListener {

    static final float FIRE_TIMEOUT_SECONDS = 0.5f;
    static final float SHOT_SEARCH_SECONDS = 0.25f;
    static final float IDLE_REMOVE_SECONDS = 3f;
    static final float CLEARANCE = 10f;

    private static final float RANGE_TOLERANCE = 1f;

    private final ShipAPI droneShip;
    private final WeaponAPI droneWeapon;
    private final SingleShotDrones<?> dronePool;
    private final String weaponId;
    private final String rangeMatchModId;
    private final Vector2f origin = new Vector2f();
    private boolean firing;
    private boolean shotSeen;
    private boolean shotKept;
    private boolean removed;
    private boolean markersMirrored;
    private float firingSeconds;
    private float firedSeconds;
    private float idleSeconds;
    private float aimFacing;

    protected SingleShotDrone(ShipAPI droneShip, SingleShotDrones<?> dronePool, String rangeMatchModId) {
        this.droneShip = droneShip;
        this.droneWeapon = droneShip.getAllWeapons().get(0);
        this.dronePool = dronePool;
        this.weaponId = droneWeapon.getSpec().getWeaponId();
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
        return !removed && Global.getCombatEngine().isEntityInPlay(droneShip);
    }

    WeaponAPI weapon() {
        return droneWeapon;
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
            WeaponDroneFactory.mirrorMarkerHullMods(dronePool.firingShip(), droneShip);
            markersMirrored = true;
        }
        advanceState(amount);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI projectile && projectile.getWeapon() == droneWeapon) {
            shotHit(projectile, hitTarget, damage, point, shieldHit);
        }
        return null;
    }

    void remove() {
        removed = true;
        firing = false;
        Global.getCombatEngine().removeEntity(droneShip);
        dronePool.forget(this);
    }

    void prepare(float sourceRange) {
        WeaponDroneStats.mirror(dronePool.firingShip().getMutableStats(), droneShip.getMutableStats());
        matchRange(sourceRange);
        if (droneWeapon.getChargeLevel() > 0f) {
            rearm();
        }
        idleSeconds = 0f;
    }

    void fire() {
        firing = true;
        shotSeen = false;
        shotKept = false;
        firingSeconds = 0f;
        firedSeconds = 0f;
        aim();
        droneWeapon.setForceFireOneFrame(true);
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
        projectile.setSource(dronePool.firingShip());
        shotSeen = true;
        if (firing && hasFired()) {
            shotKept = true;
        }
    }

    void park() {
        droneShip.getLocation().set(origin);
        droneShip.getVelocity().set(0f, 0f);
    }

    void aimAlong(float facing, ShipAPI shipTarget, float mouseX, float mouseY) {
        park();
        aimFacing = facing;
        droneShip.setFacing(facing);
        droneShip.setShipTarget(shipTarget);
        Vector2f mouseTarget = droneShip.getMouseTarget();
        if (mouseTarget != null) {
            mouseTarget.set(mouseX, mouseY);
        }
        droneWeapon.setFacing(facing);
    }

    private void advanceFiring(float amount) {
        firingSeconds += amount;
        boolean fired = hasFired();
        tagShots(fired);
        if (fired) {
            firedSeconds += amount;
        }
        if ((fired && (shotSeen || firedSeconds >= SHOT_SEARCH_SECONDS)) || firingSeconds >= FIRE_TIMEOUT_SECONDS) {
            firing = false;
            idleSeconds = 0f;
            rearm();
        } else if (!fired) {
            aim();
            droneWeapon.setForceFireOneFrame(true);
        }
    }

    private boolean hasFired() {
        return droneWeapon.getChargeLevel() > 0f || droneWeapon.getCooldownRemaining() > 0f;
    }

    private void tagShots(boolean fired) {
        List<DamagingProjectileAPI> freshShots = null;
        for (DamagingProjectileAPI projectile : Global.getCombatEngine().getProjectiles()) {
            if (projectile.getWeapon() == droneWeapon && !isTagged(projectile)) {
                if (freshShots == null) {
                    freshShots = new ArrayList<>();
                }
                freshShots.add(projectile);
            }
        }
        if (freshShots == null) {
            return;
        }
        if (!fired) {
            freshShots.forEach(this::handBack);
            return;
        }
        DamagingProjectileAPI keptShot = shotKept ? null : closestToAim(freshShots);
        for (DamagingProjectileAPI projectile : freshShots) {
            if (projectile == keptShot) {
                handBack(projectile);
            } else {
                Global.getCombatEngine().removeEntity(projectile);
            }
        }
        shotKept = true;
    }

    private DamagingProjectileAPI closestToAim(List<DamagingProjectileAPI> shots) {
        DamagingProjectileAPI closest = null;
        float closestTurn = Float.MAX_VALUE;
        for (DamagingProjectileAPI shot : shots) {
            float turn = Math.abs(MathUtils.getShortestRotation(aimFacing, shot.getFacing()));
            if (turn < closestTurn) {
                closestTurn = turn;
                closest = shot;
            }
        }
        return closest;
    }

    private void rearm() {
        if (droneWeapon.isInBurst()) {
            droneWeapon.stopFiring();
        }
        if (droneWeapon.getCooldown() > 0f) {
            droneWeapon.setRemainingCooldownTo(0f);
        }
        if (droneWeapon.usesAmmo()) {
            droneWeapon.resetAmmo();
        }
    }

    private void matchRange(float sourceRange) {
        StatBonus rangeBonus = rangeBonus(droneShip.getMutableStats());
        rangeBonus.unmodify(rangeMatchModId);
        float current = droneWeapon.getRange();
        float missing = sourceRange - current;
        if (missing <= RANGE_TOLERANCE) {
            return;
        }
        rangeBonus.modifyFlat(rangeMatchModId, missing);
        float gained = droneWeapon.getRange() - current;
        if (gained > 0f) {
            rangeBonus.modifyFlat(rangeMatchModId, missing * missing / gained);
        }
    }
}
