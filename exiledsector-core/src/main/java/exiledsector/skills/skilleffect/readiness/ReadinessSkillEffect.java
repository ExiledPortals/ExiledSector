package exiledsector.skills.skilleffect.readiness;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.SimpleStatEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.liveBonus;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum ReadinessSkillEffect implements BackedSkillEffect {

    MAX_COMBAT_READINESS_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getMaxCombatReadiness().modifyFlat(modId, magnitude / 100f, Translation.gameText("modifier.shipSkillTree"));
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.PERCENT.describeStat(magnitude, "stat.maximumCombatReadiness");
        }
    },
    CR_RECOVERY_RATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getBaseCRRecoveryRatePercentPerDay),
            "stat.combatReadinessRecoveryRate", false),
    REPAIR_RATE_PER_DAY_PERCENT(PERCENT, stat(MutableShipStatsAPI::getRepairRatePercentPerDay), "stat.repairRatePerDay", false),
    CR_LOSS_PER_SECOND_PERCENT(PERCENT, liveBonus(MutableShipStatsAPI::getCRLossPerSecondPercent),
            "stat.rateOfCombatReadinessLossFromExtendedDeployment", true),
    CR_LOSS_PER_SECOND_MULT(MULT, liveBonus(MutableShipStatsAPI::getCRLossPerSecondPercent),
            "stat.rateOfCombatReadinessLossFromExtendedDeployment", true),
    PEAK_CR_DURATION_PERCENT(PERCENT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration", false),
    PEAK_CR_DURATION_MULT(MULT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration", false);

    private final EffectBacking backing;

    ReadinessSkillEffect() {
        this.backing = null;
    }

    ReadinessSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this.backing = new SimpleStatEffect(mode, target, statKey, lowerIsBetter);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
