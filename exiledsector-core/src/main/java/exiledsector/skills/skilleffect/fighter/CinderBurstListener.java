package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class CinderBurstListener implements HullDamageAboutToBeTakenListener {

    static final String DAMAGE_KEY = "exiledSector_strikecraftDeathBurstDamage";
    static final float BURST_RADIUS = 150f;
    private static final Color BURST_COLOR = new Color(255, 120, 40, 140);
    private static final float BURST_VISUAL_SECONDS = 0.6f;

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private boolean burst;

    CinderBurstListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
    }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI damagedShip, Vector2f point, float damageAmount) {
        if (!burst && damageAmount >= damagedShip.getHitpoints()) {
            burst = true;
            burst();
        }
        return false;
    }

    private void burst() {
        float damage = FighterListenerEffect.carrierValue(carrier, DAMAGE_KEY);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (damage <= 0f || engine == null) {
            return;
        }
        Vector2f burstPoint = new Vector2f(fighter.getLocation());
        for (ShipAPI target : CombatQueries.shipsNear(burstPoint, BURST_RADIUS, other -> caughtInBurst(fighter, other, burstPoint))) {
            engine.applyDamage(target, target.getLocation(), damage, DamageType.FRAGMENTATION, 0f, false, false, fighter);
        }
        engine.spawnExplosion(burstPoint, new Vector2f(fighter.getVelocity()), BURST_COLOR, BURST_RADIUS, BURST_VISUAL_SECONDS);
    }

    static boolean caughtInBurst(ShipAPI fighter, ShipAPI other, Vector2f burstPoint) {
        return other != fighter && CombatQueries.isAliveNotHulk(other) && !other.isShuttlePod()
                && other.getCollisionClass() != CollisionClass.NONE && CombatQueries.isHostile(fighter, other)
                && CombatQueries.withinRadius(other.getLocation(), burstPoint, BURST_RADIUS + other.getCollisionRadius());
    }
}
