package exiledsector.skills.skilleffect.readiness;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.StatMode;

import static exiledsector.skills.skilleffect.Backings.statEffect;
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
    CR_RECOVERY_RATE_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getBaseCRRecoveryRatePercentPerDay),
            "stat.combatReadinessRecoveryRate", false)),
    REPAIR_RATE_PER_DAY_PERCENT(statEffect(PERCENT, stat(MutableShipStatsAPI::getRepairRatePercentPerDay), "stat.repairRatePerDay", false)),
    CR_LOSS_PER_SECOND_PERCENT(statEffect(PERCENT, liveBonus(MutableShipStatsAPI::getCRLossPerSecondPercent),
            "stat.rateOfCombatReadinessLossFromExtendedDeployment", true)),
    CR_LOSS_PER_SECOND_MULT(statEffect(MULT, liveBonus(MutableShipStatsAPI::getCRLossPerSecondPercent),
            "stat.rateOfCombatReadinessLossFromExtendedDeployment", true)),
    PEAK_CR_DURATION_PERCENT(statEffect(PERCENT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration", false)),
    PEAK_CR_DURATION_MULT(statEffect(MULT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration", false));

    private final EffectBacking backing;

    ReadinessSkillEffect() {
        this(null);
    }

    ReadinessSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
