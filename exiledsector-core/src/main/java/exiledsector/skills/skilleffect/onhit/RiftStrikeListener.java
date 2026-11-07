package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public final class RiftStrikeListener extends ShipCombatListener implements DamageDealtModifier {

    private final String chancePercentKey;
    private final Random random;

    RiftStrikeListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String chancePercentKey, Random random) {
        super(ownerShip, magnitudeHolder, null);
        this.chancePercentKey = chancePercentKey;
        this.random = random;
    }

    static RiftStrikeListener forShip(ShipAPI ownerShip) {
        return new RiftStrikeListener(ownerShip, ownerShip, RiftStrike.CHANCE_PERCENT_KEY, new Random());
    }

    public static RiftStrikeListener forStrikecraft(ShipAPI fighter, ShipAPI carrier) {
        return new RiftStrikeListener(fighter, carrier, RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, new Random());
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; rifts are spawned as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        WeaponAPI weapon = param instanceof DamagingProjectileAPI projectile ? projectile.getWeapon() : null;
        if (weapon == null || weapon.getShip() != ownerShip || !RiftStrike.firesRifts(weapon)
                || CombatQueries.hostileLiveShip(ownerShip, target) == null || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (Chance.roll(random, magnitude(chancePercentKey))) {
            RiftExplosions.spawn(ownerShip, point, RiftStrike.baseRiftDamage(weapon.getSpec()), random);
        }
        return null;
    }
}
