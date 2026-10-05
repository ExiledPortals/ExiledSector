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

    static void modifyPercent(MutableShipStatsAPI stats, String modId, float percent) {
        stats.getAcceleration().modifyPercent(modId, percent);
        stats.getDeceleration().modifyPercent(modId, percent);
        stats.getTurnAcceleration().modifyPercent(modId, percent * TURN_ACCELERATION_FACTOR);
        stats.getMaxTurnRate().modifyPercent(modId, percent);
    }

    static void unmodify(MutableShipStatsAPI stats, String modId) {
        stats.getAcceleration().unmodify(modId);
        stats.getDeceleration().unmodify(modId);
        stats.getTurnAcceleration().unmodify(modId);
        stats.getMaxTurnRate().unmodify(modId);
    }
}
