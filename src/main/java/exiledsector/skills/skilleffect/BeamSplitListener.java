package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.magiclib.util.MagicFakeBeam;

import java.util.Comparator;
import java.util.List;

final class BeamSplitListener implements DamageDealtModifier, AdvanceableListener, DroneSpawner {

    static final String TARGETS_KEY = "exiledSector_beamSplitTargets";

    private static final String DAMAGE_SHARE_MOD_ID = "exiledSector_beamSplitShare";
    private static final float SPLIT_RADIUS_MULT_OF_BEAM_RANGE = 0.5f;
    private static final float SIMULATED_BEAM_FULL_DURATION = 0.05f;
    private static final float SIMULATED_BEAM_FADE_DURATION = 0.15f;

    private static boolean applyingSimulatedHit;

    private final ShipAPI ship;
    private final SplitBeamDrones drones;
    private boolean processingSplit;

    static boolean isApplyingSimulatedHit() {
        return applyingSimulatedHit;
    }

    static void beginSimulatedHit() {
        applyingSimulatedHit = true;
    }

    static void endSimulatedHit() {
        applyingSimulatedHit = false;
    }

    BeamSplitListener(ShipAPI ship) {
        this.ship = ship;
        this.drones = new SplitBeamDrones(ship);
    }

    @Override
    public void advance(float amount) {
        drones.advance(amount);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (processingSplit || !(param instanceof BeamAPI beam) || !(target instanceof ShipAPI primaryTarget)) {
            return null;
        }
        int splitCount = Math.round(ship.getMutableStats().getDynamic().getValue(TARGETS_KEY, 0f));
        WeaponAPI weapon = beam.getWeapon();
        List<ShipAPI> splitTargets = splitCount <= 0 ? List.of()
                : findNearbyEnemies(primaryTarget, point, weapon.getRange() * SPLIT_RADIUS_MULT_OF_BEAM_RANGE, splitCount);
        if (splitTargets.isEmpty()) {
            return null;
        }

        float share = 1f / (1 + splitTargets.size());
        if (WeaponDroneFactory.supportsBeam(weapon)) {
            for (ShipAPI splitTarget : splitTargets) {
                drones.refresh(weapon, primaryTarget, splitTarget, point, share);
            }
        } else {
            simulateSplit(beam, damage, point, splitTargets, share);
        }
        damage.getModifier().modifyMult(DAMAGE_SHARE_MOD_ID, share);
        return DAMAGE_SHARE_MOD_ID;
    }

    private void simulateSplit(BeamAPI beam, DamageAPI damage, Vector2f point, List<ShipAPI> splitTargets, float share) {
        float tickDamage = damage.isDps() ? damage.getDamage() * damage.getDpsDuration() : damage.getDamage();
        float perTargetDamage = tickDamage * share;
        float perTargetEmp = perTargetDamage * empToDamageRatio(beam.getWeapon());
        CombatEngineAPI engine = Global.getCombatEngine();
        processingSplit = true;
        try {
            for (ShipAPI splitTarget : splitTargets) {
                spawnSimulatedBeam(engine, point, splitTarget, beam, perTargetDamage, perTargetEmp, damage);
            }
        } finally {
            processingSplit = false;
        }
    }

    private static float empToDamageRatio(WeaponAPI weapon) {
        WeaponAPI.DerivedWeaponStatsAPI stats = weapon.getDerivedStats();
        float dps = stats.getDps();
        return dps > 0f ? stats.getEmpPerSecond() / dps : 0f;
    }

    private List<ShipAPI> findNearbyEnemies(ShipAPI primaryTarget, Vector2f point, float radius, int count) {
        List<ShipAPI> candidates = CombatQueries.shipsNear(point, radius, other -> other != ship && other != primaryTarget
                && other.isAlive() && !other.isHulk() && other.getCollisionClass() != CollisionClass.NONE
                && CombatQueries.isHostile(ship, other) && CombatQueries.withinRadius(other.getLocation(), point, radius));
        candidates.sort(Comparator.comparingDouble(other -> Vector2f.sub(other.getLocation(), point, null).lengthSquared()));
        return candidates.size() > count ? candidates.subList(0, count) : candidates;
    }

    private void spawnSimulatedBeam(CombatEngineAPI engine, Vector2f from, ShipAPI splitTarget,
                                    BeamAPI sourceBeam, float damageAmount, float empAmount, DamageAPI hitDamage) {
        float angle = VectorUtils.getAngle(from, splitTarget.getLocation());
        float range = MathUtils.getDistance(from, splitTarget.getLocation()) + 50f;
        Vector2f segEnd = MathUtils.getPoint(from, range, angle);
        Vector2f impactPoint = MagicFakeBeam.getShipCollisionPoint(from, segEnd, splitTarget, angle);
        if (impactPoint == null) {
            impactPoint = splitTarget.getLocation();
        }

        FluxTrackerAPI flux = splitTarget.getFluxTracker();
        float fluxBefore = flux.getCurrFlux();
        beginSimulatedHit();
        try {
            engine.applyDamage(sourceBeam, splitTarget, impactPoint, damageAmount, hitDamage.getType(), empAmount,
                    false, !hitDamage.isForceHardFlux(), ship, false);
        } finally {
            endSimulatedHit();
        }
        if (!hitDamage.isForceHardFlux()) {
            ShieldSkillEffect.convertToHardFlux(flux, flux.getCurrFlux() - fluxBefore,
                    ship.getMutableStats().getDynamic().getValue(ShieldSkillEffect.BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, 0f));
        }

        float impactSize = sourceBeam.getWidth() * 2f;
        engine.addHitParticle(impactPoint, new Vector2f(), impactSize, 1f,
                SIMULATED_BEAM_FULL_DURATION + SIMULATED_BEAM_FADE_DURATION, sourceBeam.getFringeColor());
        MagicFakeBeamPlugin.addBeam(SIMULATED_BEAM_FULL_DURATION, SIMULATED_BEAM_FADE_DURATION, sourceBeam.getWidth(),
                from, angle, MathUtils.getDistance(from, impactPoint) + 10f,
                sourceBeam.getCoreColor(), sourceBeam.getFringeColor());
    }
}
