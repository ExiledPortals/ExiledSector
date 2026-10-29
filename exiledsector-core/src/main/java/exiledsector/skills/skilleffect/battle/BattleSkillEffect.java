package exiledsector.skills.skilleffect.battle;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SimpleStatEffect;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.dynamicMod;

public enum BattleSkillEffect implements BackedSkillEffect {

    ELECTRONIC_WARFARE(FLAT, dynamicMod("electronic_warfare_flat"), "stat.ecmRating", false),
    ELECTRONIC_WARFARE_PENALTY_PERCENT(PERCENT, dynamicMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD),
            "stat.electronicWarfarePenaltyAgainstThisShipSWeaponRange", true),
    ELECTRONIC_WARFARE_PENALTY_MULT(MULT, dynamicMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD),
            "stat.electronicWarfarePenaltyAgainstThisShipSWeaponRange", true),
    NAV_RATING(FLAT, dynamicMod("coord_maneuvers_flat"), "stat.fleetNavRating", false) {
        @Override
        public StyledText description(float magnitude) {
            return FLAT.describeStat(magnitude, "subject.yourFleet", "stat.fleetNavRating");
        }
    },
    OBJECTIVE_CAPTURE_RATE_MULT {
        @Override
        public StatMode statMode() {
            return StatMode.MULT;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            SkillEffectSupport.applyMult(stats.getDynamic().getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT), modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    OBJECTIVE_CAPTURE_RANGE_FLAT(FLAT, dynamicMod(Stats.SHIP_OBJECTIVE_CAP_RANGE_MOD),
            "stat.rangeFromWhichCombatObjectivesCanBeCaptured", false),
    COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP(new ConditionalStatEffect(PERCENT,
            new StatTarget.BonusPercentagePoints(stats -> stats.getDynamic().getMod("command_point_rate_flat")),
            "stat.commandPointRecoveryWhileFlagship", BattleSkillEffect::isFlagship));

    private static boolean isFlagship(ShipAPI ship) {
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            return true;
        }
        FleetMemberAPI member = ship.getMutableStats().getFleetMember();
        if (member == null) {
            return false;
        }
        PersonAPI commander = member.getFleetCommanderForStats();
        if (commander == null) commander = member.getFleetCommander();
        return commander != null && commander == ship.getCaptain();
    }

    private final EffectBacking backing;

    BattleSkillEffect() {
        this((EffectBacking) null);
    }

    BattleSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    BattleSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
