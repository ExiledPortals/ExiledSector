package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.WingRole;
import exiledsector.compat.CombatGlow;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

final class BomberPayloadDetonation implements AdvanceableListener {

    static final float CHECK_INTERVAL_SECONDS = 3f;
    static final float SAFE_DISTANCE_FROM_CARRIER = 600f;
    static final float MIN_BLAST_RADIUS = 75f;
    private static final float BLAST_SECONDS = 0.15f;
    private static final float BLAST_CORE_SHARE = 0.5f;
    private static final float BLAST_EDGE_DAMAGE_SHARE = 0.5f;
    private static final Color DEFAULT_BLAST_COLOR = new Color(255, 160, 80);
    private static final float BLAST_VISUAL_SIZE_SHARE = 2f;
    private static final float BLAST_VISUAL_SECONDS = 0.8f;
    private static final float BLAST_LIGHT_SIZE_SHARE = 3f;
    private static final float BLAST_LIGHT_INTENSITY = 1f;
    private static final float BLAST_LIGHT_SECONDS = 0.8f;
    private static final float SELF_DESTRUCT_DAMAGE_MULT = 10f;

    private final ShipAPI bomber;
    private final ShipAPI carrier;
    private final float chancePercent;
    private final Random random;
    private float secondsSinceCheck;
    private boolean detonated;

    BomberPayloadDetonation(ShipAPI bomber, ShipAPI carrier, float chancePercent, Random random) {
        this.bomber = bomber;
        this.carrier = carrier;
        this.chancePercent = chancePercent;
        this.random = random;
    }

    static void arm(ShipAPI bomber, ShipAPI carrier, float chancePercent) {
        if (chancePercent <= 0f || !isBomber(bomber) || bomber.hasListenerOfClass(BomberPayloadDetonation.class)) {
            return;
        }
        bomber.addListener(new BomberPayloadDetonation(bomber, carrier, chancePercent, new Random()));
    }

    static boolean isBomber(ShipAPI fighter) {
        FighterWingAPI wing = fighter.getWing();
        return wing != null && wing.getRole() == WingRole.BOMBER;
    }

    @Override
    public void advance(float amount) {
        if (detonated) {
            return;
        }
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceCheck -= CHECK_INTERVAL_SECONDS;
        WeaponAPI payload = payload(bomber);
        if (payload == null || !mayDetonate() || random.nextFloat() * 100f >= chancePercent) {
            return;
        }
        detonated = true;
        detonate(payload);
    }

    boolean mayDetonate() {
        if (bomber.isHulk() || bomber.getHitpoints() <= 0f || bomber.isLanding() || bomber.isFinishedLanding() || bomber.isLiftingOff()) {
            return false;
        }
        if (carrier == null || carrier.isHulk() || carrier.getHitpoints() <= 0f) {
            return true;
        }
        float safeDistance = carrier.getCollisionRadius() + SAFE_DISTANCE_FROM_CARRIER;
        return CombatQueries.distanceSquared(bomber.getLocation(), carrier.getLocation()) > safeDistance * safeDistance;
    }

    static WeaponAPI payload(ShipAPI bomber) {
        WeaponAPI loadedOther = null;
        for (WeaponAPI weapon : bomber.getAllWeapons()) {
            if (weapon.isDecorative() || weapon.isBeam() || weapon.isDisabled() || weapon.isPermanentlyDisabled()
                    || !weapon.usesAmmo() || weapon.getAmmo() <= 0) {
                continue;
            }
            if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                return weapon;
            }
            loadedOther = loadedOther == null ? weapon : loadedOther;
        }
        return loadedOther;
    }

    private void detonate(WeaponAPI payload) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        Vector2f at = new Vector2f(bomber.getLocation());
        CombatEntityAPI shot = engine.spawnProjectile(bomber, payload, payload.getId(), at, bomber.getFacing(), new Vector2f(bomber.getVelocity()));
        Color blastColor = DEFAULT_BLAST_COLOR;
        float blastRadius = MIN_BLAST_RADIUS;
        if (shot instanceof MissileAPI missile && missile.getSpec() != null) {
            blastColor = missile.getSpec().getExplosionColor() != null ? missile.getSpec().getExplosionColor() : blastColor;
            blastRadius = Math.max(blastRadius, missile.getSpec().getExplosionRadius());
        }
        if (shot instanceof MissileAPI missile && missile.getSpec() != null && missile.getSpec().getExplosionSpec() != null) {
            missile.explode();
        } else if (shot instanceof DamagingProjectileAPI projectile) {
            engine.spawnDamagingExplosion(blast(projectile.getDamageAmount(), projectile.getDamageType(), blastRadius, blastColor), bomber, at);
        }
        if (shot != null) {
            engine.removeEntity(shot);
        }
        engine.spawnExplosion(at, new Vector2f(), blastColor, blastRadius * BLAST_VISUAL_SIZE_SHARE, BLAST_VISUAL_SECONDS);
        CombatGlow.flash(at, null, blastColor, blastRadius * BLAST_LIGHT_SIZE_SHARE, BLAST_LIGHT_INTENSITY, BLAST_LIGHT_SECONDS);
        float lethalDamage = (bomber.getMaxHitpoints() + bomber.getHitpoints()) * SELF_DESTRUCT_DAMAGE_MULT;
        engine.applyDamage(bomber, at, lethalDamage, DamageType.HIGH_EXPLOSIVE, 0f, true, false, bomber);
    }

    static DamagingExplosionSpec blast(float damage, DamageType damageType, float radius, Color color) {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(BLAST_SECONDS, radius, radius * BLAST_CORE_SHARE, damage,
                damage * BLAST_EDGE_DAMAGE_SHARE, CollisionClass.HITS_SHIPS_AND_ASTEROIDS, CollisionClass.HITS_SHIPS_AND_ASTEROIDS, 3f, 3f, 0.5f, 0,
                color, color);
        spec.setDamageType(damageType == null ? DamageType.HIGH_EXPLOSIVE : damageType);
        spec.setShowGraphic(false);
        return spec;
    }
}
