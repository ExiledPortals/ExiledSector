package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class ProjectileEchoListener extends ShipCombatListener implements DamageDealtModifier {

    static final String CHANCE_KEY = "exiledLib_projectileEchoChance";
    static final String ECHO_KEY = "exiledLib_projectileEcho";
    private static final Color ECHO_FLASH_COLOR = new Color(200, 120, 255, 255);
    private static final float ECHO_FLASH_SIZE = 30f;
    private static final float ECHO_FLASH_SECONDS = 0.3f;

    ProjectileEchoListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI projectile && target instanceof ShipAPI hitShip) {
            handleHit(projectile, hitShip);
        }
        return null;
    }

    void handleHit(DamagingProjectileAPI projectile, ShipAPI hitShip) {
        WeaponAPI weapon = projectile.getWeapon();
        if (!canEcho(projectile, weapon) || projectile.getSource() != ownerShip || !ownerShip.isAlive()
                || !hitShip.isAlive() || !CombatQueries.isHostile(ownerShip, hitShip)) {
            return;
        }
        float chancePercent = magnitude(CHANCE_KEY);
        if (chancePercent <= 0f || Math.random() >= chancePercent / 100.0) {
            return;
        }
        fireEcho(weapon, hitShip);
    }

    static boolean canEcho(DamagingProjectileAPI projectile, WeaponAPI weapon) {
        return !(projectile instanceof MissileAPI) && weapon != null && !weapon.isBeam() && weapon.getType() != WeaponType.MISSILE
                && !isEcho(projectile);
    }

    static boolean isEcho(DamagingProjectileAPI projectile) {
        return projectile.getCustomData().containsKey(ECHO_KEY);
    }

    private void fireEcho(WeaponAPI weapon, ShipAPI hitShip) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f firePoint = weapon.getLocation();
        float facing = VectorUtils.getAngle(firePoint, hitShip.getLocation());
        CombatEntityAPI echo = engine.spawnProjectile(ownerShip, weapon, weapon.getId(), new Vector2f(firePoint), facing, ownerShip.getVelocity());
        if (echo != null) {
            echo.setCustomData(ECHO_KEY, Boolean.TRUE);
            engine.addHitParticle(firePoint, ownerShip.getVelocity(), ECHO_FLASH_SIZE, 1f, ECHO_FLASH_SECONDS, ECHO_FLASH_COLOR);
        }
    }
}
