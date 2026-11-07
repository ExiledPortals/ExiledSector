package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class RiftStrikeListener extends ShipCombatListener implements DamageDealtModifier {

    private final Random random;

    RiftStrikeListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    RiftStrikeListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; rifts are spawned as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof DamagingProjectileAPI projectile) || !(target instanceof ShipAPI hitShip) || !RiftStrike.firesRifts(projectile.getWeapon())
                || !CombatQueries.isHostile(ownerShip, hitShip) || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (random.nextFloat() < magnitude(RiftStrike.CHANCE_PERCENT_KEY) / 100f) {
            RiftExplosions.spawn(ownerShip, point, RiftStrike.baseRiftDamage(projectile.getWeapon().getSpec()), random);
        }
        return null;
    }
}
