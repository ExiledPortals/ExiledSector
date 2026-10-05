package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.StyledText;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
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
    },
    NON_BEAM_WEAPON_DAMAGE_PER_SPEED_PERCENT(InertialSuperchargerListener.DAMAGE_PERCENT_PER_SPEED_KEY,
            InertialSuperchargerListener.class, InertialSuperchargerListener::new),
    MEDIUM_ENERGY_SLOT_WEAPON_RANGE_FLAT(MediumEnergySlotRangeListener.RANGE_FLAT_KEY,
            MediumEnergySlotRangeListener.class, ship -> new MediumEnergySlotRangeListener()),
    SHIELD_ABSORB_RATE_OF_FIRE_PERCENT(AbsorbReserveListener.RATE_OF_FIRE_PERCENT_KEY,
            AbsorbReserveListener.class, AbsorbReserveListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("full", AbsorbReserveListener.FULL_RESERVE)
                    .arg("drain", AbsorbReserveListener.DRAIN_PER_SECOND).arg("max", AbsorbReserveListener.MAX_RESERVE).styled();
        }
    },
    HEARTLESS_ON_NEARBY_DESTRUCTION_RANGE_FLAT(NearbyDestructionHeartless.RANGE_KEY,
            NearbyDestructionHeartless.class, NearbyDestructionHeartless::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("stacks", HeartlessStacks.DEFAULT_MAX_STACKS).styled();
        }
    },
    HEARTLESS_MAX_STACKS_FLAT(HeartlessStacks.MAX_STACKS_KEY, HeartlessStacks.class, HeartlessStacks::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    HEARTLESS_ARMOR_RESTORE_PERCENT(HeartlessStacks.ARMOR_RESTORE_PERCENT_KEY, HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_ARMOR_PERCENT_PER_STACK(HeartlessStacks.ARMOR_PERCENT_PER_STACK_KEY, HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_MOBILITY_PENALTY_PERCENT_PER_STACK(HeartlessStacks.MOBILITY_PENALTY_PERCENT_PER_STACK_KEY,
            HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_RADIATION_EMP_FLAT(HeartlessStacks.RADIATION_EMP_KEY, HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_CREW_DEATH_CHANCE_PERCENT_PER_STACK(LiveMunitionsListener.CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new),
    HEARTLESS_MISSILE_DAMAGE_PERCENT_PER_STACK(LiveMunitionsListener.MISSILE_DAMAGE_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new),
    HEARTLESS_MISSILE_SPEED_PERCENT_PER_STACK(LiveMunitionsListener.MISSILE_SPEED_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new),
    CREW_STEAL_RANGE_FLAT(CrewStealListener.RANGE_KEY, CrewStealListener.class, CrewStealListener::new),
    CREW_STEAL_SKELETON_CREW_PERCENT(CrewStealListener.SKELETON_CREW_PERCENT_KEY, CrewStealListener.class, CrewStealListener::new),
    FLUX_SCALED_TOP_SPEED_FLAT(FluxScaledVolatilityListener.TOP_SPEED_KEY,
            FluxScaledVolatilityListener.class, FluxScaledVolatilityListener::new),
    FLUX_SCALED_RATE_OF_FIRE_PERCENT(FluxScaledVolatilityListener.RATE_OF_FIRE_KEY,
            FluxScaledVolatilityListener.class, FluxScaledVolatilityListener::new),
    AUGMENTED_FLUX_SCALED_PENALTY_REDUCTION_PERCENT(FluxScaledVolatilityListener.AUGMENTED_PENALTY_REDUCTION_KEY,
            FluxScaledVolatilityListener.class, FluxScaledVolatilityListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return lostSectorOnly(super.description(magnitude));
        }
    },
    AUGMENTED_INERTIAL_PROJECTILE_SPEED_PERCENT(InertialSuperchargerListener.AUGMENTED_PROJECTILE_SPEED_KEY,
            InertialSuperchargerListener.class, InertialSuperchargerListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return lostSectorOnly(super.description(magnitude));
        }
    };

    private static StyledText lostSectorOnly(StyledText description) {
        return LostSectorCompat.isModEnabled() ? description : null;
    }

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
            if (isHullTouchingAnotherShip()) {
                triggered = true;
                Global.getCombatEngine().applyDamage(ship, ship.getLocation(), LETHAL_DAMAGE,
                        DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
            }
        }

        private boolean isHullTouchingAnotherShip() {
            Vector2f loc = ship.getLocation();
            float queryRadius = ship.getCollisionRadius() * BROAD_PHASE_MARGIN;
            CombatEngineAPI engine = Global.getCombatEngine();
            return CombatQueries.anyNear(engine.getShipGrid(), loc, queryRadius,
                    candidate -> candidate instanceof ShipAPI other && isCollidableShip(other) && isTouching(other));
        }

        private boolean isCollidableShip(ShipAPI other) {
            return other != ship && !other.isFighter() && !other.isHulk() && !other.isShuttlePod()
                    && other.getCollisionClass() != CollisionClass.NONE
                    && !isSameStation(other);
        }

        private boolean isSameStation(ShipAPI other) {
            ShipAPI parent = ship.getParentStation();
            ShipAPI otherParent = other.getParentStation();
            return otherParent == ship || parent == other || (parent != null && parent == otherParent);
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
            StatBonus[] rangeStats = {stats.getBallisticWeaponRangeBonus(), stats.getEnergyWeaponRangeBonus()};

            if (mag <= 0f) {
                Maneuverability.unmodify(stats, ESCORT_BONUS_MOD_ID);
                stats.getMaxSpeed().unmodify(ESCORT_BONUS_MOD_ID);
                for (StatBonus stat : rangeStats) {
                    stat.unmodify(ESCORT_BONUS_MOD_ID);
                }
                return;
            }

            float maneuverPercent = stats.getDynamic().getValue(MANEUVER_BONUS_KEY, 0f) * mag;
            Maneuverability.modifyPercent(stats, ESCORT_BONUS_MOD_ID, maneuverPercent);
            stats.getMaxSpeed().modifyPercent(ESCORT_BONUS_MOD_ID,
                    stats.getDynamic().getValue(SPEED_BONUS_KEY, 0f) * mag);

            float rangePercent = stats.getDynamic().getValue(WEAPON_RANGE_BONUS_KEY, 0f) * mag;
            for (StatBonus stat : rangeStats) {
                stat.modifyPercent(ESCORT_BONUS_MOD_ID, rangePercent);
            }
        }
    }
}
