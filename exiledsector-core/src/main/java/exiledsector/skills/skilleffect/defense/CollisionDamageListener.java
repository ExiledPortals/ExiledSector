package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

public final class CollisionDamageListener extends ShipCombatListener implements DamageDealtModifier, DamageTakenModifier {

    static final String COLLISION_DAMAGE_PERCENT_KEY = "exiledSector_ramProwCollisionDamagePercent";
    private static final String MOD_ID_PREFIX = "exiledSector_ramProwCollisionDamage_";
    private static final float CONTACT_RADIUS_MARGIN = 1.1f;

    CollisionDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    public static boolean isCollisionDamage(Object param, DamageAPI damage) {
        DamageType type = damage.getType();
        return param == null && damage.getStats() == null && !damage.isDps() && damage.getFluxComponent() <= 0f
                && (type == DamageType.KINETIC || type == DamageType.ENERGY);
    }

    static boolean isInContact(CombatEntityAPI first, CombatEntityAPI second) {
        float contactRadius = (first.getCollisionRadius() + second.getCollisionRadius()) * CONTACT_RADIUS_MARGIN;
        return CombatQueries.withinRadius(first.getLocation(), second.getLocation(), contactRadius);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI targetShip = CombatQueries.hostileShip(ownerShip, target);
        if (targetShip == null || !isCollisionDamage(param, damage) || !isInContact(ownerShip, targetShip)) {
            return null;
        }
        float percent = magnitude(COLLISION_DAMAGE_PERCENT_KEY);
        if (percent <= 0f) {
            return null;
        }
        damage.getModifier().modifyMult(modId, SkillEffectSupport.multFrom(percent));
        return modId;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target != ownerShip || !isCollisionDamage(param, damage)) {
            return null;
        }
        float percent = magnitude(COLLISION_DAMAGE_PERCENT_KEY);
        if (percent <= 0f || !touchesAnything(ownerShip)) {
            return null;
        }
        damage.getModifier().modifyMult(modId, SkillEffectSupport.reductionMult(percent));
        return modId;
    }

    public static boolean touchesAnything(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f location = ship.getLocation();
        float queryRadius = ship.getCollisionRadius() * CONTACT_RADIUS_MARGIN;
        return CombatQueries.anyNear(engine.getShipGrid(), location, queryRadius, candidate -> isCollider(ship, candidate))
                || CombatQueries.anyNear(engine.getAsteroidGrid(), location, queryRadius, candidate -> isCollider(ship, candidate));
    }

    private static boolean isCollider(ShipAPI ship, Object candidate) {
        return candidate instanceof CombatEntityAPI entity && entity != ship && entity.getCollisionClass() != CollisionClass.NONE
                && isInContact(ship, entity);
    }
}
