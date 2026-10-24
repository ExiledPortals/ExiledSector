package exiledlib.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exiledlib.compat.CombatGlow;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.special.LethalHitArbiter;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;

final class PhaseAnchorDiveListener implements LethalHitArbiter.Saver, AdvanceableListener {

    static final String CR_PENALTY_KEY = "exiledSector_phaseAnchorCrPenaltyPercent";
    private static final String DIVE_FLAG_KEY = "phaseAnchor_canDive";
    private static final CombatGlow.Burst VANISH_GLOW = new CombatGlow.Burst(null, 4f, 1f, 1f, 3f, 0.3f, 0.8f, true);

    private final ShipAPI ownerShip;
    private final String modId;
    private final FaderUtil diveFader = new FaderUtil(1f, 1f);
    private boolean diving;
    private float diveProgress;

    PhaseAnchorDiveListener(ShipAPI ownerShip, String modId) {
        this.ownerShip = ownerShip;
        this.modId = modId;
        LethalHitArbiter.addSaver(ownerShip, LethalHitArbiter.PHASE_ANCHOR_DIVE_PRIORITY, this);
    }

    @Override
    public boolean blocksHullDamage() {
        return diving;
    }

    @Override
    public boolean saveFromLethalHit(ShipAPI damagedShip, float damageAmount) {
        if (damagedShip.getPhaseCloak() == null) {
            return false;
        }

        Map<String, Object> customData = Global.getCombatEngine().getCustomData();
        if (customData.containsKey(DIVE_FLAG_KEY)) {
            return false;
        }

        FleetMemberAPI member = damagedShip.getFleetMember();
        float deployCost = member != null ? member.getDeployCost() : 0f;
        float crPenaltyMult = damagedShip.getMutableStats().getDynamic().getValue(CR_PENALTY_KEY, 0f) / 100f;
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
