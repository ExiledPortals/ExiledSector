package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

final class Maneuverability {

    static final float TURN_ACCELERATION_FACTOR = 2f;

    private Maneuverability() {
    }

    static StatTarget target() {
        return StatTarget.all(
                StatTarget.liveStat(MutableShipStatsAPI::getAcceleration),
                StatTarget.liveStat(MutableShipStatsAPI::getDeceleration),
                StatTarget.scaled(StatTarget.liveStat(MutableShipStatsAPI::getTurnAcceleration), TURN_ACCELERATION_FACTOR),
                StatTarget.liveStat(MutableShipStatsAPI::getMaxTurnRate));
    }
}
