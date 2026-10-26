package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.CsvIdList;
import exiledlib.skills.skilleffect.HitDamageRestorer;
import exiledlib.skills.skilleffect.ShipCombatListener;
import exiledlib.skills.skilleffect.weapon.BallisticAsEnergy;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

final class EnergyChainListener extends ShipCombatListener implements DamageDealtModifier, DroneSpawner {

    static final String CHANCE_KEY = "exiledLib_energyChainChance";
    static final String FALLOFF_KEY = "exiledLib_energyChainFalloff";

    private final RefractionDrones drones;

    EnergyChainListener(ShipAPI ownerShip) {
        this(ownerShip, new RefractionDrones(ownerShip));
    }

    EnergyChainListener(ShipAPI ownerShip, RefractionDrones drones) {
        super(ownerShip);
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
        ChainLink link = ChainLink.of(projectile, ownerShip);
        if (link.dealtMult() < 1f) {
            HitDamageRestorer.reduceForThisHit(damage, link.dealtMult(), hitShip);
        }
        if (shieldHit && CombatQueries.isHostile(ownerShip, hitShip)) {
            tryChain(projectile.getWeapon(), hitShip, point, link);
        }
    }

    private static boolean canChainFrom(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && BallisticAsEnergy.countsAsEnergy(weapon)
                && !CsvIdList.ENERGY_CHAIN_WEAPONS.contains(weapon.getId());
    }

    private void tryChain(WeaponAPI weapon, ShipAPI hitShip, Vector2f point, ChainLink link) {
        float chancePercent = magnitude(CHANCE_KEY);
        boolean chains = ownerShip.isAlive() && link.chainCount() < MaxChainCountConfig.get() && chancePercent > 0f
                && Math.random() < chancePercent / 100.0;
        if (!chains) {
            return;
        }
        ChainLink next = link.next(hitShip, magnitude(FALLOFF_KEY));
        if (next.dealtMult() <= 0f || !WeaponDroneFactory.supportsProjectile(weapon)) {
            return;
        }
        ShipAPI nextTarget = findNearestChainTarget(point, weapon.getRange(), next.hitSoFar());
        if (nextTarget != null) {
            drones.launch(this, weapon, hitShip, point, nextTarget, next);
        }
    }

    private ShipAPI findNearestChainTarget(Vector2f point, float range, List<ShipAPI> excluded) {
        List<ShipAPI> candidates = CombatQueries.hostilesNear(ownerShip, point, range, other -> !excluded.contains(other));
        return candidates.isEmpty() ? null : CombatQueries.nearestFirst(candidates, point, 1).get(0);
    }
}
