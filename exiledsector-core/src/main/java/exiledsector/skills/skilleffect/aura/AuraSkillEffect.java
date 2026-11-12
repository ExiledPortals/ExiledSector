package exiledsector.skills.skilleffect.aura;

import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.DescribeContext;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import exiledsector.skills.skilleffect.weapon.PirateFrenzy;

import java.util.Set;

import static exiledsector.skills.skilleffect.Backings.listener;

public enum AuraSkillEffect implements BackedSkillEffect {

    ESCORT_MANEUVER_BONUS_PERCENT(listener(EscortListener.MANEUVER_BONUS_KEY, EscortListener.class, EscortListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return escortDescription(this, magnitude);
        }
    },
    ESCORT_SPEED_BONUS_PERCENT(listener(EscortListener.SPEED_BONUS_KEY, EscortListener.class, EscortListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return escortDescription(this, magnitude);
        }
    },
    ESCORT_WEAPON_RANGE_BONUS_PERCENT(listener(EscortListener.WEAPON_RANGE_BONUS_KEY, EscortListener.class, EscortListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return escortDescription(this, magnitude);
        }
    },
    ESCORT_SHIELD_AND_ARMOR_DAMAGE_TAKEN_PERCENT(listener(EscortListener.DAMAGE_TAKEN_KEY, EscortListener.class, EscortListener::new)) {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return escortDescription(this, -magnitude);
        }
    },
    TOP_SPEED_PERCENT_PER_NEARBY_ALLY(listener(FormationSpeedListener.SPEED_PERCENT_PER_ALLY_KEY, FormationSpeedListener.class,
            FormationSpeedListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", FormationSpeedListener.RANGE)
                    .arg("max", FormationSpeedListener.MAX_SPEED_PERCENT).styled();
        }
    },
    TERRIFYING_PRESENCE_ACCURACY_PENALTY_PERCENT(listener(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY,
            TerrifyingPresenceListener.class, TerrifyingPresenceListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return description(magnitude, DescribeContext.NONE);
        }

        @Override
        public StyledText description(float magnitude, DescribeContext context) {
            return EffectText.msg(this).arg("range", TerrifyingPresenceListener.range(context.magnitudeOf(TERRIFYING_PRESENCE_RANGE_PERCENT)))
                    .arg("value", magnitude).styled();
        }
    },
    TERRIFYING_PRESENCE_RANGE_PERCENT(listener(TerrifyingPresenceListener.RANGE_PERCENT_KEY,
            TerrifyingPresenceListener.class, TerrifyingPresenceListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }

        @Override
        public Set<SkillEffect> refines() {
            return Set.of(TERRIFYING_PRESENCE_ACCURACY_PENALTY_PERCENT);
        }
    },
    GRAVITY_DRAG_TIMEFLOW_MULT(listener(GravityDragListener.TIMEFLOW_PERCENT_KEY, GravityDragListener.class, GravityDragListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", GravityDragField.RANGE)
                    .arg("phased", magnitude * GravityDragListener.PHASED_MULT).arg("cap", GravityDragField.MAX_TOTAL_REDUCTION_PERCENT).styled();
        }
    },
    GRAVITY_DRAG_PHASED_TIMEFLOW_MULT(listener(GravityDragListener.PHASED_TIMEFLOW_PERCENT_KEY, GravityDragListener.class,
            GravityDragListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", GravityDragField.RANGE)
                    .arg("cap", GravityDragField.MAX_TOTAL_REDUCTION_PERCENT).styled();
        }
    },
    SMALLER_ALLY_DAMAGE_TAKEN_MULT(listener(AuxiliaryScreenListener.DAMAGE_TAKEN_PERCENT_KEY, AuxiliaryScreenListener.class,
            AuxiliaryScreenListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return description(magnitude, DescribeContext.NONE);
        }

        @Override
        public StyledText description(float magnitude, DescribeContext context) {
            return EffectText.msg(this).arg("value", magnitude)
                    .arg("range", AuxiliaryScreenListener.range(context.magnitudeOf(SMALLER_ALLY_DAMAGE_TAKEN_RANGE_FLAT))).styled();
        }
    },
    SMALLER_ALLY_DAMAGE_TAKEN_RANGE_FLAT(listener(AuxiliaryScreenListener.RANGE_FLAT_KEY, AuxiliaryScreenListener.class,
            AuxiliaryScreenListener::new)) {
        @Override
        public Set<SkillEffect> refines() {
            return Set.of(SMALLER_ALLY_DAMAGE_TAKEN_MULT);
        }
    },
    MALFUNCTION_AURA_PERCENT(listener(MalfunctionAuraListener.MALFUNCTION_PERCENT_KEY, MalfunctionAuraListener.class, MalfunctionAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", MalfunctionAuraField.RANGE).styled();
        }
    },
    RATE_OF_FIRE_AURA_MULT(listener(RateOfFireAuraListener.RATE_OF_FIRE_PERCENT_KEY, RateOfFireAuraListener.class,
            RateOfFireAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", RateOfFireAuraField.RANGE).styled();
        }
    },
    ALLY_ARMOR_DAMAGE_TAKEN_PERCENT_PER_DMOD(listener(DModAuraListener.ARMOR_PERCENT_PER_DMOD_KEY, DModAuraListener.class,
            DModAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", DModAuraField.RANGE)
                    .arg("max", SkillEffectSupport.MAX_COUNTED_DMODS).styled();
        }
    },
    ALLY_TOP_SPEED_PERCENT_PER_DMOD(listener(DModAuraListener.TOP_SPEED_PERCENT_PER_DMOD_KEY, DModAuraListener.class,
            DModAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", DModAuraField.RANGE)
                    .arg("max", SkillEffectSupport.MAX_COUNTED_DMODS).styled();
        }
    },
    REPAIR_COORDINATOR_ARMOR_PERCENT(listener(RepairCoordinatorListener.ARMOR_REPAIR_PERCENT_KEY, RepairCoordinatorListener.class,
            RepairCoordinatorListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", RepairCoordinatorField.RANGE)
                    .arg("perShip", RepairCoordinatorField.MAX_REPAIR_PERCENT_PER_COORDINATOR)
                    .arg("limit", RepairCoordinatorField.MAX_REPAIR_PERCENT_OF_ARMOR).styled();
        }
    },
    NEARBY_DESTRUCTION_REPLACEMENT_RATE_PERCENT(listener(WreckReclaimListener.RESTORE_PERCENT_KEY, WreckReclaimListener.class,
            WreckReclaimListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", WreckReclaimListener.RANGE).styled();
        }
    },
    NEARBY_DESTRUCTION_ARMOR_REPAIR_CAP_PERCENT(listener(WreckPlatingListener.REPAIR_CAP_PERCENT_KEY, WreckPlatingListener.class,
            WreckPlatingListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", WreckPlatingListener.RANGE)
                    .arg("percent", WreckPlatingListener.REPAIR_PERCENT_PER_WRECK).styled();
        }
    },
    PIRATE_FRENZY_ON_NEARBY_DESTRUCTION_RANGE_FLAT(listener(FrenzyOnNearbyDestructionListener.RANGE_KEY,
            FrenzyOnNearbyDestructionListener.class, FrenzyOnNearbyDestructionListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return description(magnitude, DescribeContext.NONE);
        }

        @Override
        public StyledText description(float magnitude, DescribeContext context) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", PirateFrenzy.stackSeconds(context))
                    .arg("stacks", PirateFrenzy.MAX_STACKS).styled().append("\n").append(PirateFrenzy.describeStackBonuses());
        }
    },
    CREW_STEAL_RANGE_FLAT(listener(CrewStealListener.RANGE_KEY, CrewStealListener.class, CrewStealListener::new)),
    CREW_STEAL_SKELETON_CREW_PERCENT(listener(CrewStealListener.SKELETON_CREW_PERCENT_KEY, CrewStealListener.class, CrewStealListener::new));

    private static StyledText escortDescription(AuraSkillEffect effect, float magnitude) {
        return EffectText.msg(effect).arg("range", EscortListener.ESCORT_RANGE).arg("fade", EscortListener.PROXIMITY_FADE_DISTANCE)
                .arg("value", magnitude).styled();
    }

    private final EffectBacking backing;

    AuraSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
