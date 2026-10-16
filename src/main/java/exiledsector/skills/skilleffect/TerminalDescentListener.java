package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;

final class TerminalDescentListener extends ShipCombatListener implements AdvanceableListener {

    static final String HULL_DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_terminalDescentHullDamageTakenPercent";
    static final String FLUX_DISSIPATION_PERCENT_KEY = "exiledSector_terminalDescentFluxDissipationPercent";
    static final String SYSTEM_REGEN_PERCENT_KEY = "exiledSector_terminalDescentSystemRegenPercent";
    static final String SHIELD_DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_terminalDescentShieldDamageTakenPercent";
    static final float ENTER_BELOW_HULL_FRACTION = 0.25f;
    static final float LEAVE_ABOVE_HULL_FRACTION = 0.4f;
    private static final String STATUS_ICON = "graphics/icons/hullsys/damper_field.png";
    private static final String MOD_ID_PREFIX = "exiledSector_terminalDescent_";

    private boolean inDescent;
    private boolean seenAboveEntryThreshold;

    TerminalDescentListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    boolean inDescent() {
        return inDescent;
    }

    static boolean nextDescentState(boolean inDescent, float hullFraction) {
        return inDescent ? hullFraction <= LEAVE_ABOVE_HULL_FRACTION : hullFraction < ENTER_BELOW_HULL_FRACTION;
    }

    @Override
    public void advance(float amount) {
        float maxHull = ownerShip.getMaxHitpoints();
        float hullFraction = maxHull <= 0f ? 1f : ownerShip.getHitpoints() / maxHull;
        if (hullFraction >= ENTER_BELOW_HULL_FRACTION) {
            seenAboveEntryThreshold = true;
        }
        boolean descending = ownerIsAliveNotHulk() && seenAboveEntryThreshold && nextDescentState(inDescent, hullFraction);
        if (descending != inDescent) {
            inDescent = descending;
            apply(descending);
        }
        if (inDescent) {
            ownerShip.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            showStatus();
        }
    }

    private void apply(boolean descending) {
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (!descending) {
            stats.getHullDamageTakenMult().unmodify(modId);
            stats.getFluxDissipation().unmodify(modId);
            stats.getSystemRegenBonus().unmodify(modId);
            stats.getShieldDamageTakenMult().unmodify(modId);
            stats.getPeakCRDuration().unmodify(modId);
            return;
        }
        stats.getHullDamageTakenMult().modifyPercent(modId, magnitude(HULL_DAMAGE_TAKEN_PERCENT_KEY));
        stats.getFluxDissipation().modifyPercent(modId, magnitude(FLUX_DISSIPATION_PERCENT_KEY));
        stats.getSystemRegenBonus().modifyPercent(modId, magnitude(SYSTEM_REGEN_PERCENT_KEY));
        stats.getShieldDamageTakenMult().modifyPercent(modId, magnitude(SHIELD_DAMAGE_TAKEN_PERCENT_KEY));
        stats.getPeakCRDuration().modifyMult(modId, 0f);
        if (ownerShip.getFluxTracker() != null && ownerShip.getFluxTracker().isVenting()) {
            ownerShip.getFluxTracker().stopVenting();
        }
    }

    private void showStatus() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && engine.getPlayerShip() == ownerShip) {
            engine.maintainStatusForPlayerShip(modId, STATUS_ICON, Translation.gameText("combat.terminalDescent"),
                    Translation.gameText("combat.terminalDescent.status"), false);
        }
    }
}
