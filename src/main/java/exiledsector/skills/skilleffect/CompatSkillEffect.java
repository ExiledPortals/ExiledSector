package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.HullModNames;

public enum CompatSkillEffect implements SkillEffect {

    COUNTS_AS_SHIELD_SHUNT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            String id = synergyId(modId, SecondInCommandCompat.REDISTRIBUTION_SKILL_ID);
            if (!skillSeesTheHullMod(stats, HullMods.SHIELD_SHUNT) && isSkillActive(stats, SecondInCommandCompat.REDISTRIBUTION_SKILL_ID)) {
                stats.getFluxDissipation().modifyPercent(id, 5f);
                stats.getArmorBonus().modifyPercent(id, 10f);
                stats.getEmpDamageTakenMult().modifyMult(id, 0.75f);
            } else {
                stats.getFluxDissipation().unmodify(id);
                stats.getArmorBonus().unmodify(id);
                stats.getEmpDamageTakenMult().unmodify(id);
            }
        }

        @Override
        public StyledText description(float magnitude) {
            return synergyDescription("shield_shunt", "compat.skill.redistribution");
        }
    },
    COUNTS_AS_SAFETY_OVERRIDES {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            String id = synergyId(modId, SecondInCommandCompat.ENHANCED_OVERRIDES_SKILL_ID);
            if (!skillSeesTheHullMod(stats, HullMods.SAFETYOVERRIDES) && isSkillActive(stats, SecondInCommandCompat.ENHANCED_OVERRIDES_SKILL_ID)) {
                stats.getPeakCRDuration().modifyPercent(id, 25f);
                stats.getWeaponRangeThreshold().modifyFlat(id, 100f);
            } else {
                stats.getPeakCRDuration().unmodify(id);
                stats.getWeaponRangeThreshold().unmodify(id);
            }
        }

        @Override
        public StyledText description(float magnitude) {
            return synergyDescription("safetyoverrides", "compat.skill.enhancedOverrides");
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
        return penalty != null && penalty.supportsTemporaryGating();
    }

    @Override
    public boolean lowerIsBetter() {
        return penalty != null && penalty.lowerIsBetter();
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
                || isSkillActive(stats, SecondInCommandCompat.RECONFIGURATION_SKILL_ID);
    }

    private static boolean skillSeesTheHullMod(MutableShipStatsAPI stats, String hullModId) {
        return stats.getVariant() != null && stats.getVariant().hasHullMod(hullModId);
    }

    private static boolean isSkillActive(MutableShipStatsAPI stats, String skillId) {
        return SecondInCommandCompat.isSkillActive(stats.getFleetMember(), skillId);
    }

    private static String synergyId(String modId, String skillId) {
        return modId + "_" + skillId;
    }

    private static StyledText synergyDescription(String hullModId, String skillKey) {
        if (!SecondInCommandCompat.isModEnabled()) {
            return null;
        }
        return Translation.msg("compat.countsAs").arg("hullmod", HullModNames.displayName(hullModId))
                .arg("skill", Translation.text(skillKey)).styled();
    }
}
