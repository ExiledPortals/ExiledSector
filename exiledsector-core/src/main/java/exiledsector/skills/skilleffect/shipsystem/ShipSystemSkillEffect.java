package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.DescribeContext;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.Set;

import static exiledsector.skills.skilleffect.Backings.listener;
import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicMod;
import static exiledsector.skills.skilleffect.StatTarget.dynamicStat;

public enum ShipSystemSkillEffect implements BackedSkillEffect {

    SYSTEM_CHARGES_FLAT(statEffect(FLAT, bonus(MutableShipStatsAPI::getSystemUsesBonus), "stat.systemCharges", false)),
    SYSTEM_COOLDOWN_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getSystemCooldownBonus), "stat.shipSystemCooldown", true)),
    SYSTEM_CHARGE_REGEN_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getSystemRegenBonus), "stat.shipSystemChargeRegeneration", false)),
    SYSTEM_ACTIVATION_TIMEFLOW_PERCENT(listener(TemporalExhaustListener.TIMEFLOW_PERCENT_KEY, TemporalExhaustListener.class,
            TemporalExhaustListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return description(magnitude, DescribeContext.NONE);
        }

        @Override
        public StyledText description(float magnitude, DescribeContext context) {
            return EffectText.msg(this).arg("value", magnitude)
                    .arg("seconds", TemporalExhaustListener.taperSeconds(context.magnitudeOf(SYSTEM_ACTIVATION_TIMEFLOW_TAPER_SECONDS_FLAT)))
                    .arg("damage", TemporalExhaustListener.RIFT_DAMAGE).arg("cooldown", TemporalExhaustListener.COOLDOWN_SECONDS).styled();
        }
    },
    SYSTEM_ACTIVATION_TIMEFLOW_TAPER_SECONDS_FLAT(listener(TemporalExhaustListener.ADDITIONAL_TAPER_SECONDS_KEY, TemporalExhaustListener.class,
            TemporalExhaustListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }

        @Override
        public Set<SkillEffect> refines() {
            return Set.of(SYSTEM_ACTIVATION_TIMEFLOW_PERCENT);
        }
    },
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
            return LostSectorCompat.isModEnabled() ? EffectText.templated(this, magnitude) : null;
        }
    },
    FRAGMENT_SWARM_SIZE_PERCENT(statEffect(PERCENT, dynamicMod("fragment_swarm_size_mod"), "stat.fragmentSwarmSize", false)),
    FRAGMENT_REPLACEMENT_RATE_PERCENT(statEffect(PERCENT, dynamicStat("fragment_swarm_respawn_rate_mult"), "stat.fragmentReplacementRate", false));

    private final EffectBacking backing;

    ShipSystemSkillEffect() {
        this(null);
    }

    ShipSystemSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
