package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.Color;

final class RefractionDrone extends SingleShotDrone {

    static final String RANGE_MATCH_MOD_ID = "exiledSector_refractionRange";

    private static final float STREAK_FULL_SECONDS = 0.05f;
    private static final float STREAK_FADE_SECONDS = 0.15f;
    private static final float DEFAULT_STREAK_WIDTH = 10f;
    private static final float MIN_STREAK_GAP = 1f;

    private final Vector2f aimPoint = new Vector2f();
    private EnergyChainListener chain;
    private ChainLink link;
    private ShipAPI target;

    RefractionDrone(ShipAPI drone, RefractionDrones pool) {
        super(drone, pool, RANGE_MATCH_MOD_ID);
    }

    void launch(EnergyChainListener newChain, ShipAPI hitShip, Vector2f impactPoint, ShipAPI newTarget, ChainLink newLink, float sourceRange) {
        prepare(sourceRange);
        chain = newChain;
        link = newLink;
        target = newTarget;
        Vector2f exit = Refraction.origin(hitShip, impactPoint, newTarget.getLocation());
        Vector2f direction = VectorUtils.getDirectionalVector(exit, newTarget.getLocation());
        origin().set(exit.x + direction.x * CLEARANCE, exit.y + direction.y * CLEARANCE);
        fire();
        drawStreak(impactPoint);
    }

    @Override
    StatBonus rangeBonus(MutableShipStatsAPI stats) {
        return stats.getEnergyWeaponRangeBonus();
    }

    @Override
    boolean isTagged(DamagingProjectileAPI projectile) {
        return ChainLink.isTagged(projectile);
    }

    @Override
    void tag(DamagingProjectileAPI projectile) {
        link.tag(projectile);
    }

    @Override
    void shotHit(DamagingProjectileAPI projectile, CombatEntityAPI hitTarget, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!isTagged(projectile) && link != null) {
            handBack(projectile);
        }
        chain.handleHit(projectile, hitTarget, damage, point, shieldHit);
    }

    @Override
    void aim() {
        Vector2f origin = origin();
        Vector2f lead = AIUtils.getBestInterceptPoint(origin, weapon().getProjectileSpeed(), target.getLocation(), target.getVelocity());
        aimPoint.set(lead != null ? lead : target.getLocation());
        aimAlong(VectorUtils.getAngle(origin, aimPoint), target, aimPoint.x, aimPoint.y);
    }

    private void drawStreak(Vector2f impactPoint) {
        Vector2f origin = origin();
        float gap = MathUtils.getDistance(impactPoint, origin);
        if (gap < MIN_STREAK_GAP || !(weapon().getSpec().getProjectileSpec() instanceof ProjectileSpecAPI spec)) {
            return;
        }
        Color core = spec.getCoreColor() != null ? spec.getCoreColor() : spec.getGlowColor();
        Color fringe = spec.getFringeColor() != null ? spec.getFringeColor() : spec.getGlowColor();
        if (core == null || fringe == null) {
            return;
        }
        float width = spec.getWidth() > 0f ? spec.getWidth() : DEFAULT_STREAK_WIDTH;
        MagicFakeBeamPlugin.addBeam(STREAK_FULL_SECONDS, STREAK_FADE_SECONDS, width, new Vector2f(impactPoint),
                VectorUtils.getAngle(impactPoint, origin), gap, core, fringe);
    }
}
