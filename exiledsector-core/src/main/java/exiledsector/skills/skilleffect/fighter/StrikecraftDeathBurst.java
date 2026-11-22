package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

abstract class StrikecraftDeathBurst implements HullDamageAboutToBeTakenListener {

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private final String damageKey;
    private final DamageType damageType;
    private final float burstRadius;
    private boolean burst;

    StrikecraftDeathBurst(ShipAPI fighter, ShipAPI carrier, String damageKey, DamageType damageType, float burstRadius) {
        this.fighter = fighter;
        this.carrier = carrier;
        this.damageKey = damageKey;
        this.damageType = damageType;
        this.burstRadius = burstRadius;
    }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI damagedShip, Vector2f point, float damageAmount) {
        if (!burst && damageAmount >= damagedShip.getHitpoints()) {
            burst = true;
            burst();
        }
        return false;
    }

    abstract void showBurst(CombatEngineAPI engine, ShipAPI fighter, Vector2f burstPoint);

    private void burst() {
        float damage = FighterListenerEffect.carrierValue(carrier, damageKey);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (damage <= 0f || engine == null) {
            return;
        }
        Vector2f burstPoint = new Vector2f(fighter.getLocation());
        for (ShipAPI target : CombatQueries.shipsNear(burstPoint, burstRadius, other -> caughtInBurst(fighter, other, burstPoint, burstRadius))) {
            engine.applyDamage(target, target.getLocation(), damage, damageType, 0f, false, false, fighter);
        }
        showBurst(engine, fighter, burstPoint);
    }

    static boolean caughtInBurst(ShipAPI fighter, ShipAPI other, Vector2f burstPoint, float burstRadius) {
        return other != fighter && CombatQueries.isAliveNotHulk(other) && !other.isShuttlePod()
                && other.getCollisionClass() != CollisionClass.NONE && CombatQueries.isHostile(fighter, other)
                && CombatQueries.withinRadius(other.getLocation(), burstPoint, burstRadius + other.getCollisionRadius());
    }
}
