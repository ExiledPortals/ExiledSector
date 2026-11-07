package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ProximityFuseAIAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.BallisticAsEnergy;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class RiftStrikeListener extends ShipCombatListener implements DamageDealtModifier {

    static final String RIFT_MINE_WEAPON_ID = "riftbeam_minelayer";
    private static final float RIFT_FADE_IN_SECONDS = 0.05f;

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
        if (!(param instanceof DamagingProjectileAPI projectile) || !(target instanceof ShipAPI hitShip) || !firesRifts(projectile.getWeapon())
                || !CombatQueries.isHostile(ownerShip, hitShip) || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (random.nextFloat() < magnitude(RiftStrike.CHANCE_PERCENT_KEY) / 100f) {
            spawnRift(point, RiftStrike.baseRiftDamage(projectile.getWeapon().getSpec()));
        }
        return null;
    }

    private static boolean firesRifts(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && BallisticAsEnergy.countsAsEnergy(weapon);
    }

    private void spawnRift(Vector2f point, float riftDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || point == null) {
            return;
        }
        if (!(engine.spawnProjectile(ownerShip, null, RIFT_MINE_WEAPON_ID, new Vector2f(point), random.nextFloat() * 360f, null) instanceof MissileAPI rift)) {
            return;
        }
        rift.getDamage().setDamage(riftDamage);
        if (rift.getMissileAI() instanceof ProximityFuseAIAPI fuse) {
            fuse.updateDamage();
        }
        rift.getVelocity().scale(0f);
        rift.fadeOutThenIn(RIFT_FADE_IN_SECONDS);
        rift.setFlightTime(rift.getMaxFlightTime());
        rift.addDamagedAlready(ownerShip);
        rift.setNoMineFFConcerns(true);
    }
}
