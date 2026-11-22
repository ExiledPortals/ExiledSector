package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

final class LensStrike {

    static final float RANGE = 400f;
    static final float DAMAGE = 75f;
    private static final float EXPLOSION_RADIUS = 50f;
    private static final float CLOSE_DISTANCE = 100f;
    private static final float HIT_SPREAD_DEGREES = 90f;
    private static final float FAR_POINT_DISTANCE = 10000f;
    private static final float BEAM_THICKNESS = 30f;
    private static final String EXPLOSION_SOUND_ID = "abyssal_glare_explosion";

    private LensStrike() {
    }

    static WeightedRandomPicker<CombatEntityAPI> targetsNear(ShipAPI ship, Random random) {
        WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<>(random);
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Vector2f from = ship.getLocation();
        Iterator<Object> nearby = CombatQueries.objectsNear(Global.getCombatEngine().getAllObjectGrid(), from, RANGE);
        while (nearby.hasNext()) {
            Object nearbyObject = nearby.next();
            if (nearbyObject instanceof CombatEntityAPI candidate && seen.add(candidate) && isStrikeable(ship, candidate)) {
                float edgeDistance = Misc.getDistance(from, candidate.getLocation()) - Misc.getTargetingRadius(from, candidate, false)
                        - Misc.getTargetingRadius(candidate.getLocation(), ship, false);
                if (edgeDistance <= RANGE) {
                    picker.add(candidate, weight(edgeDistance));
                }
            }
        }
        return picker;
    }

    static boolean isStrikeable(ShipAPI ship, CombatEntityAPI candidate) {
        if (candidate.getCollisionClass() == CollisionClass.NONE || !CombatQueries.isHostileOwner(ship, candidate.getOwner())) {
            return false;
        }
        if (candidate instanceof ShipAPI candidateShip) {
            return !candidateShip.isHulk() && !candidateShip.isPhased() && candidateShip.isTargetable();
        }
        return candidate instanceof MissileAPI;
    }

    static float weight(float edgeDistance) {
        return CLOSE_DISTANCE / Math.max(CLOSE_DISTANCE, edgeDistance);
    }

    static void strike(ShipAPI ship, CombatEntityAPI target, Random random) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f point = hitPoint(ship, target, random);
        DamagingProjectileAPI explosion = engine.spawnDamagingExplosion(explosionSpec(), ship, point);
        RiftCascadeMineExplosion.spawnStandardRift(explosion, riftParams());
        EmpArcEntityAPI beam = engine.spawnEmpArcVisual(ship.getLocation(), ship, point, explosion, BEAM_THICKNESS,
                DwellerShroud.SHROUD_GLOW_COLOR, DwellerShroud.SHROUD_GLOW_COLOR, beamParams(random));
        beam.setCoreWidthOverride(BEAM_THICKNESS);
        beam.setSingleFlickerMode();
        beam.setRenderGlowAtStart(false);
        beam.setWarping(0.2f);
    }

    private static Vector2f hitPoint(ShipAPI ship, CombatEntityAPI target, Random random) {
        float angle = Misc.getAngleInDegrees(target.getLocation(), ship.getLocation()) + HIT_SPREAD_DEGREES / 2f
                - HIT_SPREAD_DEGREES * random.nextFloat();
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(angle);
        Vector2f farPoint = new Vector2f(direction);
        farPoint.scale(FAR_POINT_DISTANCE);
        Vector2f.add(target.getLocation(), farPoint, farPoint);
        Vector2f point = new Vector2f(direction);
        point.scale(Misc.getTargetingRadius(farPoint, target, false) * (0.8f + 0.4f * random.nextFloat()));
        return Vector2f.add(target.getLocation(), point, point);
    }

    private static DamagingExplosionSpec explosionSpec() {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, EXPLOSION_RADIUS, EXPLOSION_RADIUS * 0.5f, DAMAGE, DAMAGE,
                CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER, 3f, 3f, 0.5f, 0,
                new Color(255, 255, 255, 0), new Color(255, 100, 100, 0));
        spec.setDamageType(DamageType.ENERGY);
        spec.setUseDetailedExplosion(false);
        spec.setSoundSetId(EXPLOSION_SOUND_ID);
        spec.setSoundVolume(0.33f);
        return spec;
    }

    private static NEParams riftParams() {
        NEParams params = RiftCascadeMineExplosion.createStandardRiftParams(RiftLightningEffect.RIFT_LIGHTNING_COLOR, 7f);
        params.noiseMult = 6f;
        params.thickness = 25f;
        params.fadeOut = 0.5f;
        params.spawnHitGlowAt = 1f;
        params.additiveBlend = true;
        params.blackColor = Color.white;
        params.underglow = null;
        params.withNegativeParticles = false;
        params.withHitGlow = false;
        params.fadeIn = 0f;
        return params;
    }

    private static EmpArcParams beamParams(Random random) {
        EmpArcParams params = new EmpArcParams();
        params.segmentLengthMult = 4f;
        params.maxZigZagMult = 0.25f;
        params.zigZagReductionFactor = 1f;
        params.flickerRateMult = 0.75f + 0.25f * random.nextFloat();
        params.fadeOutDist = 150f;
        params.minFadeOutMult = 5f;
        params.glowSizeMult = 0.5f;
        return params;
    }
}
