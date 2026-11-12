package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.scar.ScarEvidence.FiredAs;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.defense.CollisionDamageListener;
import exiledsector.skills.skilleffect.scar.MineHits;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;
import java.util.Set;

final class ScarDamageTally implements DamageTakenModifier, DamageDealtModifier {

    static final float REAR_ANGLE = 120f;
    static final float LARGE_HIT_DAMAGE = 1000f;

    private final ScarFlagshipTracker tracker;

    ScarDamageTally(ScarFlagshipTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI flagship = tracker.flagship();
        if (target != flagship || flagship == null || tracker.finished() || damage == null) {
            return null;
        }
        float amount = CombatQueries.hitDamage(damage);
        if (amount <= 0f) {
            return null;
        }
        ScarEvidence evidence = tracker.evidence();
        if (CollisionDamageListener.isCollisionDamage(param, damage) && CollisionDamageListener.touchesAnything(flagship)) {
            evidence.addFired(amount, EnumSet.of(FiredAs.COLLISION));
            return null;
        }
        ShipAPI attacker = CombatQueries.attackingShip(null, param);
        if (attacker != null && attacker.getOwner() == flagship.getOwner()) {
            return null;
        }
        evidence.addFired(amount, categories(param, attacker, flagship, damage, amount, point));
        evidence.addFired(FiredAs.EMP, damage.computeFluxDealt(damage.getDpsDuration()));
        tracker.recordHit(CombatQueries.owningShip(attacker));
        return null;
    }

    private Set<FiredAs> categories(Object param, ShipAPI attacker, ShipAPI flagship, DamageAPI damage, float amount, Vector2f point) {
        Set<FiredAs> categories = EnumSet.noneOf(FiredAs.class);
        FiredAs damageType = damageType(damage);
        if (damageType != null) {
            categories.add(damageType);
        }
        FiredAs delivery = delivery(param, attacker);
        if (delivery != null) {
            categories.add(delivery);
        }
        if (isBomber(attacker)) {
            categories.add(FiredAs.BOMBER);
        }
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        if (weapon != null && weapon.hasAIHint(WeaponAPI.AIHints.STRIKE) || !damage.isDps() && amount >= LARGE_HIT_DAMAGE) {
            categories.add(FiredAs.STRIKE);
        }
        if (point != null && Misc.getAngleDiff(flagship.getFacing(), Misc.getAngleInDegrees(flagship.getLocation(), point)) > REAR_ANGLE) {
            categories.add(FiredAs.REAR);
        }
        if (attacker != null) {
            addAttackerCategories(categories, attacker, flagship);
        }
        return categories;
    }

    private void addAttackerCategories(Set<FiredAs> categories, ShipAPI attacker, ShipAPI flagship) {
        float range = tracker.evidence().fleet().flagshipLongestRange() + flagship.getCollisionRadius();
        if (!CombatQueries.withinRadius(attacker.getLocation(), flagship.getLocation(), range)) {
            categories.add(FiredAs.BEYOND_RANGE);
        }
        ShipAPI owningShip = CombatQueries.owningShip(attacker);
        if (owningShip == null) {
            return;
        }
        if (owningShip.getHullSpec() != null && owningShip.getHullSpec().isPhase()) {
            categories.add(FiredAs.PHASE_ATTACKER);
        }
        if (owningShip.getSystem() != null && owningShip.getSystem().isActive()) {
            categories.add(FiredAs.SYSTEM_ACTIVE);
        }
        if (owningShip.getMaxSpeedWithoutBoost() > flagship.getMaxSpeedWithoutBoost()) {
            categories.add(FiredAs.FASTER_ATTACKER);
        }
    }

    static FiredAs damageType(DamageAPI damage) {
        if (damage.getType() == null) {
            return null;
        }
        return switch (damage.getType()) {
            case KINETIC -> FiredAs.KINETIC;
            case HIGH_EXPLOSIVE -> FiredAs.HIGH_EXPLOSIVE;
            case ENERGY -> FiredAs.ENERGY;
            case FRAGMENTATION -> FiredAs.FRAGMENTATION;
            default -> null;
        };
    }

    static FiredAs delivery(Object param, ShipAPI attacker) {
        if (attacker != null && attacker.isFighter()) {
            return FiredAs.FIGHTER;
        }
        if (MineHits.isMineHit(param)) {
            return FiredAs.MINE;
        }
        if (param instanceof MissileAPI) {
            return FiredAs.MISSILE;
        }
        if (param instanceof BeamAPI) {
            return FiredAs.BEAM;
        }
        return param instanceof DamagingProjectileAPI ? FiredAs.PROJECTILE : null;
    }

    static boolean isBomber(ShipAPI attacker) {
        if (attacker == null || !attacker.isFighter()) {
            return false;
        }
        FighterWingAPI wing = attacker.getWing();
        return wing != null && wing.getRole() == WingRole.BOMBER;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI flagship = tracker.flagship();
        if (flagship == null || tracker.finished() || damage == null || !(target instanceof ShipAPI targetShip)) {
            return null;
        }
        if (targetShip.getOwner() != flagship.getOwner() && !targetShip.isFighter()) {
            tracker.evidence().addDamageDealt(CombatQueries.hitDamage(damage));
        }
        return null;
    }
}
