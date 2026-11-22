package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class EmergencyVentingListener extends ShipCombatListener implements AdvanceableListener {

    static final String OVERLOAD_SECONDS_KEY = "exiledSector_emergencyVentingOverloadSeconds";
    static final String VENT_RATE_PERCENT_KEY = "exiledSector_emergencyVentRatePercent";
    static final float VENT_START_TIMEOUT_SECONDS = 0.5f;
    private static final String MOD_ID_PREFIX = "exiledSector_emergencyVenting_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/infernium_injector.png";

    private enum Stage { IDLE, OVERLOADED, AWAITING_VENT, VENTING }

    private final PlayerStatus status;
    private Stage stage = Stage.IDLE;
    private float awaitingSeconds;

    EmergencyVentingListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.emergencyVenting");
    }

    boolean ventBoosted() {
        return stage == Stage.VENTING;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            endVentBoost();
            return;
        }
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        switch (stage) {
            case IDLE -> shortenOverload(flux);
            case OVERLOADED -> {
                if (!flux.isOverloadedOrVenting()) {
                    ownerShip.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    awaitingSeconds = 0f;
                    stage = Stage.AWAITING_VENT;
                }
            }
            case AWAITING_VENT -> awaitVent(flux, amount);
            case VENTING -> {
                if (flux.isVenting()) {
                    showStatus();
                } else {
                    endVentBoost();
                }
            }
        }
    }

    private void shortenOverload(FluxTrackerAPI flux) {
        float maxSeconds = magnitude(OVERLOAD_SECONDS_KEY);
        if (maxSeconds <= 0f || !flux.isOverloaded() || flux.getOverloadTimeRemaining() < maxSeconds || !canVent()) {
            return;
        }
        flux.stopOverload();
        flux.beginOverloadWithTotalBaseDuration(maxSeconds);
        stage = Stage.OVERLOADED;
    }

    private void awaitVent(FluxTrackerAPI flux, float amount) {
        if (flux.isVenting()) {
            float ventPercent = magnitude(VENT_RATE_PERCENT_KEY);
            if (ventPercent > 0f) {
                ownerShip.getMutableStats().getVentRateMult().modifyPercent(modId, ventPercent);
            }
            stage = Stage.VENTING;
            return;
        }
        awaitingSeconds += amount;
        if (awaitingSeconds >= VENT_START_TIMEOUT_SECONDS) {
            stage = Stage.IDLE;
        }
    }

    private boolean canVent() {
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        return stats.getVentRateMult().getModifiedValue() > 0f;
    }

    private void endVentBoost() {
        ownerShip.getMutableStats().getVentRateMult().unmodify(modId);
        stage = Stage.IDLE;
    }

    private void showStatus() {
        float ventPercent = magnitude(VENT_RATE_PERCENT_KEY);
        if (ventPercent <= 0f) {
            return;
        }
        status.showIfPlayer(ownerShip, false, () -> Translation.msg("combat.emergencyVenting.status").arg("value", ventPercent).text());
    }
}
