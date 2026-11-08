package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityFuseAIAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.CsvIdList;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.concurrent.ThreadLocalRandom;

final class BallisticPierceListener extends ShipCombatListener implements DamageDealtModifier, DroneSpawner {

    static final String CHANCE_KEY = "exiledSector_ballisticPierceChance";
    static final String ADDITIONAL_PIERCES_KEY = "exiledSector_ballisticAdditionalPierces";
    static final String PIERCED_KEY = "exiledSector_ballisticPierced";

    private final PierceDrones drones;

    BallisticPierceListener(ShipAPI ownerShip) {
        this(ownerShip, new PierceDrones(ownerShip));
    }

    BallisticPierceListener(ShipAPI ownerShip, PierceDrones drones) {
        super(ownerShip);
        this.drones = drones;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!shieldHit && param instanceof DamagingProjectileAPI projectile && target instanceof ShipAPI hitShip) {
            handleHit(projectile, hitShip, point);
        }
        return null;
    }

    void handleHit(DamagingProjectileAPI projectile, ShipAPI hitShip, Vector2f point) {
        WeaponAPI weapon = projectile.getWeapon();
        int earlierPierces = piercesOf(projectile);
        if (!canPierce(weapon) || earlierPierces > Math.round(magnitude(ADDITIONAL_PIERCES_KEY))
                || projectile.getAI() instanceof ProximityFuseAIAPI
                || hitShip.isFighter() || !hitShip.isAlive()
                || !ownerShip.isAlive() || !CombatQueries.isHostile(ownerShip, hitShip)) {
            return;
        }
        if (!Chance.roll(ThreadLocalRandom.current(), magnitude(CHANCE_KEY)) || !WeaponDroneFactory.supportsProjectile(weapon)) {
            return;
        }
        drones.launch(weapon, hitShip, point, travelFacing(projectile), earlierPierces + 1);
    }

    static float travelFacing(DamagingProjectileAPI projectile) {
        Vector2f velocity = projectile.getVelocity();
        if (projectile.getSpawnType() == ProjectileSpawnType.BALLISTIC && velocity != null && velocity.lengthSquared() > 0f) {
            return VectorUtils.getFacing(velocity);
        }
        return projectile.getFacing();
    }

    static boolean canPierce(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && WeaponScope.BALLISTIC.matches(weapon)
                && !CsvIdList.BALLISTIC_PIERCE_WEAPONS.contains(weapon.getId());
    }

    static boolean isPierced(DamagingProjectileAPI projectile) {
        return projectile.getCustomData().containsKey(PIERCED_KEY);
    }

    static int piercesOf(DamagingProjectileAPI projectile) {
        return projectile.getCustomData().get(PIERCED_KEY) instanceof Integer pierces ? pierces : 0;
    }

    static void markPierced(DamagingProjectileAPI projectile, int pierces) {
        projectile.setCustomData(PIERCED_KEY, pierces);
    }
}
