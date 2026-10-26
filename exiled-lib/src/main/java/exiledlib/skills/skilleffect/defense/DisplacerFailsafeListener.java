package exiledlib.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class DisplacerFailsafeListener extends ShipCombatListener implements DamageListener {

    static final String HULL_PERCENT_KEY = "exiledLib_displacerFailsafeHullPercent";
    static final float DISPLACE_DISTANCE = 1200f;
    private static final float ENEMY_SEARCH_RADIUS = 6000f;
    private static final float MAP_EDGE_MARGIN = 300f;
    private static final String SOUND = "system_phase_teleporter";
    private static final Color FLASH_COLOR = new Color(120, 200, 255, 255);
    private static final float FLASH_DURATION_SECONDS = 0.6f;

    private boolean used;

    DisplacerFailsafeListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    boolean used() {
        return used;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (used || target != ownerShip || !ownerIsAliveNotHulk() || ownerShip.getHullLevel() * 100f >= magnitude(HULL_PERCENT_KEY)) {
            return;
        }
        used = true;
        ShipAPI enemy = CombatQueries.nearestShip(ownerShip.getLocation(), ENEMY_SEARCH_RADIUS,
                candidate -> CombatQueries.isHostile(ownerShip, candidate) && CombatQueries.isAliveNotHulk(candidate) && !candidate.isFighter());
        if (enemy != null) {
            displaceAwayFrom(enemy);
        }
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        if (flux.getCurrFlux() > 0f && !flux.isOverloadedOrVenting()) {
            flux.ventFlux();
        }
    }

    private void displaceAwayFrom(ShipAPI enemy) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f from = new Vector2f(ownerShip.getLocation());
        Vector2f to = destination(from, enemy.getLocation(), engine.getMapWidth(), engine.getMapHeight());
        flash(engine, from);
        ownerShip.getLocation().set(to);
        ownerShip.getVelocity().set(0f, 0f);
        flash(engine, to);
        Global.getSoundPlayer().playSound(SOUND, 1f, 1f, to, ownerShip.getVelocity());
    }

    static Vector2f destination(Vector2f from, Vector2f enemyLocation, float mapWidth, float mapHeight) {
        float awayAngle = from.equals(enemyLocation) ? 0f : VectorUtils.getAngle(enemyLocation, from);
        Vector2f to = MathUtils.getPointOnCircumference(from, DISPLACE_DISTANCE, awayAngle);
        float halfWidth = Math.max(0f, mapWidth / 2f - MAP_EDGE_MARGIN);
        float halfHeight = Math.max(0f, mapHeight / 2f - MAP_EDGE_MARGIN);
        to.set(Math.max(-halfWidth, Math.min(halfWidth, to.x)), Math.max(-halfHeight, Math.min(halfHeight, to.y)));
        return to;
    }

    private void flash(CombatEngineAPI engine, Vector2f point) {
        engine.addHitParticle(point, new Vector2f(), ownerShip.getCollisionRadius() * 3f, 1f, FLASH_DURATION_SECONDS, FLASH_COLOR);
    }
}
