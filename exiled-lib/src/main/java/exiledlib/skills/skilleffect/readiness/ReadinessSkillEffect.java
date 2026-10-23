package exiledlib.skills.skilleffect.readiness;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;

import static exiledlib.skills.skilleffect.StatMode.MULT;
import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.liveBonus;
import static exiledlib.skills.skilleffect.StatTarget.stat;

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
