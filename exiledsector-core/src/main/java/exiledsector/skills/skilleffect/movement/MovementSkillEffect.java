package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.PerDModMultEffect;
import exiledsector.skills.skilleffect.StatTarget;

import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

public enum MovementSkillEffect implements BackedSkillEffect {

    MANEUVERABILITY_PERCENT(statEffect(PERCENT, Maneuverability.target(), "stat.maneuverability", false)),
    MANEUVERABILITY_MULT(statEffect(MULT, Maneuverability.multTarget(), "stat.maneuverability", false)),
    TOP_SPEED_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false)),
    TOP_SPEED_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false)),
    TOP_SPEED_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false)),
    ACCELERATION_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getAcceleration), "stat.acceleration", false)),
    ACCELERATION_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getAcceleration), "stat.acceleration", false)),
    DECELERATION_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getDeceleration), "stat.deceleration", false)),
    TIMEFLOW_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getTimeMult), "stat.timeflow", false)),
    TIMEFLOW_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getTimeMult), "stat.timeflow", false)),
    TIMEFLOW_AT_TOP_SPEED_PERCENT(new ConditionalStatEffect(PERCENT, liveStat(MutableShipStatsAPI::getTimeMult), "stat.timeflowAtTopSpeed",
            MovementSkillEffect::isAtTopSpeed)),
    SHIP_MASS_PERCENT(statEffect(PERCENT, new StatTarget.ShipMass(), "stat.shipMass", false)),
    TOP_SPEED_MULT_PER_DMOD(new PerDModMultEffect(liveStat(MutableShipStatsAPI::getMaxSpeed), "stat.topSpeedPerDMod", false));

    static final float TOP_SPEED_SHARE = 0.95f;

    private final EffectBacking backing;

    MovementSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    static boolean isAtTopSpeed(ShipAPI ship) {
        float topSpeed = ship.getMaxSpeed() * TOP_SPEED_SHARE;
        return topSpeed > 0f && ship.getVelocity().lengthSquared() >= topSpeed * topSpeed;
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
