package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.function.Predicate;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

final class HullLossLungeListener extends ShipCombatListener implements AdvanceableListener {

    static final String SPEED_PERCENT_KEY = "exiledSector_hullLossLungeSpeedPercent";
    static final float HULL_LOSS_PERCENT = 10f;
    static final float BURST_SECONDS = 4f;
    static final float COOLDOWN_SECONDS = 10f;
    private static final String MOD_ID_PREFIX = "exiledSector_hullLossLungeSpeed_";
    private static final ScaledBonus SPEED_BURST = new ScaledBonus(
            percent(liveStat(MutableShipStatsAPI::getMaxSpeed), SPEED_PERCENT_KEY),
            percent(Maneuverability.target(), SPEED_PERCENT_KEY));

    private final ScaledBonus.Tracker speedBurst = SPEED_BURST.tracker();
    private final Predicate<ShipAPI> mantleLunging;
    private float highestHullLevel = Float.NaN;
    private float burstSecondsLeft;
    private float cooldownSecondsLeft;

    HullLossLungeListener(ShipAPI ownerShip) {
        this(ownerShip, MantleLunge::isLunging);
    }

    HullLossLungeListener(ShipAPI ownerShip, Predicate<ShipAPI> mantleLunging) {
        super(ownerShip, MOD_ID_PREFIX);
        this.mantleLunging = mantleLunging;
    }

    boolean bursting() {
        return burstSecondsLeft > 0f;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            endBurst();
            return;
        }
        advanceBurst(amount);
        if (cooldownSecondsLeft > 0f) {
            cooldownSecondsLeft -= amount;
            return;
        }
        float hullLevel = ownerShip.getHullLevel();
        highestHullLevel = Float.isNaN(highestHullLevel) ? hullLevel : Math.max(highestHullLevel, hullLevel);
        if (hullLevel <= highestHullLevel - HULL_LOSS_PERCENT / 100f && !mantleLunging.test(ownerShip)) {
            startBurst(hullLevel);
        }
    }

    private void startBurst(float hullLevel) {
        highestHullLevel = hullLevel;
        burstSecondsLeft = BURST_SECONDS;
        cooldownSecondsLeft = COOLDOWN_SECONDS;
        speedBurst.apply(ownerShip.getMutableStats(), modId, 1f);
    }

    private void advanceBurst(float amount) {
        if (burstSecondsLeft <= 0f) {
            return;
        }
        burstSecondsLeft -= amount;
        if (burstSecondsLeft <= 0f) {
            endBurst();
        }
    }

    private void endBurst() {
        burstSecondsLeft = 0f;
        speedBurst.apply(ownerShip.getMutableStats(), modId, 0f);
    }
}
