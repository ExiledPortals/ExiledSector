package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
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
        public boolean reshapesDealtDamage() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    EXPLODE_ON_DEATH(DeathExplosionListener.FUEL_DAMAGE_PERCENT_KEY, DeathExplosionListener.class, DeathExplosionListener::new),
    DEATH_ON_COLLISION(CollisionDeathListener.class, CollisionDeathListener::new),
    NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT(EnergyChainListener.CHANCE_KEY, EnergyChainListener.class, EnergyChainListener::new) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT(EnergyChainListener.FALLOFF_KEY, EnergyChainListener.class, EnergyChainListener::new) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT(BallisticPierceListener.CHANCE_KEY, BallisticPierceListener.class, BallisticPierceListener::new),
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

    private static final class DeathExplosionListener extends ShipCombatListener implements HullDamageAboutToBeTakenListener {

        private static final String FUEL_DAMAGE_PERCENT_KEY = "exiledSector_explodeOnDeathFuelDamagePercent";

        private static final float RADIUS_MULT = 4f;
        private static final float MIN_RADIUS = 300f;
        private static final Color BLAST_COLOR = new Color(255, 200, 120, 40);
        private static final Color CORE_COLOR = new Color(255, 255, 255, 60);

        private boolean exploded;

        private DeathExplosionListener(ShipAPI ownerShip) {
            super(ownerShip);
        }

        // java:S3516: returning true would cancel the hull damage; this listener only reacts to the killing blow and never cancels it
        @SuppressWarnings("java:S3516")
        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI damagedShip, Vector2f point, float damageAmount) {
            if (exploded || damageAmount < damagedShip.getHitpoints()) {
                return false;
            }
            exploded = true;
            detonate();
            return false;
        }

        private void detonate() {
            CombatEngineAPI engine = Global.getCombatEngine();
            Vector2f loc = ownerShip.getLocation();
            float radius = Math.max(MIN_RADIUS, ownerShip.getCollisionRadius() * RADIUS_MULT);
            float damage = fuelDamage();

            List<ShipAPI> nearby = CombatQueries.shipsMatching(other -> other != ownerShip && !other.isHulk() && !other.isShuttlePod()
                    && CombatQueries.withinRadius(other.getLocation(), loc, radius));
            for (ShipAPI other : nearby) {
                float distance = Vector2f.sub(other.getLocation(), loc, null).length();
                float dealt = damage * (radius - distance) / radius;
                if (dealt <= 0f) {
                    continue;
                }
                engine.applyDamage(other, other.getLocation(), dealt, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ownerShip);
            }

            engine.spawnExplosion(loc, new Vector2f(), BLAST_COLOR, radius, 1.2f);
            engine.spawnExplosion(loc, new Vector2f(), CORE_COLOR, radius * 0.5f, 0.6f);
        }

        private float fuelDamage() {
            FleetMemberAPI member = ownerShip.getMutableStats().getFleetMember();
            if (member == null) {
                return 0f;
            }
            return member.getFuelCapacity() * magnitude(FUEL_DAMAGE_PERCENT_KEY) / 100f;
        }
    }

    private static final class CollisionDeathListener extends ShipCombatListener implements AdvanceableListener {

        private static final float COLLISION_CHECK_GRACE_PERIOD = 1.5f;
        private static final float BROAD_PHASE_MARGIN = 1.1f;
        private static final float LETHAL_DAMAGE = 999999f;

        private boolean triggered;
        private float aliveTime;

        private CollisionDeathListener(ShipAPI ownerShip) {
            super(ownerShip);
        }

        @Override
        public void advance(float amount) {
            if (triggered || !ownerIsAliveNotHulk()) {
                return;
            }
            aliveTime += amount;
            if (aliveTime < COLLISION_CHECK_GRACE_PERIOD || ownerShip.getCollisionClass() == CollisionClass.NONE) {
                return;
            }
            if (isHullTouchingAnotherShip()) {
                triggered = true;
                Global.getCombatEngine().applyDamage(ownerShip, ownerShip.getLocation(), LETHAL_DAMAGE,
                        DamageType.HIGH_EXPLOSIVE, 0f, true, false, ownerShip);
            }
        }

        private boolean isHullTouchingAnotherShip() {
            Vector2f loc = ownerShip.getLocation();
            float queryRadius = ownerShip.getCollisionRadius() * BROAD_PHASE_MARGIN;
            CombatEngineAPI engine = Global.getCombatEngine();
            return CombatQueries.anyNear(engine.getShipGrid(), loc, queryRadius,
                    candidate -> candidate instanceof ShipAPI other && isCollidableShip(other) && isTouching(other));
        }

        private boolean isCollidableShip(ShipAPI other) {
            return other != ownerShip && !other.isFighter() && !other.isHulk() && !other.isShuttlePod()
                    && other.getCollisionClass() != CollisionClass.NONE
                    && !isSameStation(other);
        }

        private boolean isSameStation(ShipAPI other) {
            ShipAPI parent = ownerShip.getParentStation();
            ShipAPI otherParent = other.getParentStation();
            return otherParent == ownerShip || parent == other || (parent != null && parent == otherParent);
        }

        private boolean isTouching(CombatEntityAPI other) {
            return isBroadPhaseNear(ownerShip.getLocation(), ownerShip.getCollisionRadius(), other) && hullsOverlap(ownerShip, other);
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

    private static final class EscortListener extends ShipCombatListener implements AdvanceableListener {

        private static final String MANEUVER_BONUS_KEY = "exiledSector_escortManeuverBonusPercent";
        private static final String SPEED_BONUS_KEY = "exiledSector_escortSpeedBonusPercent";
        private static final String WEAPON_RANGE_BONUS_KEY = "exiledSector_escortWeaponRangeBonusPercent";
        private static final String PROXIMITY_RANGE_KEY = "exiledSector_escortProximityRange";
        private static final ScaledBonus ESCORT_BONUS = new ScaledBonus(
                ScaledBonus.percent(Maneuverability.target(), MANEUVER_BONUS_KEY),
                ScaledBonus.percent(StatTarget.liveStat(MutableShipStatsAPI::getMaxSpeed), SPEED_BONUS_KEY),
                ScaledBonus.percent(StatTarget.all(StatTarget.liveBonus(MutableShipStatsAPI::getBallisticWeaponRangeBonus),
                        StatTarget.liveBonus(MutableShipStatsAPI::getEnergyWeaponRangeBonus)), WEAPON_RANGE_BONUS_KEY));

        private static final float PROXIMITY_FADE_DISTANCE = 500f;
        private static final float SHIELD_RADIUS_OVERLAP_MULT = 0.75f;
        private static final float DESTROYER_ESCORTING_CAPITAL_MULT = 2f;
        private static final float SEARCH_MARGIN = 300f;
        private static final float EASE_SECONDS = 0.5f;
        private static final float MAG_SNAP = 0.001f;
        private static final String ESCORT_BONUS_MOD_ID = "exiledSector_escortBonus";

        private final IntervalUtil retargetInterval = new IntervalUtil(0.9f, 1.1f);
        private float targetMag;
        private float appliedMag;
        private float easeRate;

        private EscortListener(ShipAPI ownerShip) {
            super(ownerShip);
        }

        @Override
        public void advance(float amount) {
            if (!ownerIsAliveNotHulk()) {
                return;
            }
            retargetInterval.advance(amount);
            boolean retargeted = retargetInterval.intervalElapsed();
            if (retargeted) {
                targetMag = proximityMagnitude();
                easeRate = Math.abs(targetMag - appliedMag) / EASE_SECONDS;
            }
            if (retargeted || appliedMag != targetMag) {
                appliedMag = approach(appliedMag, targetMag, easeRate * amount);
                ESCORT_BONUS.apply(ownerShip.getMutableStats(), ESCORT_BONUS_MOD_ID, appliedMag);
            }
        }

        private static float approach(float current, float target, float maxStep) {
            if (Math.abs(target - current) <= maxStep + MAG_SNAP) {
                return target;
            }
            return current + Math.signum(target - current) * maxStep;
        }

        private float proximityMagnitude() {
            float range = magnitude(PROXIMITY_RANGE_KEY);
            float searchRadius = range + PROXIMITY_FADE_DISTANCE + ownerShip.getCollisionRadius() + SEARCH_MARGIN;
            float best = 0f;
            for (ShipAPI escorted : CombatQueries.shipsNear(ownerShip.getLocation(), searchRadius, this::isLargerFriendly)) {
                best = Math.max(best, magnitudeFor(escorted, range));
            }
            return best;
        }

        private boolean isLargerFriendly(ShipAPI candidate) {
            return candidate != ownerShip && candidate.getOwner() == ownerShip.getOwner() && CombatQueries.isAliveNotHulk(candidate)
                    && candidate.getHullSize().ordinal() > ownerShip.getHullSize().ordinal();
        }

        private float magnitudeFor(ShipAPI escorted, float range) {
            float radiusOverlap = (ownerShip.getShieldRadiusEvenIfNoShield() + escorted.getShieldRadiusEvenIfNoShield())
                    * SHIELD_RADIUS_OVERLAP_MULT;
            float distance = Vector2f.sub(ownerShip.getShieldCenterEvenIfNoShield(),
                    escorted.getShieldCenterEvenIfNoShield(), null).length() - radiusOverlap;

            float mag;
            if (distance < range) {
                mag = 1f;
            } else if (distance < range + PROXIMITY_FADE_DISTANCE) {
                mag = 1f - (distance - range) / PROXIMITY_FADE_DISTANCE;
            } else {
                mag = 0f;
            }

            if (ownerShip.isDestroyer() && escorted.isCapital()) {
                mag *= DESTROYER_ESCORTING_CAPITAL_MULT;
            }
            return mag;
        }
    }
}
