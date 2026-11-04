package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class FlakBurstListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    static final String DAMAGE_KEY = "exiledSector_flakBurstDamage";
    static final float BURST_RADIUS = 100f;
    private static final Color BURST_COLOR = new Color(255, 200, 120, 160);
    private static final float BURST_VISUAL_SECONDS = 0.4f;

    private final Set<MissileAPI> struckMissiles = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean bursting;

    FlakBurstListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!bursting && param != null && target instanceof MissileAPI missile && isHostileMissile(missile)) {
            struckMissiles.add(missile);
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        if (struckMissiles.isEmpty()) {
            return;
        }
        List<Vector2f> burstPoints = new ArrayList<>();
        Iterator<MissileAPI> struck = struckMissiles.iterator();
        while (struck.hasNext()) {
            MissileAPI missile = struck.next();
            if (missile.getHitpoints() <= 0f) {
                burstPoints.add(new Vector2f(missile.getLocation()));
            }
            struck.remove();
        }
        float damage = magnitude(DAMAGE_KEY);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (damage <= 0f || engine == null) {
            return;
        }
        for (Vector2f burstPoint : burstPoints) {
            burst(engine, burstPoint, damage);
        }
    }

    private void burst(CombatEngineAPI engine, Vector2f burstPoint, float damage) {
        bursting = true;
        for (MissileAPI caught : missilesCaughtInBurst(engine, burstPoint)) {
            engine.applyDamage(caught, caught.getLocation(), damage, DamageType.FRAGMENTATION, 0f, false, false, ownerShip);
        }
        for (ShipAPI caught : CombatQueries.shipsNear(burstPoint, BURST_RADIUS, other -> isFighterCaughtInBurst(other, burstPoint))) {
            engine.applyDamage(caught, caught.getLocation(), damage, DamageType.FRAGMENTATION, 0f, false, false, ownerShip);
        }
        bursting = false;
        engine.spawnExplosion(burstPoint, new Vector2f(), BURST_COLOR, BURST_RADIUS, BURST_VISUAL_SECONDS);
    }

    private List<MissileAPI> missilesCaughtInBurst(CombatEngineAPI engine, Vector2f burstPoint) {
        List<MissileAPI> caught = new ArrayList<>();
        Iterator<Object> nearby = CombatQueries.objectsNear(engine.getMissileGrid(), burstPoint, BURST_RADIUS);
        while (nearby.hasNext()) {
            if (nearby.next() instanceof MissileAPI missile && !caught.contains(missile) && isHostileMissile(missile)
                    && missile.getHitpoints() > 0f
                    && CombatQueries.withinRadius(missile.getLocation(), burstPoint, BURST_RADIUS + missile.getCollisionRadius())) {
                caught.add(missile);
            }
        }
        return caught;
    }

    private boolean isHostileMissile(MissileAPI missile) {
        return missile.getOwner() != ownerShip.getOwner() && missile.getOwner() != Misc.OWNER_NEUTRAL;
    }

    private boolean isFighterCaughtInBurst(ShipAPI other, Vector2f burstPoint) {
        return other.isFighter() && CombatQueries.isAliveNotHulk(other) && other.getCollisionClass() != CollisionClass.NONE
                && CombatQueries.isHostile(ownerShip, other)
                && CombatQueries.withinRadius(other.getLocation(), burstPoint, BURST_RADIUS + other.getCollisionRadius());
    }
}
