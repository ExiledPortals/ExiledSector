package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class ShieldDeflectionListener extends ShipCombatListener implements DamageTakenModifier {

    static final String CHANCE_PERCENT_KEY = "exiledSector_shieldDeflectChancePercent";
    static final String DEFLECT_MOD_ID = "exiledSector_shieldDeflect";

    private final Random random;

    ShieldDeflectionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    ShieldDeflectionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    static boolean isDeflectable(Object param) {
        if (!(param instanceof DamagingProjectileAPI projectile)) {
            return false;
        }
        ProjectileSpawnType spawnType = projectile.getSpawnType();
        return spawnType == ProjectileSpawnType.BALLISTIC || spawnType == ProjectileSpawnType.BALLISTIC_AS_BEAM
                || spawnType == ProjectileSpawnType.PLASMA;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!shieldHit || target != ownerShip || !isDeflectable(param)) {
            return null;
        }
        float chancePercent = magnitude(CHANCE_PERCENT_KEY);
        if (chancePercent <= 0f || random.nextFloat() * 100f >= chancePercent) {
            return null;
        }
        damage.getModifier().modifyMult(DEFLECT_MOD_ID, 0f);
        return DEFLECT_MOD_ID;
    }
}
