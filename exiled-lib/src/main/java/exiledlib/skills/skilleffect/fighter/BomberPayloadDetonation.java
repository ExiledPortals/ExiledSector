package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ShotBehaviorSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import exiledlib.compat.CombatGlow;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.SkillEffectSupport;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BomberPayloadDetonation implements AdvanceableListener {

    static final float CHECK_INTERVAL_SECONDS = 3f;
    static final float SAFE_DISTANCE_FROM_CARRIER = 600f;
    static final float MIN_BLAST_RADIUS = 75f;
    static final int MAX_SPAWNED_PAYLOADS = 8;
    static final float SELF_DESTRUCT_DAMAGE = 1000000f;
    static final float BEAM_SECONDS_WITHOUT_BURST = 1f;
    private static final String PROXIMITY_FUSE_BEHAVIOR = "PROXIMITY_FUSE";
    private static final String MIRV_BEHAVIOR = "MIRV";
    private static final String BEHAVIOR_EXPLOSION_SPEC = "explosionSpec";
    private static final float BLAST_SECONDS = 0.15f;
    private static final float BLAST_CORE_SHARE = 0.5f;
    private static final float BLAST_EDGE_DAMAGE_SHARE = 0.5f;
    private static final float PROXIMITY_EDGE_DAMAGE_SHARE = 0.5f;
    private static final Color DEFAULT_BLAST_COLOR = new Color(255, 160, 80);
    private static final float BLAST_VISUAL_SIZE_SHARE = 2f;
    private static final float BLAST_VISUAL_SECONDS = 0.8f;
    private static final float BLAST_LIGHT_SIZE_SHARE = 3f;
    private static final float BLAST_LIGHT_INTENSITY = 1f;
    private static final float BLAST_LIGHT_SECONDS = 0.8f;
    private static final Logger LOG = Logger.getLogger(BomberPayloadDetonation.class);

    enum PayloadKind { OWN_EXPLOSION, PROXIMITY_FUSE, FOLDED }

    record Payload(WeaponAPI weapon, PayloadKind kind, int shots, float damagePerShot, float empPerShot, DamageType damageType,
                   float radius, float visualRadius, Color color) {

        float totalDamage() {
            return damagePerShot * shots;
        }
    }

    static final class BlastShare {
        float damage;
        float emp;
        float radius = MIN_BLAST_RADIUS;
        float strongestContribution;
        Color color;

        void add(Payload payload, int shots) {
            float contribution = payload.damagePerShot() * shots;
            damage += contribution;
            emp += payload.empPerShot() * shots;
            radius = Math.max(radius, payload.radius());
            if (payload.color() != null && contribution > strongestContribution) {
                strongestContribution = contribution;
                color = payload.color();
            }
        }

        void addEmpOnly(Payload payload, int shots) {
            emp += payload.empPerShot() * shots;
            radius = Math.max(radius, payload.radius());
        }
    }

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
        if (chancePercent <= 0f || !isBomber(bomber)) {
            return;
        }
        SkillEffectSupport.ensureListener(bomber, BomberPayloadDetonation.class,
                ship -> new BomberPayloadDetonation(ship, carrier, chancePercent, new Random()));
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
        if (!carriesPayload(bomber) || !mayDetonate() || random.nextFloat() * 100f >= chancePercent) {
            return;
        }
        detonated = true;
        detonate();
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

    static boolean carriesPayload(ShipAPI bomber) {
        boolean regeneratingOrdnanceCounts = regeneratingOrdnanceCounts(bomber);
        for (WeaponAPI weapon : bomber.getAllWeapons()) {
            if (isLoaded(weapon, regeneratingOrdnanceCounts)) {
                return true;
            }
        }
        return false;
    }

    static boolean regeneratingOrdnanceCounts(ShipAPI bomber) {
        boolean carriesFixedOrdnance = false;
        for (WeaponAPI weapon : bomber.getAllWeapons()) {
            if (weapon.isDecorative() || !weapon.usesAmmo() || regeneratesAmmo(weapon)) {
                continue;
            }
            if (isLoaded(weapon, false)) {
                return true;
            }
            carriesFixedOrdnance = true;
        }
        return !carriesFixedOrdnance;
    }

    static boolean isLoaded(WeaponAPI weapon, boolean regeneratingOrdnanceCounts) {
        if (weapon.isDecorative() || weapon.isDisabled() || weapon.isPermanentlyDisabled() || !weapon.usesAmmo()) {
            return false;
        }
        return regeneratesAmmo(weapon) ? regeneratingOrdnanceCounts : weapon.getAmmo() > 0;
    }

    static boolean regeneratesAmmo(WeaponAPI weapon) {
        if (weapon.getAmmoPerSecond() > 0f) {
            return true;
        }
        WeaponSpecAPI spec = weapon.getSpec();
        if (spec != null && spec.getAmmoPerSecond() > 0f) {
            return true;
        }
        AmmoTrackerAPI tracker = weapon.getAmmoTracker();
        return tracker != null && tracker.getAmmoPerSecond() > 0f;
    }

    static int shotsCarried(WeaponAPI weapon) {
        WeaponSpecAPI spec = weapon.getSpec();
        int burstSize = weapon.isBeam() || spec == null ? 1 : Math.max(1, spec.getBurstSize());
        int shots = Math.min(Math.max(0, weapon.getAmmo()), burstSize);
        return regeneratesAmmo(weapon) ? Math.max(1, shots) : shots;
    }

    static List<Payload> payloads(ShipAPI bomber) {
        List<Payload> payloads = new ArrayList<>();
        boolean regeneratingOrdnanceCounts = regeneratingOrdnanceCounts(bomber);
        for (WeaponAPI weapon : bomber.getAllWeapons()) {
            if (!isLoaded(weapon, regeneratingOrdnanceCounts)) {
                continue;
            }
            int shots = shotsCarried(weapon);
            if (shots > 0) {
                payloads.add(payload(bomber, weapon, shots));
            }
        }
        return payloads;
    }

    static Payload payload(ShipAPI bomber, WeaponAPI weapon, int shots) {
        WeaponSpecAPI spec = weapon.getSpec();
        float damageFactor = shipDamageFactor(bomber, weapon);
        if (weapon.isBeam() || spec instanceof BeamWeaponSpecAPI) {
            return beamPayload(weapon, shots, spec instanceof BeamWeaponSpecAPI beam ? beam : null, damageFactor);
        }
        Object projectileSpec = spec == null ? null : spec.getProjectileSpec();
        if (projectileSpec instanceof MissileSpecAPI missile) {
            return missilePayload(weapon, shots, missile, damageFactor);
        }
        DamageAPI damage = projectileSpec instanceof ProjectileSpecAPI projectile ? projectile.getDamage() : weapon.getDamage();
        return damagePayload(weapon, PayloadKind.FOLDED, shots, damage, damageFactor, MIN_BLAST_RADIUS, MIN_BLAST_RADIUS, null);
    }

    private static Payload beamPayload(WeaponAPI weapon, int shots, BeamWeaponSpecAPI beam, float damageFactor) {
        if (beam == null) {
            return new Payload(weapon, PayloadKind.FOLDED, shots, 0f, 0f, damageTypeOr(weapon.getDamageType()), MIN_BLAST_RADIUS,
                    MIN_BLAST_RADIUS, null);
        }
        float firingSeconds = beam.getBurstDuration() > 0f ? beam.getBurstDuration() : BEAM_SECONDS_WITHOUT_BURST;
        DamageType damageType = damageTypeOr(beam.getDamageType() != null ? beam.getDamageType() : weapon.getDamageType());
        return new Payload(weapon, PayloadKind.FOLDED, shots, beam.getDamagePerSecond() * firingSeconds * damageFactor,
                beam.getEMPPerSecond() * firingSeconds * damageFactor, damageType, MIN_BLAST_RADIUS, MIN_BLAST_RADIUS, beam.getGlowColor());
    }

    private static Payload missilePayload(WeaponAPI weapon, int shots, MissileSpecAPI missile, float damageFactor) {
        DamagingExplosionSpec explosion = missile.getExplosionSpec();
        ShotBehaviorSpecAPI behavior = missile.getBehaviorSpec();
        JSONObject params = behavior == null ? null : behavior.getParams();
        String behaviorName = behavior == null ? null : behavior.getBehavorString();
        float shownRadius = Math.max(MIN_BLAST_RADIUS, missile.getExplosionRadius());
        Color color = missile.getExplosionColor();
        if (explosion != null && explosion.getRadius() >= MIN_BLAST_RADIUS) {
            return damagePayload(weapon, PayloadKind.OWN_EXPLOSION, shots, missile.getDamage(), damageFactor, explosion.getRadius(),
                    Math.max(shownRadius, explosion.getRadius()), color);
        }
        JSONObject proximityExplosion = explosion == null && params != null && PROXIMITY_FUSE_BEHAVIOR.equals(behaviorName)
                ? params.optJSONObject(BEHAVIOR_EXPLOSION_SPEC) : null;
        if (proximityExplosion != null) {
            float proximityRadius = Math.max(MIN_BLAST_RADIUS, (float) proximityExplosion.optDouble("radius", 0d));
            return damagePayload(weapon, PayloadKind.PROXIMITY_FUSE, shots, missile.getDamage(), damageFactor, proximityRadius,
                    Math.max(shownRadius, proximityRadius), color);
        }
        int warheads = params != null && MIRV_BEHAVIOR.equals(behaviorName) && params.has("damage") ? params.optInt("numShots", 0) : 0;
        if (warheads > 0) {
            DamageAPI carrierStage = missile.getDamage();
            DamageType fallbackType = carrierStage != null && carrierStage.getType() != null ? carrierStage.getType() : weapon.getDamageType();
            float warheadDamage = (float) params.optDouble("damage", 0d) * damageFactor;
            float warheadEmp = (float) params.optDouble("emp", 0d) * damageFactor;
            return new Payload(weapon, PayloadKind.FOLDED, shots, warheadDamage * warheads, warheadEmp * warheads,
                    damageTypeNamed(params.optString("damageType", null), fallbackType), shownRadius, shownRadius, color);
        }
        return damagePayload(weapon, PayloadKind.FOLDED, shots, missile.getDamage(), damageFactor, shownRadius, shownRadius, color);
    }

    private static Payload damagePayload(WeaponAPI weapon, PayloadKind kind, int shots, DamageAPI damage, float damageFactor, float radius,
                                         float visualRadius, Color color) {
        if (damage == null) {
            return new Payload(weapon, kind, shots, 0f, 0f, damageTypeOr(weapon.getDamageType()), radius, visualRadius, color);
        }
        DamageType damageType = damageTypeOr(damage.getType() != null ? damage.getType() : weapon.getDamageType());
        return new Payload(weapon, kind, shots, Math.max(0f, damage.getBaseDamage()) * damageFactor,
                Math.max(0f, damage.getFluxComponent()) * damageFactor, damageType, radius, visualRadius, color);
    }

    static float shipDamageFactor(ShipAPI bomber, WeaponAPI weapon) {
        MutableShipStatsAPI stats = bomber.getMutableStats();
        if (stats == null) {
            return 1f;
        }
        float percent = 0f;
        float flat = 0f;
        float mult = 1f;
        MutableStat beamBonus = weapon.isBeam() ? stats.getBeamWeaponDamageMult() : null;
        MutableStat typeBonus = weaponTypeDamageBonus(stats, weapon.getType());
        for (MutableStat bonus : new MutableStat[]{beamBonus, typeBonus}) {
            if (bonus != null) {
                percent += bonus.getPercentMod();
                flat += bonus.getFlatMod();
                mult *= bonus.getMult();
            }
        }
        return Math.max(0f, (1f + percent / 100f + flat) * mult);
    }

    private static MutableStat weaponTypeDamageBonus(MutableShipStatsAPI stats, WeaponAPI.WeaponType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case ENERGY -> stats.getEnergyWeaponDamageMult();
            case BALLISTIC -> stats.getBallisticWeaponDamageMult();
            case MISSILE -> stats.getMissileWeaponDamageMult();
            default -> null;
        };
    }

    private static DamageType damageTypeOr(DamageType damageType) {
        return damageType == null ? DamageType.HIGH_EXPLOSIVE : damageType;
    }

    private static DamageType damageTypeNamed(String name, DamageType fallback) {
        if (name != null) {
            for (DamageType damageType : DamageType.values()) {
                if (damageType.name().equalsIgnoreCase(name.trim())) {
                    return damageType;
                }
            }
        }
        return damageTypeOr(fallback);
    }

    private void detonate() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        Vector2f at = new Vector2f(bomber.getLocation());
        List<Payload> payloads = List.of();
        try {
            payloads = payloads(bomber);
            blowUp(engine, payloads, at);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[ExiledSector] Martyr's Payload could not blow up the payload of " + bomber.getId(), e);
        }
        float visualRadius = MIN_BLAST_RADIUS;
        Color visualColor = DEFAULT_BLAST_COLOR;
        float strongestPayload = 0f;
        for (Payload payload : payloads) {
            visualRadius = Math.max(visualRadius, payload.visualRadius());
            if (payload.color() != null && payload.totalDamage() > strongestPayload) {
                strongestPayload = payload.totalDamage();
                visualColor = payload.color();
            }
        }
        engine.spawnExplosion(at, new Vector2f(), visualColor, visualRadius * BLAST_VISUAL_SIZE_SHARE, BLAST_VISUAL_SECONDS);
        CombatGlow.flash(at, null, visualColor, visualRadius * BLAST_LIGHT_SIZE_SHARE, BLAST_LIGHT_INTENSITY, BLAST_LIGHT_SECONDS);
        engine.applyDamage(bomber, at, SELF_DESTRUCT_DAMAGE, DamageType.ENERGY, 0f, true, false, bomber, false);
    }

    private void blowUp(CombatEngineAPI engine, List<Payload> payloads, Vector2f at) {
        payloads.sort(Comparator.comparingDouble(Payload::damagePerShot).reversed());
        Map<DamageType, BlastShare> foldedBlasts = new EnumMap<>(DamageType.class);
        int spawnBudget = MAX_SPAWNED_PAYLOADS;
        for (Payload payload : payloads) {
            int shotsToFold = payload.shots();
            while (payload.kind() != PayloadKind.FOLDED && shotsToFold > 0 && spawnBudget > 0) {
                spawnBudget--;
                if (!explodeOnItsOwn(engine, payload, at)) {
                    break;
                }
                shotsToFold--;
            }
            int shotsExplodedOnTheirOwn = payload.shots() - shotsToFold;
            if (shotsExplodedOnTheirOwn > 0 && payload.empPerShot() > 0f) {
                foldedBlasts.computeIfAbsent(payload.damageType(), damageType -> new BlastShare()).addEmpOnly(payload, shotsExplodedOnTheirOwn);
            }
            if (shotsToFold > 0 && payload.damagePerShot() + payload.empPerShot() > 0f) {
                foldedBlasts.computeIfAbsent(payload.damageType(), damageType -> new BlastShare()).add(payload, shotsToFold);
            }
        }
        for (Map.Entry<DamageType, BlastShare> folded : foldedBlasts.entrySet()) {
            BlastShare share = folded.getValue();
            Color shareColor = share.color != null ? share.color : DEFAULT_BLAST_COLOR;
            engine.spawnDamagingExplosion(blast(share.damage, share.emp, folded.getKey(), share.radius, shareColor), bomber, at);
        }
    }

    private boolean explodeOnItsOwn(CombatEngineAPI engine, Payload payload, Vector2f at) {
        WeaponAPI weapon = payload.weapon();
        CombatEntityAPI shot = null;
        boolean exploded = false;
        try {
            shot = engine.spawnProjectile(bomber, weapon, weapon.getId(), new Vector2f(at), bomber.getFacing(), new Vector2f(bomber.getVelocity()));
            if (shot instanceof MissileAPI missile) {
                exploded = missile.explode() != null;
                if (!exploded && payload.kind() == PayloadKind.PROXIMITY_FUSE) {
                    exploded = proximityExplosion(engine, missile, at);
                }
            }
        } catch (RuntimeException | LinkageError | JSONException e) {
            LOG.warn("[ExiledSector] Martyr's Payload could not set off " + weapon.getId() + " on its own; folding it into the blast", e);
        }
        discard(engine, shot);
        return exploded;
    }

    private boolean proximityExplosion(CombatEngineAPI engine, MissileAPI missile, Vector2f at) throws JSONException {
        ShotBehaviorSpecAPI behavior = missile.getSpec().getBehaviorSpec();
        DamagingExplosionSpec spec = DamagingExplosionSpec.loadFromJSON(behavior.getParams().getJSONObject(BEHAVIOR_EXPLOSION_SPEC));
        spec.setDamageType(damageTypeOr(missile.getDamageType()));
        spec.setMaxDamage(missile.getDamageAmount());
        spec.setMinDamage(missile.getDamageAmount() * PROXIMITY_EDGE_DAMAGE_SHARE);
        spec.setEffect(behavior.getOnHitEffect());
        DamagingProjectileAPI explosion = engine.spawnDamagingExplosion(spec, bomber, new Vector2f(at));
        ProximityExplosionEffect effect = behavior.getOnProximityExplosionEffect();
        if (effect != null) {
            try {
                effect.onExplosion(explosion, missile);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[ExiledSector] Martyr's Payload proximity explosion effect failed for " + missile.getProjectileSpecId(), e);
            }
        }
        return true;
    }

    private static void discard(CombatEngineAPI engine, CombatEntityAPI shot) {
        try {
            if (shot != null && engine.isEntityInPlay(shot)) {
                engine.removeEntity(shot);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[ExiledSector] Martyr's Payload could not remove a spawned payload shot", e);
        }
    }

    static DamagingExplosionSpec blast(float damage, float emp, DamageType damageType, float radius, Color color) {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(BLAST_SECONDS, radius, radius * BLAST_CORE_SHARE, damage,
                damage * BLAST_EDGE_DAMAGE_SHARE, CollisionClass.HITS_SHIPS_AND_ASTEROIDS, CollisionClass.HITS_SHIPS_AND_ASTEROIDS, 3f, 3f, 0.5f, 0,
                color, color);
        spec.setDamageType(damageTypeOr(damageType));
        spec.setMaxEMPDamage(emp);
        spec.setMinEMPDamage(emp * BLAST_EDGE_DAMAGE_SHARE);
        spec.setShowGraphic(false);
        return spec;
    }
}
