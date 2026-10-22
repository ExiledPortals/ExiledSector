package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exiledlib.compat.CombatGlow;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipFacts;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;

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
            stat(MutableShipStatsAPI::getBallisticRoFMult), stat(MutableShipStatsAPI::getEnergyRoFMult),
            stat(MutableShipStatsAPI::getMissileRoFMult), stat(MutableShipStatsAPI::getBallisticAmmoRegenMult),
            stat(MutableShipStatsAPI::getEnergyAmmoRegenMult), stat(MutableShipStatsAPI::getMissileAmmoRegenMult)),
            "stat.fluxRateOfFireAndAmmoRegenWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
    PHASE_TOP_SPEED_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getMaxSpeed), "stat.topSpeedWhilePhased",
            PhaseSkillEffect::isBoostedByPhase)),
    FLUX_DISSIPATION_WHILE_PHASED_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getFluxDissipation),
            "stat.fluxDissipationWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
    AMMO_REGEN_WHILE_PHASED_PERCENT(new ConditionalStatEffect(PERCENT, all(stat(MutableShipStatsAPI::getBallisticAmmoRegenMult),
            stat(MutableShipStatsAPI::getEnergyAmmoRegenMult), stat(MutableShipStatsAPI::getMissileAmmoRegenMult)),
            "stat.ammoRegenWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
    PHASE_ANCHOR_EMERGENCY_DIVE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(PHASE_ANCHOR_CR_PENALTY_KEY).modifyFlat(modId, magnitude);
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

    private static final String PHASE_ANCHOR_CR_PENALTY_KEY = "exiledSector_phaseAnchorCrPenaltyPercent";

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

    private static final class PhaseAnchorDiveListener implements HullDamageAboutToBeTakenListener, AdvanceableListener {

        private static final String DIVE_FLAG_KEY = "phaseAnchor_canDive";
        private static final CombatGlow.Burst VANISH_GLOW = new CombatGlow.Burst(null, 4f, 1f, 1f, 3f, 0.3f, 0.8f, true);

        private final ShipAPI ownerShip;
        private final String modId;
        private final FaderUtil diveFader = new FaderUtil(1f, 1f);
        private boolean diving;
        private float diveProgress;

        private PhaseAnchorDiveListener(ShipAPI ownerShip, String modId) {
            this.ownerShip = ownerShip;
            this.modId = modId;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI damagedShip, Vector2f point, float damageAmount) {
            if (diving) {
                return true;
            }
            if (damageAmount < damagedShip.getHitpoints() || damagedShip.getPhaseCloak() == null) {
                return false;
            }

            Map<String, Object> customData = Global.getCombatEngine().getCustomData();
            if (customData.containsKey(DIVE_FLAG_KEY)) {
                return false;
            }

            FleetMemberAPI member = damagedShip.getFleetMember();
            float deployCost = member != null ? member.getDeployCost() : 0f;
            float crPenaltyMult = damagedShip.getMutableStats().getDynamic().getValue(PHASE_ANCHOR_CR_PENALTY_KEY, 0f) / 100f;
            float crCost = crPenaltyMult * deployCost;
            if (damagedShip.getCurrentCR() < crCost) {
                return false;
            }

            damagedShip.setHitpoints(1f);
            if (member != null) {
                member.getRepairTracker().applyCREvent(-crCost, Translation.gameText("modifier.emergencyPhaseDive"));
            }
            diving = true;
            customData.put(DIVE_FLAG_KEY, Boolean.TRUE);
            return true;
        }

        @Override
        public void advance(float amount) {
            if (!diving) {
                return;
            }
            ShipSystemAPI phaseCloak = ownerShip.getPhaseCloak();
            if (phaseCloak == null) {
                return;
            }

            Color effectColor = Misc.setAlpha(phaseCloak.getSpecAPI().getEffectColor2(), 255);
            effectColor = Misc.interpolateColor(effectColor, Color.white, 0.5f);

            if (diveProgress == 0f && ownerShip.getFluxTracker().showFloaty()) {
                float timeMult = ownerShip.getMutableStats().getTimeMult().getModifiedValue();
                Global.getCombatEngine().addFloatingTextAlways(ownerShip.getLocation(), Translation.gameText("combat.emergencyDive"),
                        NeuralLinkScript.getFloatySize(ownerShip), effectColor, ownerShip,
                        16f * timeMult, 3.2f / timeMult, 1f / timeMult, 0f, 0f, 1f);
            }

            diveFader.advance(amount);
            ownerShip.setRetreating(true, false);
            ownerShip.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);

            // multiplying (not dividing) by chargeUpDur matches vanilla's own phase-anchor dive timing exactly
            diveProgress += amount * phaseCloak.getChargeUpDur();
            float extraAlphaMult = ownerShip.getExtraAlphaMult();
            phaseCloak.forceState(ShipSystemAPI.SystemState.IN, Math.min(1f, Math.max(extraAlphaMult, diveProgress)));

            ownerShip.getMutableStats().getHullDamageTakenMult().modifyMult(modId, 0f);

            if (diveProgress < 1f) {
                return;
            }

            if (diveFader.isIdle()) {
                Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ownerShip.getLocation(), ownerShip.getVelocity());
                vanishGlow(effectColor);
            }
            diveFader.fadeOut();
            diveFader.advance(amount);
            float brightness = diveFader.getBrightness();
            ownerShip.setExtraAlphaMult2(brightness);

            float jitterAmount = ownerShip.getCollisionRadius() * 5f;
            ownerShip.setJitter(this, effectColor, brightness, 20, jitterAmount * (1f - brightness));

            if (diveFader.isFadedOut()) {
                ownerShip.getLocation().set(0f, -1000000f);
            }
        }

        private void vanishGlow(Color effectColor) {
            VANISH_GLOW.play(ownerShip.getLocation(), null, new Vector2f(), ownerShip.getCollisionRadius(), effectColor);
        }
    }
}
