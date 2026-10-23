package exiledlib.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledlib.compat.LostSectorCompat;
import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;

import static exiledlib.skills.skilleffect.StatMode.FLAT;
import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.dynamicMod;
import static exiledlib.skills.skilleffect.StatTarget.dynamicStat;

public enum ShipSystemSkillEffect implements BackedSkillEffect {

    SYSTEM_CHARGES_FLAT(FLAT, bonus(MutableShipStatsAPI::getSystemUsesBonus), "stat.systemCharges", false),
    SYSTEM_COOLDOWN_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getSystemCooldownBonus), "stat.shipSystemCooldown", true),
    SYSTEM_CHARGE_REGEN_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getSystemRegenBonus), "stat.shipSystemChargeRegeneration", false),
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
    },
    FRAGMENT_SWARM_SIZE_PERCENT(PERCENT, dynamicMod("fragment_swarm_size_mod"), "stat.fragmentSwarmSize", false),
    FRAGMENT_REPLACEMENT_RATE_PERCENT(PERCENT, dynamicStat("fragment_swarm_respawn_rate_mult"), "stat.fragmentReplacementRate", false);

    private final EffectBacking backing;

    ShipSystemSkillEffect() {
        this.backing = null;
    }

    ShipSystemSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this.backing = new SimpleStatEffect(mode, target, statKey, lowerIsBetter);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
