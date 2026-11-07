package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.DamagePercentListener;

public final class RiftExplosionDamageListener extends DamagePercentListener {

    static final String DAMAGE_PERCENT_KEY = RiftExplosions.DAMAGE_PERCENT_KEY;
    public static final String STRIKECRAFT_DAMAGE_PERCENT_KEY = "exiledSector_strikecraftRiftExplosionDamagePercent";
    private static final String MOD_ID_PREFIX = "exiledSector_riftExplosionDamage_";

    private final String damagePercentKey;

    private RiftExplosionDamageListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String damagePercentKey) {
        super(ownerShip, magnitudeHolder, MOD_ID_PREFIX);
        this.damagePercentKey = damagePercentKey;
    }

    static RiftExplosionDamageListener forShip(ShipAPI ownerShip) {
        return new RiftExplosionDamageListener(ownerShip, ownerShip, DAMAGE_PERCENT_KEY);
    }

    public static RiftExplosionDamageListener forStrikecraft(ShipAPI fighter, ShipAPI carrier) {
        return new RiftExplosionDamageListener(fighter, carrier, STRIKECRAFT_DAMAGE_PERCENT_KEY);
    }

    @Override
    protected float damagePercent(Object param, CombatEntityAPI target) {
        return param instanceof DamagingProjectileAPI projectile && RiftExplosions.isRift(projectile) ? magnitude(damagePercentKey) : 0f;
    }
}
