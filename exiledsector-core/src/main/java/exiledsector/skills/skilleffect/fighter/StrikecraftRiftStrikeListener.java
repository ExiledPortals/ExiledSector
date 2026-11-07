package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.onhit.RiftStrike;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class StrikecraftRiftStrikeListener implements DamageDealtModifier {

    static final String CHANCE_PERCENT_KEY = "exiledSector_strikecraftRiftStrikeChancePercent";

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private final Random random;

    StrikecraftRiftStrikeListener(ShipAPI fighter, ShipAPI carrier) {
        this(fighter, carrier, new Random());
    }

    StrikecraftRiftStrikeListener(ShipAPI fighter, ShipAPI carrier, Random random) {
        this.fighter = fighter;
        this.carrier = carrier;
        this.random = random;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; rifts are spawned as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof DamagingProjectileAPI projectile) || !(target instanceof ShipAPI hitShip) || !RiftStrike.firesRifts(projectile.getWeapon())
                || !CombatQueries.isHostile(fighter, hitShip) || !CombatQueries.isAliveNotHulk(fighter)) {
            return null;
        }
        if (random.nextFloat() < FighterListenerEffect.carrierValue(carrier, CHANCE_PERCENT_KEY) / 100f) {
            RiftExplosions.spawn(fighter, point, RiftStrike.baseRiftDamage(projectile.getWeapon().getSpec()), random);
        }
        return null;
    }
}
