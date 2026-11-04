package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.PerDModMultEffect;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.TopSpeedShare;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

import static exiledsector.skills.skilleffect.Backings.listener;
import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum MovementSkillEffect implements BackedSkillEffect {

    MANEUVERABILITY_PERCENT(statEffect(PERCENT, Maneuverability.target(), "stat.maneuverability", false)),
    MANEUVERABILITY_MULT(statEffect(MULT, Maneuverability.multTarget(), "stat.maneuverability", false)),
    TOP_SPEED_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false)),
    TOP_SPEED_FLAT(statEffect(FLAT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false)),
    TOP_SPEED_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED, false)),
    TOP_SPEED_WHILE_SWARM_FULL_PERCENT(new ConditionalStatEffect(PERCENT, stat(MutableShipStatsAPI::getMaxSpeed), StatNames.TOP_SPEED,
            ship -> FragmentSwarms.isFullTo(FragmentSwarms.swarmOf(ship), SwarmHunt.FULL_PERCENT / 100f))) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("full", SwarmHunt.FULL_PERCENT).styled();
        }
    },
    EMP_ARC_STRIKE_SPEED_PERCENT(listener(ArcStrikeSpeedListener.SPEED_PERCENT_KEY, ArcStrikeSpeedListener.class, ArcStrikeSpeedListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", ArcStrikeSpeedListener.BURST_SECONDS).styled();
        }
    },
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
        return TopSpeedShare.isAtLeast(ship, ship.getMaxSpeedWithoutBoost(), TOP_SPEED_SHARE);
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

    static final class SwarmHunt {
        static final float FULL_PERCENT = 90f;

        private SwarmHunt() {
        }
    }
}
