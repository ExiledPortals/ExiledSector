package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.i18n.StyledText;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public enum CombatSkillEffect implements BackedSkillEffect {

    BEAM_WEAPON_SPLIT_TARGETS_FLAT(BeamSplitListener.TARGETS_KEY, BeamSplitListener.class, BeamSplitListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    EXPLODE_ON_DEATH(DeathExplosionListener.FUEL_DAMAGE_PERCENT_KEY, DeathExplosionListener.class, DeathExplosionListener::new),
    DEATH_ON_COLLISION(CollisionDeathListener.class, CollisionDeathListener::new),
    NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT(EnergyChainListener.CHANCE_KEY, EnergyChainListener.class, EnergyChainListener::new),
    NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT(EnergyChainListener.FALLOFF_KEY, EnergyChainListener.class, EnergyChainListener::new),
    ESCORT_MANEUVER_BONUS_PERCENT(EscortListener.MANEUVER_BONUS_KEY, EscortListener.class, EscortListener::new),
    ESCORT_SPEED_BONUS_PERCENT(EscortListener.SPEED_BONUS_KEY, EscortListener.class, EscortListener::new),
    ESCORT_WEAPON_RANGE_BONUS_PERCENT(EscortListener.WEAPON_RANGE_BONUS_KEY, EscortListener.class, EscortListener::new),
    ESCORT_PROXIMITY_RANGE_FLAT(EscortListener.PROXIMITY_RANGE_KEY, EscortListener.class, EscortListener::new),
    NANOFORGE_HULL_REGEN_PERCENT(NanoforgeMendingListener.REGEN_PERCENT_KEY, NanoforgeMendingListener.class, NanoforgeMendingListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("seconds", NanoforgeMendingListener.UNDAMAGED_SECONDS).arg("value", magnitude)
                    .arg("limit", NanoforgeMendingListener.MAX_TOTAL_REGEN_PERCENT_OF_HULL).styled();
        }
    },
    DISINTEGRATION_ARMOR_DAMAGE_PERCENT(DisintegrationListener.ARMOR_DAMAGE_PERCENT_KEY,
            DisintegrationListener.class, DisintegrationListener::new),
    TERRIFYING_PRESENCE_ACCURACY_PENALTY_PERCENT(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY,
            TerrifyingPresenceListener.class, TerrifyingPresenceListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("range", TerrifyingPresenceListener.RANGE).arg("value", magnitude).styled();
        }
    };

    private static final String NON_BEAM_ENERGY_CHAIN_HIT_LIST_KEY = "exiledSector_energyChainHitList";
    private static final String NON_BEAM_ENERGY_CHAIN_COUNT_KEY = "exiledSector_energyChainCount";
    private static final String NON_BEAM_ENERGY_CHAIN_DEALT_MULT_KEY = "exiledSector_energyChainDealtMult";

    private final EffectBacking backing;

    <T> CombatSkillEffect(Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(null, listenerType, listenerFactory);
    }

    <T> CombatSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this.backing = new ListenerEffect(magnitudeKey, listenerType, listenerFactory);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }

    private static final class EnergyChainListener implements DamageDealtModifier {

        private static final String CHANCE_KEY = "exiledSector_energyChainChance";
        private static final String FALLOFF_KEY = "exiledSector_energyChainFalloff";

        private final ShipAPI ship;

        private EnergyChainListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI proj) || !(target instanceof ShipAPI targetShip)
                    || !canChainFrom(proj.getWeapon())) {
                return null;
            }
            WeaponAPI weapon = proj.getWeapon();

            float fullDamage = damage.getDamage();
            float dealtMult = dealtMultOf(proj);
            if (dealtMult < 1f) {
                ChainHitDamageRestorer.reduceForThisHit(damage, dealtMult, targetShip);
            }
            if (shieldHit) {
                tryChain(proj, weapon, targetShip, point, fullDamage, dealtMult);
            }
            return null;
        }

        private static boolean canChainFrom(WeaponAPI weapon) {
            return weapon != null && !weapon.isBeam() && weapon.getType() == WeaponAPI.WeaponType.ENERGY
                    && !CsvIdBlocklist.ENERGY_CHAIN_WEAPONS.contains(weapon.getId());
        }

        private static float dealtMultOf(DamagingProjectileAPI proj) {
            return proj.getCustomData().get(NON_BEAM_ENERGY_CHAIN_DEALT_MULT_KEY) instanceof Float mult ? mult : 1f;
        }

        private void tryChain(DamagingProjectileAPI proj, WeaponAPI weapon, ShipAPI target, Vector2f point,
                              float fullDamage, float dealtMult) {
            Map<String, Object> customData = proj.getCustomData();
            int chainCount = customData.get(NON_BEAM_ENERGY_CHAIN_COUNT_KEY) instanceof Integer integer ? integer : 0;
            float chancePercent = ship.getMutableStats().getDynamic().getValue(CHANCE_KEY, 0f);
            boolean chains = chainCount < MaxChainCountConfig.get() && chancePercent > 0f
                    && Math.random() < chancePercent / 100.0;
            if (!chains) {
                return;
            }

            List<ShipAPI> hitSoFar = hitList(customData);
            hitSoFar.add(target);
            ShipAPI nextTarget = findNearestChainTarget(point, weapon.getRange(), hitSoFar);
            float falloffPercent = ship.getMutableStats().getDynamic().getValue(FALLOFF_KEY, 0f);
            float nextDealtMult = dealtMult * (1f - falloffPercent / 100f);
            if (nextTarget == null || nextDealtMult <= 0f) {
                return;
            }

            ChainShot shot = new ChainShot(fullDamage, nextDealtMult, hitSoFar, chainCount + 1);
            spawnChainProjectile(weapon, proj, point, nextTarget, shot);
        }

        // unchecked: the hit list is only ever written by this class as List<ShipAPI>
        @SuppressWarnings("unchecked")
        private List<ShipAPI> hitList(Map<String, Object> customData) {
            List<ShipAPI> hitSoFar = new ArrayList<>();
            if (customData.get(NON_BEAM_ENERGY_CHAIN_HIT_LIST_KEY) instanceof List<?> storedHits) {
                hitSoFar.addAll((List<ShipAPI>) storedHits);
            } else {
                hitSoFar.add(ship);
            }
            return hitSoFar;
        }

        private ShipAPI findNearestChainTarget(Vector2f point, float range, List<ShipAPI> excluded) {
            ShipAPI nearest = null;
            float nearestDistanceSq = Float.MAX_VALUE;
            for (ShipAPI candidate : CombatQueries.shipsNear(point, range, other -> !excluded.contains(other) && other.isAlive() && !other.isHulk()
                    && CombatQueries.isHostile(ship, other) && CombatQueries.withinRadius(other.getLocation(), point, range))) {
                float distanceSq = Vector2f.sub(candidate.getLocation(), point, null).lengthSquared();
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearest = candidate;
                }
            }
            return nearest;
        }

        private void spawnChainProjectile(WeaponAPI weapon, DamagingProjectileAPI source, Vector2f from,
                                          ShipAPI target, ChainShot shot) {
            CombatEngineAPI engine = Global.getCombatEngine();
            float facing = VectorUtils.getAngle(from, target.getLocation());
            CombatEntityAPI spawned = engine.spawnProjectile(ship, weapon, weapon.getId(), source.getProjectileSpecId(),
                    from, facing, new Vector2f());
            if (spawned instanceof DamagingProjectileAPI chainProj) {
                DamageAPI chainDamage = chainProj.getDamage();
                float existingScaling = chainDamage.getModifier().getModifiedValue() * chainDamage.getMultiplier();
                if (existingScaling > 0f) {
                    chainDamage.setDamage(shot.fullDamage() / existingScaling);
                }
                chainProj.setCustomData(NON_BEAM_ENERGY_CHAIN_HIT_LIST_KEY, shot.hitSoFar());
                chainProj.setCustomData(NON_BEAM_ENERGY_CHAIN_COUNT_KEY, shot.chainCount());
                chainProj.setCustomData(NON_BEAM_ENERGY_CHAIN_DEALT_MULT_KEY, shot.dealtMult());
            }
        }

        private record ChainShot(float fullDamage, float dealtMult, List<ShipAPI> hitSoFar, int chainCount) {
        }
    }

    private static final class DeathExplosionListener implements HullDamageAboutToBeTakenListener {

        private static final String FUEL_DAMAGE_PERCENT_KEY = "exiledSector_explodeOnDeathFuelDamagePercent";

        private static final float RADIUS_MULT = 4f;
        private static final float MIN_RADIUS = 300f;
        private static final Color BLAST_COLOR = new Color(255, 200, 120, 40);
        private static final Color CORE_COLOR = new Color(255, 255, 255, 60);

        private final ShipAPI ship;
        private boolean exploded;

        private DeathExplosionListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (exploded || damageAmount < ship.getHitpoints()) {
                return false;
            }
            exploded = true;
            detonate();
            return false;
        }

        private void detonate() {
            CombatEngineAPI engine = Global.getCombatEngine();
            Vector2f loc = ship.getLocation();
            float radius = Math.max(MIN_RADIUS, ship.getCollisionRadius() * RADIUS_MULT);
            float damage = fuelDamage();

            List<ShipAPI> nearby = CombatQueries.shipsMatching(other -> other != ship && !other.isHulk() && !other.isShuttlePod()
                    && CombatQueries.withinRadius(other.getLocation(), loc, radius));
            for (ShipAPI other : nearby) {
                float distance = Vector2f.sub(other.getLocation(), loc, null).length();
                float dealt = damage * (radius - distance) / radius;
                if (dealt <= 0f) {
                    continue;
                }
                engine.applyDamage(other, other.getLocation(), dealt, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
            }

            engine.spawnExplosion(loc, new Vector2f(), BLAST_COLOR, radius, 1.2f);
            engine.spawnExplosion(loc, new Vector2f(), CORE_COLOR, radius * 0.5f, 0.6f);
        }

        private float fuelDamage() {
            FleetMemberAPI member = ship.getMutableStats().getFleetMember();
            if (member == null) {
                return 0f;
            }
            float fuelDamagePercent = ship.getMutableStats().getDynamic().getValue(FUEL_DAMAGE_PERCENT_KEY, 0f);
            return member.getFuelCapacity() * fuelDamagePercent / 100f;
        }
    }

    private static final class CollisionDeathListener implements AdvanceableListener {

        private static final float COLLISION_CHECK_GRACE_PERIOD = 1.5f;
        private static final float BROAD_PHASE_MARGIN = 1.1f;
        private static final float LETHAL_DAMAGE = 999999f;

        private final ShipAPI ship;
        private boolean triggered;
        private float aliveTime;

        private CollisionDeathListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (triggered || !ship.isAlive() || ship.isHulk()) {
                return;
            }
            aliveTime += amount;
            if (aliveTime < COLLISION_CHECK_GRACE_PERIOD || ship.getCollisionClass() == CollisionClass.NONE) {
                return;
            }
            if (isHullTouchingAnything()) {
                triggered = true;
                Global.getCombatEngine().applyDamage(ship, ship.getLocation(), LETHAL_DAMAGE,
                        DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
            }
        }

        private boolean isHullTouchingAnything() {
            Vector2f loc = ship.getLocation();
            float queryRadius = ship.getCollisionRadius() * BROAD_PHASE_MARGIN;
            CombatEngineAPI engine = Global.getCombatEngine();
            return CombatQueries.anyNear(engine.getShipGrid(), loc, queryRadius,
                    candidate -> candidate instanceof ShipAPI other && isCollidableShip(other) && isTouching(other))
                    || CombatQueries.anyNear(engine.getAsteroidGrid(), loc, queryRadius,
                    candidate -> candidate instanceof CombatEntityAPI asteroid && isTouching(asteroid));
        }

        private boolean isCollidableShip(ShipAPI other) {
            return other != ship && !other.isFighter() && !other.isHulk() && !other.isShuttlePod()
                    && other.getCollisionClass() != CollisionClass.NONE;
        }

        private boolean isTouching(CombatEntityAPI other) {
            return isBroadPhaseNear(ship.getLocation(), ship.getCollisionRadius(), other) && hullsOverlap(ship, other);
        }

        private boolean isBroadPhaseNear(Vector2f loc, float myRadius, CombatEntityAPI other) {
            float triggerRadius = (myRadius + other.getCollisionRadius()) * BROAD_PHASE_MARGIN;
            return CombatQueries.withinRadius(other.getLocation(), loc, triggerRadius);
        }

        private boolean hullsOverlap(CombatEntityAPI a, CombatEntityAPI b) {
            BoundsAPI boundsA = a.getExactBounds();
            BoundsAPI boundsB = b.getExactBounds();
            if (boundsA == null || boundsB == null) {
                return false;
            }
            boundsA.update(a.getLocation(), a.getFacing());
            boundsB.update(b.getLocation(), b.getFacing());

            List<BoundsAPI.SegmentAPI> segmentsA = boundsA.getSegments();
            List<BoundsAPI.SegmentAPI> segmentsB = boundsB.getSegments();
            for (BoundsAPI.SegmentAPI segA : segmentsA) {
                for (BoundsAPI.SegmentAPI segB : segmentsB) {
                    if (segmentsIntersect(segA.getP1(), segA.getP2(), segB.getP1(), segB.getP2())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean segmentsIntersect(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4) {
            float d1 = cross(p3, p4, p1);
            float d2 = cross(p3, p4, p2);
            float d3 = cross(p1, p2, p3);
            float d4 = cross(p1, p2, p4);
            return ((d1 > 0f) != (d2 > 0f)) && ((d3 > 0f) != (d4 > 0f));
        }

        private float cross(Vector2f a, Vector2f b, Vector2f c) {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        }
    }

    private static final class EscortListener implements AdvanceableListener {

        private static final String MANEUVER_BONUS_KEY = "exiledSector_escortManeuverBonusPercent";
        private static final String SPEED_BONUS_KEY = "exiledSector_escortSpeedBonusPercent";
        private static final String WEAPON_RANGE_BONUS_KEY = "exiledSector_escortWeaponRangeBonusPercent";
        private static final String PROXIMITY_RANGE_KEY = "exiledSector_escortProximityRange";

        private static final float PROXIMITY_FADE_DISTANCE = 500f;
        private static final float SHIELD_RADIUS_OVERLAP_MULT = 0.75f;
        private static final float DESTROYER_ESCORTING_CAPITAL_MULT = 2f;
        private static final float SEARCH_MARGIN = 300f;
        private static final float TURN_ACCELERATION_MULT = 2f;
        private static final float EASE_SECONDS = 0.5f;
        private static final float MAG_SNAP = 0.001f;
        private static final String ESCORT_BONUS_MOD_ID = "exiledSector_escortBonus";

        private final ShipAPI ship;
        private final IntervalUtil interval = new IntervalUtil(0.9f, 1.1f);
        private float targetMag;
        private float appliedMag;
        private float easeRate;

        private EscortListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive() || ship.isHulk()) {
                return;
            }
            interval.advance(amount);
            boolean retargeted = interval.intervalElapsed();
            if (retargeted) {
                targetMag = proximityMagnitude();
                easeRate = Math.abs(targetMag - appliedMag) / EASE_SECONDS;
            }
            if (retargeted || appliedMag != targetMag) {
                appliedMag = approach(appliedMag, targetMag, easeRate * amount);
                applyBonuses(appliedMag);
            }
        }

        private static float approach(float current, float target, float maxStep) {
            if (Math.abs(target - current) <= maxStep + MAG_SNAP) {
                return target;
            }
            return current + Math.signum(target - current) * maxStep;
        }

        private float proximityMagnitude() {
            float range = ship.getMutableStats().getDynamic().getValue(PROXIMITY_RANGE_KEY, 0f);
            float searchRadius = range + PROXIMITY_FADE_DISTANCE + ship.getCollisionRadius() + SEARCH_MARGIN;
            float best = 0f;
            for (ShipAPI escorted : CombatQueries.shipsNear(ship.getLocation(), searchRadius, this::isLargerFriendly)) {
                best = Math.max(best, magnitudeFor(escorted, range));
            }
            return best;
        }

        private boolean isLargerFriendly(ShipAPI candidate) {
            return candidate != ship && candidate.getOwner() == ship.getOwner() && candidate.isAlive() && !candidate.isHulk()
                    && candidate.getHullSize().ordinal() > ship.getHullSize().ordinal();
        }

        private float magnitudeFor(ShipAPI escorted, float range) {
            float radiusOverlap = (ship.getShieldRadiusEvenIfNoShield() + escorted.getShieldRadiusEvenIfNoShield())
                    * SHIELD_RADIUS_OVERLAP_MULT;
            float distance = Vector2f.sub(ship.getShieldCenterEvenIfNoShield(),
                    escorted.getShieldCenterEvenIfNoShield(), null).length() - radiusOverlap;

            float mag;
            if (distance < range) {
                mag = 1f;
            } else if (distance < range + PROXIMITY_FADE_DISTANCE) {
                mag = 1f - (distance - range) / PROXIMITY_FADE_DISTANCE;
            } else {
                mag = 0f;
            }

            if (ship.isDestroyer() && escorted.isCapital()) {
                mag *= DESTROYER_ESCORTING_CAPITAL_MULT;
            }
            return mag;
        }

        private void applyBonuses(float mag) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            MutableStat[] maneuverStats = {stats.getAcceleration(), stats.getDeceleration(), stats.getMaxTurnRate()};
            StatBonus[] rangeStats = {stats.getBallisticWeaponRangeBonus(), stats.getEnergyWeaponRangeBonus()};

            if (mag <= 0f) {
                for (MutableStat stat : maneuverStats) {
                    stat.unmodify(ESCORT_BONUS_MOD_ID);
                }
                stats.getTurnAcceleration().unmodify(ESCORT_BONUS_MOD_ID);
                stats.getMaxSpeed().unmodify(ESCORT_BONUS_MOD_ID);
                for (StatBonus stat : rangeStats) {
                    stat.unmodify(ESCORT_BONUS_MOD_ID);
                }
                return;
            }

            float maneuverPercent = stats.getDynamic().getValue(MANEUVER_BONUS_KEY, 0f) * mag;
            for (MutableStat stat : maneuverStats) {
                stat.modifyPercent(ESCORT_BONUS_MOD_ID, maneuverPercent);
            }
            stats.getTurnAcceleration().modifyPercent(ESCORT_BONUS_MOD_ID, maneuverPercent * TURN_ACCELERATION_MULT);
            stats.getMaxSpeed().modifyPercent(ESCORT_BONUS_MOD_ID,
                    stats.getDynamic().getValue(SPEED_BONUS_KEY, 0f) * mag);

            float rangePercent = stats.getDynamic().getValue(WEAPON_RANGE_BONUS_KEY, 0f) * mag;
            for (StatBonus stat : rangeStats) {
                stat.modifyPercent(ESCORT_BONUS_MOD_ID, rangePercent);
            }
        }
    }
}
