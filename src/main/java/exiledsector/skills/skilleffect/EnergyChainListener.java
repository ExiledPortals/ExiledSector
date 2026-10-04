package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

final class EnergyChainListener implements DamageDealtModifier, DroneSpawner {

    static final String CHANCE_KEY = "exiledSector_energyChainChance";
    static final String FALLOFF_KEY = "exiledSector_energyChainFalloff";

    private final ShipAPI ship;
    private final RefractionDrones drones;

    EnergyChainListener(ShipAPI ship) {
        this(ship, new RefractionDrones(ship));
    }

    EnergyChainListener(ShipAPI ship, RefractionDrones drones) {
        this.ship = ship;
        this.drones = drones;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI projectile) {
            handleHit(projectile, target, damage, point, shieldHit);
        }
        return null;
    }

    void handleHit(DamagingProjectileAPI projectile, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI hitShip) || !canChainFrom(projectile.getWeapon())) {
            return;
        }
        ChainLink link = ChainLink.of(projectile, ship);
        if (link.dealtMult() < 1f) {
            ChainHitDamageRestorer.reduceForThisHit(damage, link.dealtMult(), hitShip);
        }
        if (shieldHit && CombatQueries.isHostile(ship, hitShip)) {
            tryChain(projectile.getWeapon(), hitShip, point, link);
        }
    }

    private static boolean canChainFrom(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && weapon.getType() == WeaponAPI.WeaponType.ENERGY
                && !CsvIdBlocklist.ENERGY_CHAIN_WEAPONS.contains(weapon.getId());
    }

    private void tryChain(WeaponAPI weapon, ShipAPI hitShip, Vector2f point, ChainLink link) {
        float chancePercent = ship.getMutableStats().getDynamic().getValue(CHANCE_KEY, 0f);
        boolean chains = ship.isAlive() && link.count() < MaxChainCountConfig.get() && chancePercent > 0f
                && Math.random() < chancePercent / 100.0;
        if (!chains) {
            return;
        }
        ChainLink next = link.next(hitShip, ship.getMutableStats().getDynamic().getValue(FALLOFF_KEY, 0f));
        if (next.dealtMult() <= 0f || !WeaponDroneFactory.supportsProjectile(weapon)) {
            return;
        }
        ShipAPI nextTarget = findNearestChainTarget(point, weapon.getRange(), next.hitSoFar());
        if (nextTarget != null) {
            drones.launch(this, weapon, hitShip, point, nextTarget, next);
        }
    }

    private ShipAPI findNearestChainTarget(Vector2f point, float range, List<ShipAPI> excluded) {
        ShipAPI nearest = null;
        float nearestDistanceSq = Float.MAX_VALUE;
        for (ShipAPI candidate : CombatQueries.shipsNear(point, range, other -> !excluded.contains(other) && other.isAlive()
                && !other.isHulk() && other.getCollisionClass() != CollisionClass.NONE
                && CombatQueries.isHostile(ship, other) && CombatQueries.withinRadius(other.getLocation(), point, range))) {
            float distanceSq = Vector2f.sub(candidate.getLocation(), point, null).lengthSquared();
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = candidate;
            }
        }
        return nearest;
    }
}
