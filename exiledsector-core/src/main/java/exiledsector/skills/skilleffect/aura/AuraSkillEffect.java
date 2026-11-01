package exiledsector.skills.skilleffect.aura;

import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;

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
    MALFUNCTION_AURA_PERCENT(listener(MalfunctionAuraListener.MALFUNCTION_PERCENT_KEY, MalfunctionAuraListener.class, MalfunctionAuraListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", MalfunctionAuraField.RANGE).styled();
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
