package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledlib.compat.CombatGlow;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

final class IonArcListener extends ShipCombatListener implements DamageDealtModifier {

    static final String CHANCE_AT_FULL_HARD_FLUX_KEY = "exiledSector_ionArcChanceAtFullHardFlux";
    static final float ARC_DAMAGE_FRACTION = 0.25f;
    static final float ARC_EMP_FRACTION = 0.5f;
    private static final float ARC_RANGE = 100000f;
    private static final float ARC_THICKNESS = 10f;
    private static final Color ARC_FRINGE = new Color(95, 98, 205, 255);
    private static final Color ARC_CORE = new Color(220, 225, 255, 255);
    private static final String ARC_SOUND = "tachyon_lance_emp_impact";
    private static final float ARC_LIGHT_SIZE = 80f;
    private static final float ARC_LIGHT_INTENSITY = 1f;
    private static final float ARC_LIGHT_SECONDS = 0.35f;
    private static final float IMPACT_LIGHT_SIZE = 120f;
    private static final float IMPACT_LIGHT_INTENSITY = 0.8f;
    private static final float IMPACT_LIGHT_SECONDS = 0.3f;

    private final Random random;

    IonArcListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    IonArcListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    static float arcChance(float chanceAtFullHardFluxPercent, ShipAPI target) {
        FluxTrackerAPI flux = target.getFluxTracker();
        float maxFlux = flux.getMaxFlux();
        float hardFluxLevel = maxFlux <= 0f ? 0f : Math.min(1f, flux.getHardFlux() / maxFlux);
        return chanceAtFullHardFluxPercent / 100f * hardFluxLevel;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; arcs are spawned as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!shieldHit || !(param instanceof DamagingProjectileAPI projectile) || !(target instanceof ShipAPI hitShip)
                || !isEnergyProjectile(projectile.getWeapon()) || !CombatQueries.isHostile(ownerShip, hitShip) || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (random.nextFloat() < arcChance(magnitude(CHANCE_AT_FULL_HARD_FLUX_KEY), hitShip)) {
            arc(hitShip, point, damage.getDamage());
        }
        return null;
    }

    private static boolean isEnergyProjectile(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && weapon.getType() == WeaponAPI.WeaponType.ENERGY;
    }

    private void arc(ShipAPI hitShip, Vector2f point, float shotDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        EmpArcEntityAPI arc = engine.spawnEmpArcPierceShields(ownerShip, new Vector2f(point), hitShip, hitShip, DamageType.ENERGY,
                shotDamage * ARC_DAMAGE_FRACTION, shotDamage * ARC_EMP_FRACTION, ARC_RANGE, ARC_SOUND, ARC_THICKNESS, ARC_FRINGE, ARC_CORE);
        Vector2f arcEnd = arc == null ? null : arc.getTargetLocation();
        if (arcEnd != null) {
            CombatGlow.arc(point, arcEnd, ARC_FRINGE, ARC_LIGHT_SIZE, ARC_LIGHT_INTENSITY, ARC_LIGHT_SECONDS);
            CombatGlow.flash(arcEnd, null, ARC_CORE, IMPACT_LIGHT_SIZE, IMPACT_LIGHT_INTENSITY, IMPACT_LIGHT_SECONDS);
        }
    }
}
