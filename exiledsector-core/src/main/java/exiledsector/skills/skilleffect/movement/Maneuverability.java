package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.skills.skilleffect.StatTarget;

public final class Maneuverability {

    static final float TURN_ACCELERATION_FACTOR = 2f;

    private Maneuverability() {
    }

    public static StatTarget target() {
        return StatTarget.all(
                StatTarget.liveStat(MutableShipStatsAPI::getAcceleration),
                StatTarget.liveStat(MutableShipStatsAPI::getDeceleration),
                StatTarget.scaled(StatTarget.liveStat(MutableShipStatsAPI::getTurnAcceleration), TURN_ACCELERATION_FACTOR),
                StatTarget.liveStat(MutableShipStatsAPI::getMaxTurnRate));
    }
}
