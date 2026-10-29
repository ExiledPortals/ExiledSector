package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

public record WaivedPenalty(SkillEffect penalty, String waiverStatId) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        penalty.apply(stats, modId, isWaived(stats) ? 0f : magnitude);
    }

    @Override
    public StatMode mode() {
        return penalty.statMode();
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
    public StyledText description(SkillEffect effect, float magnitude) {
        StyledText text = penalty.description(magnitude);
        if (!SecondInCommandCompat.isModEnabled()) {
            return text;
        }
        StyledText waived = Translation.msg("compat.waived").arg("skill", Translation.text("compat.skill.reconfiguration")).styled();
        return Translation.msg("format.sentences").arg("a", text).arg("b", waived).styled();
    }

    private boolean isWaived(MutableShipStatsAPI stats) {
        return stats.getDynamic().getMod(waiverStatId).computeEffective(0f) > 0f
                || SecondInCommandCompat.isSkillActive(stats.getFleetMember(), SecondInCommandCompat.RECONFIGURATION_SKILL_ID);
    }
}
