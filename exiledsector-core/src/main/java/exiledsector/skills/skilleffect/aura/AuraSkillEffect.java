package exiledsector.skills.skilleffect.aura;

import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffectSupport;

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
            return EffectText.msg(this).arg("range", TerrifyingPresenceField.RANGE).arg("value", magnitude).styled();
        }
    },
    GRAVITY_DRAG_TIMEFLOW_PERCENT(listener(GravityDragListener.TIMEFLOW_PERCENT_KEY, GravityDragListener.class, GravityDragListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", GravityDragField.RANGE)
                    .arg("phased", magnitude * GravityDragListener.PHASED_MULT).arg("cap", GravityDragField.MAX_TOTAL_REDUCTION_PERCENT).styled();
        }
    },
    GRAVITY_DRAG_PHASED_TIMEFLOW_PERCENT(listener(GravityDragListener.PHASED_TIMEFLOW_PERCENT_KEY, GravityDragListener.class,
            GravityDragListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", GravityDragField.RANGE)
                    .arg("cap", GravityDragField.MAX_TOTAL_REDUCTION_PERCENT).styled();
        }
    },
    SMALLER_ALLY_DAMAGE_TAKEN_PERCENT(listener(AuxiliaryScreenListener.DAMAGE_TAKEN_PERCENT_KEY, AuxiliaryScreenListener.class,
            AuxiliaryScreenListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", AuxiliaryScreenField.RANGE).styled();
        }
    },
    MALFUNCTION_AURA_PERCENT(listener(MalfunctionAuraListener.MALFUNCTION_PERCENT_KEY, MalfunctionAuraListener.class, MalfunctionAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", MalfunctionAuraField.RANGE).styled();
        }
    },
    RATE_OF_FIRE_AURA_PERCENT(listener(RateOfFireAuraListener.RATE_OF_FIRE_PERCENT_KEY, RateOfFireAuraListener.class,
            RateOfFireAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", RateOfFireAuraField.RANGE).styled();
        }
    },
    ALLY_ARMOR_DAMAGE_TAKEN_PERCENT_PER_DMOD(listener(DModArmorAuraListener.PERCENT_PER_DMOD_KEY, DModArmorAuraListener.class,
            DModArmorAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", DModArmorAuraField.RANGE)
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
