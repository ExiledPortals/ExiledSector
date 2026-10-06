package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityFuseAIAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

final class BallisticPierceListener implements DamageDealtModifier, DroneSpawner {

    static final String CHANCE_KEY = "exiledSector_ballisticPierceChance";
    static final String PIERCED_KEY = "exiledSector_ballisticPierced";

    private final ShipAPI ship;
    private final PierceDrones drones;

    BallisticPierceListener(ShipAPI ship) {
        this(ship, new PierceDrones(ship));
    }

    BallisticPierceListener(ShipAPI ship, PierceDrones drones) {
        this.ship = ship;
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
        if (!canPierce(weapon) || isPierced(projectile) || projectile.getAI() instanceof ProximityFuseAIAPI
                || hitShip.isFighter() || !hitShip.isAlive()
                || !ship.isAlive() || !CombatQueries.isHostile(ship, hitShip)) {
            return;
        }
        float chancePercent = ship.getMutableStats().getDynamic().getValue(CHANCE_KEY, 0f);
        if (chancePercent <= 0f || Math.random() >= chancePercent / 100.0 || !WeaponDroneFactory.supportsProjectile(weapon)) {
            return;
        }
        drones.launch(weapon, hitShip, point, travelFacing(projectile));
    }

    static float travelFacing(DamagingProjectileAPI projectile) {
        Vector2f velocity = projectile.getVelocity();
        if (projectile.getSpawnType() == ProjectileSpawnType.BALLISTIC && velocity != null && velocity.lengthSquared() > 0f) {
            return VectorUtils.getFacing(velocity);
        }
        return projectile.getFacing();
    }

    static boolean canPierce(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && weapon.getType() == WeaponAPI.WeaponType.BALLISTIC
                && !CsvIdList.BALLISTIC_PIERCE_WEAPONS.contains(weapon.getId());
    }

    static boolean isPierced(DamagingProjectileAPI projectile) {
        return projectile.getCustomData().containsKey(PIERCED_KEY);
    }

    static void markPierced(DamagingProjectileAPI projectile) {
        projectile.setCustomData(PIERCED_KEY, Boolean.TRUE);
    }
}
