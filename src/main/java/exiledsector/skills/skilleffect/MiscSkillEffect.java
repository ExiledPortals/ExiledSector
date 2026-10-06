package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.i18n.StyledText;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicMod;
import static exiledsector.skills.skilleffect.StatTarget.liveBonus;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum MiscSkillEffect implements BackedSkillEffect {

    PD_IGNORES_DECOY_FLARES {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(modId, 1f);
        }
    },
    PD_BEST_TARGET_LEADING {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(modId, 1f);
        }
    },
    PD_DAMAGE_TO_MISSILES_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToMissiles),
            "stat.damageDealtToMissilesByPointDefenceWeapons", false),
    PD_RECLASSIFY_SMALL_WEAPONS {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                boolean sizeMatches = weapon.getSize() == WeaponSize.SMALL;
                if (sizeMatches && weapon.getType() != WeaponType.MISSILE && !weapon.hasAIHint(AIHints.STRIKE)) {
                    weapon.setPD(true);
                }
            }
        }
    },
    ELECTRONIC_WARFARE_PENALTY_PERCENT(PERCENT, dynamicMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD),
            "stat.electronicWarfarePenaltyAgainstThisShipSWeaponRange", true),
    ELECTRONIC_WARFARE_PENALTY_MULT(MULT, dynamicMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD),
            "stat.electronicWarfarePenaltyAgainstThisShipSWeaponRange", true),

    ELECTRONIC_WARFARE(FLAT, dynamicMod("electronic_warfare_flat"), "stat.ecmRating", false),
    NAV_RATING(FLAT, dynamicMod("coord_maneuvers_flat"), "stat.fleetNavRating", false),
    SYSTEM_CHARGES_FLAT(FLAT, bonus(MutableShipStatsAPI::getSystemUsesBonus), "stat.systemCharges", false),
    DAMAGE_TO_CAPITAL_PERCENT(PERCENT, stat(MutableShipStatsAPI::getDamageToCapital), "stat.damageToCapitalShips", false),
    WEAPON_MALFUNCTION_PERCENT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getWeaponMalfunctionChance().modifyFlat(modId, magnitude / 100f);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.PERCENT.describeStat(magnitude, "stat.weaponMalfunctionChance");
        }

        @Override
        public boolean lowerIsBetter() {
            return true;
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
    PEAK_CR_DURATION_PERCENT(PERCENT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration", false),
    PEAK_CR_DURATION_MULT(MULT, liveBonus(MutableShipStatsAPI::getPeakCRDuration), "stat.peakCombatReadinessDuration", false),
    COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP(new ConditionalStatEffect(PERCENT,
            new StatTarget.BonusPercentagePoints(stats -> stats.getDynamic().getMod("command_point_rate_flat")),
            "stat.commandPointRecoveryWhileFlagship", MiscSkillEffect::isFlagship));

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

    MiscSkillEffect() {
        this((EffectBacking) null);
    }

    MiscSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    MiscSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
