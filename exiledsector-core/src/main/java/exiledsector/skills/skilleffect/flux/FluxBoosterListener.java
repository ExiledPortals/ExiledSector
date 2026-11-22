package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatTarget;

import java.awt.Color;

final class FluxBoosterListener extends ShipCombatListener implements AdvanceableListener {

    static final String MANEUVERABILITY_PERCENT_KEY = "exiledSector_fluxBoosterManeuverabilityPercent";
    static final String SECONDS_KEY = "exiledSector_fluxBoosterSeconds";
    static final float ACCELERATION_SHARE = 2f;
    private static final String MOD_ID_PREFIX = "exiledSector_fluxBooster_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/flare_launcher.png";
    private static final Color ENGINE_COLOR = new Color(153, 12, 184, 255);
    private static final ScaledBonus BOOST = new ScaledBonus(
            ScaledBonus.percent(StatTarget.stat(MutableShipStatsAPI::getMaxSpeed), MANEUVERABILITY_PERCENT_KEY),
            ScaledBonus.percent(StatTarget.stat(MutableShipStatsAPI::getMaxTurnRate), MANEUVERABILITY_PERCENT_KEY),
            ScaledBonus.percent(StatTarget.scaled(StatTarget.stat(MutableShipStatsAPI::getAcceleration), ACCELERATION_SHARE),
                    MANEUVERABILITY_PERCENT_KEY),
            ScaledBonus.percent(StatTarget.scaled(StatTarget.stat(MutableShipStatsAPI::getDeceleration), ACCELERATION_SHARE),
                    MANEUVERABILITY_PERCENT_KEY),
            ScaledBonus.percent(StatTarget.scaled(StatTarget.stat(MutableShipStatsAPI::getTurnAcceleration), ACCELERATION_SHARE),
                    MANEUVERABILITY_PERCENT_KEY));

    private final ScaledBonus.Tracker boost = BOOST.tracker();
    private final PlayerStatus status;
    private float ventStartFluxLevel;
    private float secondsLeft;
    private int shownSeconds = -1;

    FluxBoosterListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.fluxBooster");
    }

    boolean boosting() {
        return boost.appliedScale() > 0f;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            boost.apply(ownerShip.getMutableStats(), modId, 0f);
            return;
        }
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        if (!flux.isVenting()) {
            ventStartFluxLevel = flux.getFluxLevel();
            secondsLeft = magnitude(SECONDS_KEY);
            boost.apply(ownerShip.getMutableStats(), modId, 0f);
            return;
        }
        if (flux.getCurrFlux() <= 0f || secondsLeft <= 0f) {
            boost.apply(ownerShip.getMutableStats(), modId, 0f);
            return;
        }
        secondsLeft -= amount;
        boost.apply(ownerShip.getMutableStats(), modId, ventStartFluxLevel);
        ownerShip.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, 0.75f, 0.75f);
        ownerShip.getEngineController().extendFlame(this, 1f, 0f, 0.2f);
        showStatus();
    }

    private void showStatus() {
        int seconds = (int) Math.ceil(Math.max(0f, secondsLeft));
        if (seconds != shownSeconds) {
            shownSeconds = seconds;
            status.invalidate();
        }
        status.showIfPlayer(ownerShip, false, () -> Translation.msg("combat.fluxBooster.status").arg("seconds", seconds).text());
    }
}
