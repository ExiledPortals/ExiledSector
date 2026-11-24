package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.compat.PrivateField;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ArcWeapons {

    static final float FIRE_POINT_MARGIN = 10f;
    static final float CHAIN_START_MARGIN = 10f;
    static final float CHAIN_MEMORY_SECONDS = 1f;

    private static final String UNREADABLE_ARC_WARNING = "EMP arcs no longer expose where they start, so arcs from weapon scripts skip damage conversions";
    static final PrivateField<Object> ARC_ANCHOR = PrivateField.named(EmpOnlyHits.ENGINE_ARC_CLASS, "pointAnchor", CombatEntityAPI.class,
            UNREADABLE_ARC_WARNING);
    static final PrivateField<Object> ARC_OFFSET = PrivateField.named(EmpOnlyHits.ENGINE_ARC_CLASS, "offset", Vector2f.class, UNREADABLE_ARC_WARNING);

    private record ScriptedWeapon(WeaponAPI weapon, Vector2f mountLocation, float firePointReach) {
    }

    private record ArcHit(WeaponAPI weapon, Vector2f hitOffset, float time) {
    }

    private final ShipAPI ownerShip;
    private final Map<ShipAPI, List<ScriptedWeapon>> scriptedWeaponsByShip = new WeakHashMap<>();
    private final Map<CombatEntityAPI, ArcHit> lastArcHitByTarget = new WeakHashMap<>();

    public ArcWeapons(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
    }

    public WeaponAPI weaponBehind(Object damageParam, CombatEntityAPI target) {
        WeaponAPI weapon = CombatQueries.weaponBehind(damageParam);
        if (weapon != null || !(damageParam instanceof EmpArcEntityAPI arc) || !EmpOnlyHits.isEngineArc(arc)) {
            return weapon;
        }
        WeaponAPI arcWeapon = arcWeapon(arc);
        if (arcWeapon != null && target != null) {
            lastArcHitByTarget.put(target, new ArcHit(arcWeapon, hitOffset(arc, target), now()));
        }
        return arcWeapon;
    }

    private WeaponAPI arcWeapon(EmpArcEntityAPI arc) {
        if (!(ARC_ANCHOR.read(arc) instanceof CombatEntityAPI anchor) || !(ARC_OFFSET.read(arc) instanceof Vector2f startOffset)) {
            return null;
        }
        if (anchor instanceof ShipAPI anchorShip && anchorShip.getOwner() == ownerShip.getOwner()) {
            return weaponAt(anchorShip, startOffset);
        }
        return chainedFrom(anchor, startOffset);
    }

    private WeaponAPI weaponAt(ShipAPI anchorShip, Vector2f startOffset) {
        WeaponAPI closest = null;
        float closestDistanceSquared = Float.MAX_VALUE;
        for (ScriptedWeapon candidate : scriptedWeaponsByShip.computeIfAbsent(anchorShip, ArcWeapons::scriptedWeapons)) {
            float distanceSquared = CombatQueries.distanceSquared(candidate.mountLocation(), startOffset);
            float reach = candidate.firePointReach() + FIRE_POINT_MARGIN;
            if (distanceSquared <= reach * reach && distanceSquared < closestDistanceSquared) {
                closest = candidate.weapon();
                closestDistanceSquared = distanceSquared;
            }
        }
        return closest;
    }

    private WeaponAPI chainedFrom(CombatEntityAPI anchor, Vector2f startOffset) {
        ArcHit previous = lastArcHitByTarget.get(anchor);
        if (previous == null || now() - previous.time() > CHAIN_MEMORY_SECONDS) {
            return null;
        }
        boolean startsAtCentre = startOffset.lengthSquared() <= CHAIN_START_MARGIN * CHAIN_START_MARGIN;
        boolean startsAtHit = previous.hitOffset() != null && CombatQueries.withinRadius(previous.hitOffset(), startOffset, CHAIN_START_MARGIN);
        return startsAtCentre || startsAtHit ? previous.weapon() : null;
    }

    private static List<ScriptedWeapon> scriptedWeapons(ShipAPI ship) {
        List<ScriptedWeapon> scripted = new ArrayList<>();
        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (weapons == null) {
            return scripted;
        }
        for (WeaponAPI weapon : weapons) {
            if (isScripted(weapon) && weapon.getSlot() != null && !weapon.isDecorative() && !weapon.getSlot().isSystemSlot()) {
                scripted.add(new ScriptedWeapon(weapon, new Vector2f(weapon.getSlot().getLocation()), firePointReach(weapon.getSpec())));
            }
        }
        return scripted;
    }

    static boolean isScripted(WeaponAPI weapon) {
        if (weapon.getEffectPlugin() != null) {
            return true;
        }
        WeaponSpecAPI spec = weapon.getSpec();
        if (spec instanceof BeamWeaponSpecAPI beamSpec) {
            return beamSpec.getBeamEffect() != null;
        }
        if (spec instanceof ProjectileWeaponSpecAPI projectileWeaponSpec) {
            Object projectileSpec = projectileWeaponSpec.getProjectileSpec();
            return projectileSpec instanceof ProjectileSpecAPI shot && shot.getOnFireEffect() != null
                    || projectileSpec instanceof MissileSpecAPI missile && missile.getOnFireEffect() != null;
        }
        return false;
    }

    private static float firePointReach(WeaponSpecAPI spec) {
        if (spec == null) {
            return 0f;
        }
        float reach = longest(spec.getTurretFireOffsets(), 0f);
        reach = longest(spec.getHardpointFireOffsets(), reach);
        return longest(spec.getHiddenFireOffsets(), reach);
    }

    private static float longest(List<Vector2f> offsets, float atLeast) {
        float longest = atLeast;
        if (offsets != null) {
            for (Vector2f offset : offsets) {
                longest = Math.max(longest, offset.length());
            }
        }
        return longest;
    }

    private static Vector2f hitOffset(EmpArcEntityAPI arc, CombatEntityAPI target) {
        Vector2f hitPoint = arc.getTargetLocation();
        return hitPoint == null || target.getLocation() == null ? null : inFrameOf(target, hitPoint);
    }

    static Vector2f inFrameOf(CombatEntityAPI anchor, Vector2f worldPoint) {
        double radians = Math.toRadians(-anchor.getFacing());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float deltaX = worldPoint.x - anchor.getLocation().x;
        float deltaY = worldPoint.y - anchor.getLocation().y;
        return new Vector2f(deltaX * cos - deltaY * sin, deltaX * sin + deltaY * cos);
    }

    private static float now() {
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine == null ? 0f : engine.getTotalElapsedTime(false);
    }
}
