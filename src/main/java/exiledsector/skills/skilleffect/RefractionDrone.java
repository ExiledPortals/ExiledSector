package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.Color;

final class RefractionDrone implements DamageDealtModifier, AdvanceableListener {

    static final float FIRE_TIMEOUT_SECONDS = 0.5f;
    static final float SHOT_SEARCH_SECONDS = 0.25f;
    static final float IDLE_REMOVE_SECONDS = 3f;
    static final String RANGE_MATCH_MOD_ID = "exiledSector_refractionRange";

    private static final float CLEARANCE = 10f;
    private static final float RANGE_TOLERANCE = 1f;
    private static final float STREAK_FULL_SECONDS = 0.05f;
    private static final float STREAK_FADE_SECONDS = 0.15f;
    private static final float DEFAULT_STREAK_WIDTH = 10f;
    private static final float MIN_STREAK_GAP = 1f;

    private final ShipAPI drone;
    private final WeaponAPI weapon;
    private final RefractionDrones pool;
    private final EnergyChainListener chain;
    private final String weaponId;
    private final Vector2f origin = new Vector2f();
    private final Vector2f aimPoint = new Vector2f();
    private ChainLink link;
    private ShipAPI target;
    private boolean firing;
    private boolean shotSeen;
    private boolean removed;
    private boolean markersMirrored;
    private float firingSeconds;
    private float firedSeconds;
    private float idleSeconds;

    RefractionDrone(ShipAPI drone, RefractionDrones pool, EnergyChainListener chain) {
        this.drone = drone;
        this.weapon = drone.getAllWeapons().get(0);
        this.pool = pool;
        this.chain = chain;
        this.weaponId = weapon.getSpec().getWeaponId();
    }

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

    void launch(ShipAPI hitShip, Vector2f impactPoint, ShipAPI newTarget, ChainLink newLink, float sourceRange) {
        WeaponDroneStats.mirror(pool.firingShip().getMutableStats(), drone.getMutableStats());
        matchRange(sourceRange);
        if (weapon.getChargeLevel() > 0f) {
            rearm();
        }
        link = newLink;
        target = newTarget;
        firing = true;
        shotSeen = false;
        firingSeconds = 0f;
        firedSeconds = 0f;
        idleSeconds = 0f;
        Vector2f exit = Refraction.origin(hitShip, impactPoint, newTarget.getLocation());
        Vector2f direction = VectorUtils.getDirectionalVector(exit, newTarget.getLocation());
        origin.set(exit.x + direction.x * CLEARANCE, exit.y + direction.y * CLEARANCE);
        aim();
        weapon.setForceFireOneFrame(true);
        drawStreak(impactPoint);
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
        if (firing) {
            advanceFiring(amount);
            return;
        }
        idleSeconds += amount;
        if (idleSeconds > IDLE_REMOVE_SECONDS) {
            remove();
        }
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI projectile && projectile.getWeapon() == weapon) {
            if (!ChainLink.isTagged(projectile) && link != null) {
                handBack(projectile);
            }
            chain.handleHit(projectile, hitTarget, damage, point, shieldHit);
        }
        return null;
    }

    void remove() {
        removed = true;
        firing = false;
        Global.getCombatEngine().removeEntity(drone);
        pool.forget(this);
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
            if (projectile.getWeapon() == weapon && !ChainLink.isTagged(projectile)) {
                handBack(projectile);
            }
        }
    }

    private void handBack(DamagingProjectileAPI projectile) {
        link.tag(projectile);
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

    private void aim() {
        Vector2f lead = AIUtils.getBestInterceptPoint(origin, weapon.getProjectileSpeed(), target.getLocation(), target.getVelocity());
        aimPoint.set(lead != null ? lead : target.getLocation());
        float facing = VectorUtils.getAngle(origin, aimPoint);
        drone.getLocation().set(origin);
        drone.getVelocity().set(0f, 0f);
        drone.setFacing(facing);
        drone.setShipTarget(target);
        Vector2f mouseTarget = drone.getMouseTarget();
        if (mouseTarget != null) {
            mouseTarget.set(aimPoint);
        }
        weapon.setFacing(facing);
    }

    private void matchRange(float sourceRange) {
        StatBonus rangeBonus = drone.getMutableStats().getEnergyWeaponRangeBonus();
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

    private void drawStreak(Vector2f impactPoint) {
        float gap = MathUtils.getDistance(impactPoint, origin);
        if (gap < MIN_STREAK_GAP || !(weapon.getSpec().getProjectileSpec() instanceof ProjectileSpecAPI spec)) {
            return;
        }
        Color core = spec.getCoreColor() != null ? spec.getCoreColor() : spec.getGlowColor();
        Color fringe = spec.getFringeColor() != null ? spec.getFringeColor() : spec.getGlowColor();
        if (core == null || fringe == null) {
            return;
        }
        float width = spec.getWidth() > 0f ? spec.getWidth() : DEFAULT_STREAK_WIDTH;
        MagicFakeBeamPlugin.addBeam(STREAK_FULL_SECONDS, STREAK_FADE_SECONDS, width, new Vector2f(impactPoint),
                VectorUtils.getAngle(impactPoint, origin), gap, core, fringe);
    }
}
