package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.compat.LostSectorCompat;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

public enum CompatSkillEffect implements SkillEffect {

    AUGMENTED_SYSTEM_REGEN_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            if (LostSectorCompat.hasAugmentedSystems(stats.getVariant())) {
                stats.getSystemRegenBonus().modifyPercent(modId, magnitude);
            } else {
                stats.getSystemRegenBonus().unmodify(modId);
            }
        }

        @Override
        public StyledText description(float magnitude) {
            return LostSectorCompat.isModEnabled() ? EffectText.msg(this).arg("value", magnitude).styled() : null;
        }

        @Override
        public boolean supportsTemporaryGating() {
            return false;
        }

        @Override
        public boolean lowerIsBetter() {
            return false;
        }
    },
    CONVERTED_HANGAR_REFIT_TIME_MULT(FighterSkillEffect.FIGHTER_REFIT_TIME_MULT, Stats.CONVERTED_HANGAR_NO_REFIT_PENALTY),
    CONVERTED_HANGAR_REPLACEMENT_RATE_MULT(FighterSkillEffect.FIGHTER_REPLACEMENT_RATE_MULT, Stats.CONVERTED_HANGAR_NO_REFIT_PENALTY),
    CONVERTED_HANGAR_RELAUNCH_TIME_FLAT(FighterSkillEffect.FIGHTER_RELAUNCH_TIME_FLAT, Stats.CONVERTED_HANGAR_NO_REARM_INCREASE),
    CONVERTED_HANGAR_MIN_CREW_FLAT(LogisticsSkillEffect.MIN_CREW_FLAT, Stats.CONVERTED_HANGAR_NO_CREW_INCREASE);

    private final SkillEffect penalty;
    private final String waiverStatId;

    CompatSkillEffect() {
        this(null, null);
    }

    CompatSkillEffect(SkillEffect penalty, String waiverStatId) {
        this.penalty = penalty;
        this.waiverStatId = waiverStatId;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        penalty.apply(stats, modId, isConvertedHangarPenaltyWaived(stats, waiverStatId) ? 0f : magnitude);
    }

    @Override
    public boolean supportsTemporaryGating() {
        return penalty.supportsTemporaryGating();
    }

    @Override
    public boolean lowerIsBetter() {
        return penalty.lowerIsBetter();
    }

    @Override
    public StyledText description(float magnitude) {
        StyledText text = penalty.description(magnitude);
        if (!SecondInCommandCompat.isModEnabled()) {
            return text;
        }
        StyledText waived = Translation.msg("compat.waived").arg("skill", Translation.text("compat.skill.reconfiguration")).styled();
        return Translation.msg("format.sentences").arg("a", text).arg("b", waived).styled();
    }

    private static boolean isConvertedHangarPenaltyWaived(MutableShipStatsAPI stats, String waiverStatId) {
        return stats.getDynamic().getMod(waiverStatId).computeEffective(0f) > 0f
                || SecondInCommandCompat.isSkillActive(stats.getFleetMember(), SecondInCommandCompat.RECONFIGURATION_SKILL_ID);
    }
}
