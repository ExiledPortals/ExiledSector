package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipFacts;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;

import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicMod;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum PhaseSkillEffect implements BackedSkillEffect {

    PHASE_CLOAK_ACTIVATION_COST_MULT(MULT, bonus(MutableShipStatsAPI::getPhaseCloakActivationCostBonus),
            "stat.phaseCloakActivationCost", true),
    PHASE_CLOAK_FLUX_THRESHOLD_PERCENT(PERCENT, dynamicMod("phase_cloak_flux_level_for_min_speed_mod"),
            "stat.hardFluxLevelForMinimumPhaseSpeed", false),
    COMBAT_BOOST_WHILE_PHASED(new ConditionalStatEffect(MULT, all(stat(MutableShipStatsAPI::getFluxDissipation),
            stat(MutableShipStatsAPI::getBallisticRoFMult), stat(MutableShipStatsAPI::getEnergyRoFMult),
            stat(MutableShipStatsAPI::getMissileRoFMult), stat(MutableShipStatsAPI::getBallisticAmmoRegenMult),
            stat(MutableShipStatsAPI::getEnergyAmmoRegenMult), stat(MutableShipStatsAPI::getMissileAmmoRegenMult)),
            "stat.fluxRateOfFireAndAmmoRegenWhilePhased", PhaseSkillEffect::isBoostedByPhase)),
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
            return ship.phaseHull() ? null : "Requires a phase hull.";
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

        private final ShipAPI ship;
        private final String modId;
        private final FaderUtil diveFader = new FaderUtil(1f, 1f);
        private boolean diving;
        private float diveProgress;

        private PhaseAnchorDiveListener(ShipAPI ship, String modId) {
            this.ship = ship;
            this.modId = modId;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (diving) {
                return true;
            }
            if (damageAmount < ship.getHitpoints() || ship.getPhaseCloak() == null) {
                return false;
            }

            Map<String, Object> customData = Global.getCombatEngine().getCustomData();
            if (customData.containsKey(DIVE_FLAG_KEY)) {
                return false;
            }

            FleetMemberAPI member = ship.getFleetMember();
            float deployCost = member != null ? member.getDeployCost() : 0f;
            float crPenaltyMult = ship.getMutableStats().getDynamic().getValue(PHASE_ANCHOR_CR_PENALTY_KEY, 0f) / 100f;
            float crCost = crPenaltyMult * deployCost;
            if (ship.getCurrentCR() < crCost) {
                return false;
            }

            ship.setHitpoints(1f);
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
            ShipSystemAPI phaseCloak = ship.getPhaseCloak();
            if (phaseCloak == null) {
                return;
            }

            Color effectColor = Misc.setAlpha(phaseCloak.getSpecAPI().getEffectColor2(), 255);
            effectColor = Misc.interpolateColor(effectColor, Color.white, 0.5f);

            if (diveProgress == 0f && ship.getFluxTracker().showFloaty()) {
                float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(), Translation.gameText("combat.emergencyDive"),
                        NeuralLinkScript.getFloatySize(ship), effectColor, ship,
                        16f * timeMult, 3.2f / timeMult, 1f / timeMult, 0f, 0f, 1f);
            }

            diveFader.advance(amount);
            ship.setRetreating(true, false);
            ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);

            // multiplying (not dividing) by chargeUpDur matches vanilla's own phase-anchor dive timing exactly
            diveProgress += amount * phaseCloak.getChargeUpDur();
            float extraAlphaMult = ship.getExtraAlphaMult();
            phaseCloak.forceState(ShipSystemAPI.SystemState.IN, Math.min(1f, Math.max(extraAlphaMult, diveProgress)));

            ship.getMutableStats().getHullDamageTakenMult().modifyMult(modId, 0f);

            if (diveProgress < 1f) {
                return;
            }

            if (diveFader.isIdle()) {
                Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
            }
            diveFader.fadeOut();
            diveFader.advance(amount);
            float brightness = diveFader.getBrightness();
            ship.setExtraAlphaMult2(brightness);

            float jitterAmount = ship.getCollisionRadius() * 5f;
            ship.setJitter(this, effectColor, brightness, 20, jitterAmount * (1f - brightness));

            if (diveFader.isFadedOut()) {
                ship.getLocation().set(0f, -1000000f);
            }
        }
    }
}
