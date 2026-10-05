package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

public enum MovementSkillEffect implements BackedSkillEffect {

    MANEUVERABILITY_PERCENT(PERCENT, Maneuverability.target(), "stat.maneuverability", false),
    TOP_SPEED_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false),
    TOP_SPEED_FLAT(FLAT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false),
    TOP_SPEED_MULT(MULT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false),
    ACCELERATION_FLAT(FLAT, liveStat(MutableShipStatsAPI::getAcceleration), "stat.acceleration", false),
    DECELERATION_FLAT(FLAT, liveStat(MutableShipStatsAPI::getDeceleration), "stat.deceleration", false);

    private final EffectBacking backing;

    MovementSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    MovementSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }

    private static final class StatNames {
        static final String TOP_SPEED = "stat.topSpeed";

        private StatNames() {
        }
    }
}
