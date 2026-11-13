package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class ShieldPhaseThroughListener extends ShipCombatListener implements DamageTakenModifier {

    static final String ENERGY_CHANCE_PERCENT_KEY = "exiledSector_energyShieldPhaseThroughChancePercent";
    static final String BALLISTIC_CHANCE_PERCENT_KEY = "exiledSector_ballisticShieldPhaseThroughChancePercent";
    static final String PHASE_THROUGH_MOD_ID = "exiledSector_shieldPhaseThrough";
    static final String PHASED_THROUGH_KEY = "exiledSector_shieldPhasedThrough";

    private final Random random;

    ShieldPhaseThroughListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    ShieldPhaseThroughListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    float chancePercent(Object param) {
        if (!(param instanceof DamagingProjectileAPI projectile) || param instanceof MissileAPI
                || projectile.getCustomData().containsKey(PHASED_THROUGH_KEY)) {
            return 0f;
        }
        WeaponAPI weapon = projectile.getWeapon();
        if (weapon == null || weapon.isBeam()) {
            return 0f;
        }
        if (weapon.getType() == WeaponType.ENERGY) {
            return magnitude(ENERGY_CHANCE_PERCENT_KEY);
        }
        return weapon.getType() == WeaponType.BALLISTIC ? magnitude(BALLISTIC_CHANCE_PERCENT_KEY) : 0f;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!shieldHit || target != ownerShip || !Chance.roll(random, chancePercent(param))) {
            return null;
        }
        damage.getModifier().modifyMult(PHASE_THROUGH_MOD_ID, 0f);
        showPassage((DamagingProjectileAPI) param, point);
        return PHASE_THROUGH_MOD_ID;
    }

    private static void showPassage(DamagingProjectileAPI projectile, Vector2f point) {
        CombatEngineAPI engine = Global.getCombatEngine();
        WeaponAPI weapon = projectile.getWeapon();
        if (engine == null || point == null || weapon == null || weapon.getId() == null) {
            return;
        }
        CombatEntityAPI passage = engine.spawnProjectile(projectile.getSource(), weapon, weapon.getId(), new Vector2f(point),
                projectile.getFacing(), null);
        if (passage instanceof DamagingProjectileAPI passing) {
            passing.setCustomData(PHASED_THROUGH_KEY, true);
            passing.setDamageAmount(0f);
            passing.setCollisionClass(CollisionClass.NONE);
        }
    }
}
