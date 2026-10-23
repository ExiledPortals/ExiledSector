package exiledlib.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.PlayerStatus;
import exiledlib.skills.skilleffect.ScaledBonus;
import exiledlib.skills.skilleffect.ShipCombatListener;
import exiledlib.skills.skilleffect.StatMode;

import static exiledlib.skills.skilleffect.ScaledBonus.fixed;
import static exiledlib.skills.skilleffect.ScaledBonus.percent;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.stat;

final class TerminalDescentListener extends ShipCombatListener implements AdvanceableListener {

    static final String HULL_DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_terminalDescentHullDamageTakenPercent";
    static final String FLUX_DISSIPATION_PERCENT_KEY = "exiledSector_terminalDescentFluxDissipationPercent";
    static final String SYSTEM_REGEN_PERCENT_KEY = "exiledSector_terminalDescentSystemRegenPercent";
    static final String SHIELD_DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_terminalDescentShieldDamageTakenPercent";
    static final float ENTER_BELOW_HULL_FRACTION = 0.25f;
    static final float LEAVE_ABOVE_HULL_FRACTION = 0.4f;
    private static final String STATUS_ICON = "graphics/icons/hullsys/damper_field.png";
    private static final String MOD_ID_PREFIX = "exiledSector_terminalDescent_";
    private static final ScaledBonus DESCENT_STATS = new ScaledBonus(
            percent(stat(MutableShipStatsAPI::getHullDamageTakenMult), HULL_DAMAGE_TAKEN_PERCENT_KEY),
            percent(stat(MutableShipStatsAPI::getFluxDissipation), FLUX_DISSIPATION_PERCENT_KEY),
            percent(bonus(MutableShipStatsAPI::getSystemRegenBonus), SYSTEM_REGEN_PERCENT_KEY),
            percent(stat(MutableShipStatsAPI::getShieldDamageTakenMult), SHIELD_DAMAGE_TAKEN_PERCENT_KEY),
            fixed(bonus(MutableShipStatsAPI::getPeakCRDuration), StatMode.MULT, -100f));

    private final PlayerStatus status;

    private boolean inDescent;
    private boolean seenAboveEntryThreshold;

    TerminalDescentListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.terminalDescent");
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
        DESCENT_STATS.apply(ownerShip.getMutableStats(), modId, descending ? 1f : 0f);
        if (!descending) {
            return;
        }
        if (ownerShip.getFluxTracker() != null && ownerShip.getFluxTracker().isVenting()) {
            ownerShip.getFluxTracker().stopVenting();
        }
    }

    private void showStatus() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && engine.getPlayerShip() == ownerShip) {
            if (status.needsText()) {
                status.setText(() -> Translation.text("combat.terminalDescent.status"));
            }
            status.show(engine, false);
        }
    }
}
