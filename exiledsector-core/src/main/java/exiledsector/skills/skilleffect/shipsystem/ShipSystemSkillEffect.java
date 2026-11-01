package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;

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
