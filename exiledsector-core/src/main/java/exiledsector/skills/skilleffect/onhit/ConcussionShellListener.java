package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.DoubleSupplier;

final class ConcussionShellListener extends ShipCombatListener implements DamageDealtModifier {

    static final String CHANCE_PERCENT_KEY = "exiledSector_concussionShellChancePercent";

    private final DoubleSupplier roll;

    ConcussionShellListener(ShipAPI ownerShip) {
        this(ownerShip, Math::random);
    }

    ConcussionShellListener(ShipAPI ownerShip, DoubleSupplier roll) {
        super(ownerShip);
        this.roll = roll;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI hitShip = CombatQueries.hostileLiveShip(ownerShip, target);
        if (!shieldHit && param instanceof DamagingProjectileAPI projectile && hitShip != null
                && isLargeBallistic(projectile.getWeapon()) && !hitShip.isFighter()) {
            float chancePercent = magnitude(CHANCE_PERCENT_KEY);
            if (chancePercent > 0f && roll.getAsDouble() < chancePercent / 100.0) {
                disableNearest(hitShip, point);
            }
        }
        return null;
    }

    static boolean isLargeBallistic(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && weapon.getSize() == WeaponSize.LARGE && WeaponScope.BALLISTIC.matches(weapon);
    }

    static void disableNearest(ShipAPI hitShip, Vector2f point) {
        WeaponAPI nearestWeapon = null;
        float nearestWeaponDistanceSquared = Float.MAX_VALUE;
        for (WeaponAPI weapon : hitShip.getAllWeapons()) {
            if (weapon.isDecorative() || weapon.isDisabled() || weapon.isPermanentlyDisabled()) {
                continue;
            }
            float distanceSquared = CombatQueries.distanceSquared(weapon.getLocation(), point);
            if (distanceSquared < nearestWeaponDistanceSquared) {
                nearestWeapon = weapon;
                nearestWeaponDistanceSquared = distanceSquared;
            }
        }
        ShipEngineAPI nearestEngine = null;
        float nearestEngineDistanceSquared = Float.MAX_VALUE;
        ShipEngineControllerAPI engineController = hitShip.getEngineController();
        if (engineController != null) {
            for (ShipEngineAPI engine : engineController.getShipEngines()) {
                if (engine.isSystemActivated() || engine.isDisabled() || engine.isPermanentlyDisabled()) {
                    continue;
                }
                float distanceSquared = CombatQueries.distanceSquared(engine.getLocation(), point);
                if (distanceSquared < nearestEngineDistanceSquared) {
                    nearestEngine = engine;
                    nearestEngineDistanceSquared = distanceSquared;
                }
            }
        }
        if (nearestEngine != null && nearestEngineDistanceSquared < nearestWeaponDistanceSquared) {
            nearestEngine.disable();
        } else if (nearestWeapon != null) {
            nearestWeapon.disable();
        }
    }
}
