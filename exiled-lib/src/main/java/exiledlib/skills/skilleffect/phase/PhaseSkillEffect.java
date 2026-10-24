package exiledlib.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.ConditionalStatEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.SkillEffectSupport;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;
import exiledlib.skills.skilleffect.weapon.WeaponScope;
import exiledlib.skills.skilleffect.weapon.WeaponStatFamily;

import static exiledlib.skills.skilleffect.StatMode.FLAT;
import static exiledlib.skills.skilleffect.StatMode.MULT;
import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.all;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.dynamicMod;
import static exiledlib.skills.skilleffect.StatTarget.dynamicStat;
import static exiledlib.skills.skilleffect.StatTarget.stat;

public enum PhaseSkillEffect implements BackedSkillEffect {

    PHASE_CLOAK_ACTIVATION_COST_MULT(MULT, bonus(MutableShipStatsAPI::getPhaseCloakActivationCostBonus),
            "stat.phaseCloakActivationCost", true),
    PHASE_CLOAK_UPKEEP_COST_MULT(MULT, bonus(MutableShipStatsAPI::getPhaseCloakUpkeepCostBonus),
            "stat.phaseCloakUpkeepCost", true),
    PHASE_CLOAK_COOLDOWN_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getPhaseCloakCooldownBonus),
            "stat.phaseCloakCooldown", true),
    PHASE_CLOAK_ACCELERATION_PERCENT(FLAT, dynamicMod(Stats.PHASE_CLOAK_ACCEL_MOD), "stat.accelerationWhilePhased", false) {
        @Override
        public StyledText description(float magnitude) {
            return PERCENT.describeStat(magnitude, "stat.accelerationWhilePhased");
        }
    },
    PHASE_TIMEFLOW_PERCENT(PERCENT, dynamicStat(Stats.PHASE_TIME_BONUS_MULT), "stat.phaseTimeFlowBonus", false),
    PHASE_CLOAK_FLUX_THRESHOLD_PERCENT(PERCENT, dynamicMod("phase_cloak_flux_level_for_min_speed_mod"),
            "stat.hardFluxLevelForMinimumPhaseSpeed", false),
    COMBAT_BOOST_WHILE_PHASED(new ConditionalStatEffect(MULT, all(stat(MutableShipStatsAPI::getFluxDissipation),
            WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL), WeaponStatFamily.AMMO_REGEN.target(WeaponScope.ALL)),
            "stat.fluxRateOfFireAndAmmoRegenWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
    PHASE_TOP_SPEED_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getMaxSpeed), "stat.topSpeedWhilePhased",
            PhaseSkillEffect::isBoostedByPhase)),
    FLUX_DISSIPATION_WHILE_PHASED_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getFluxDissipation),
            "stat.fluxDissipationWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
    AMMO_REGEN_WHILE_PHASED_PERCENT(new ConditionalStatEffect(PERCENT, WeaponStatFamily.AMMO_REGEN.target(WeaponScope.ALL),
            "stat.ammoRegenWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
    PHASE_ANCHOR_EMERGENCY_DIVE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(PhaseAnchorDiveListener.CR_PENALTY_KEY).modifyFlat(modId, magnitude);
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            SkillEffectSupport.ensureListener(ship, PhaseAnchorDiveListener.class, s -> new PhaseAnchorDiveListener(s, modId));
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return ship.phaseHull() ? null : Translation.text("node.block.phaseHull");
        }
    };

    private static boolean isBoostedByPhase(ShipAPI ship) {
        ShipSystemAPI phaseCloak = ship.getPhaseCloak();
        return ship.isPhased() && (phaseCloak == null || !phaseCloak.isChargedown());
    }

    private final EffectBacking backing;

    PhaseSkillEffect() {
        this((EffectBacking) null);
    }

    PhaseSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    PhaseSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
