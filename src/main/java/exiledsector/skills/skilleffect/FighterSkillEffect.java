package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WingRole;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipFacts;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicStat;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum FighterSkillEffect implements SkillEffect {

    FIGHTER_WEAPON_DAMAGE_PERCENT(RoleStat.WEAPON_DAMAGE),
    FIGHTER_TOP_SPEED_PERCENT(RoleStat.TOP_SPEED),
    FIGHTER_CREW_LOSS_PERCENT(PERCENT, dynamicStat("fighter_crew_loss_mult"),
            "stat.casualtiesSufferedByFighterPilotsLaunchedFromThisShip", true),
    FIGHTER_CREW_LOSS_MULT(MULT, dynamicStat("fighter_crew_loss_mult"),
            "stat.casualtiesSufferedByFighterPilotsLaunchedFromThisShip", true),
    FIGHTER_REFIT_TIME_MULT(MULT, liveStat(MutableShipStatsAPI::getFighterRefitTimeMult), "stat.fighterRefitTime", true),
    FIGHTER_REFIT_TIME_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getFighterRefitTimeMult), "stat.fighterRefitTime", true),
    FIGHTER_REPLACEMENT_RATE_MULT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = 1f / SkillEffectSupport.multFrom(magnitude);
            stats.getDynamic().getStat("replacement_rate_decrease_mult").modifyMult(modId, mult);
            stats.getDynamic().getStat("replacement_rate_increase_mult").modifyMult(modId, mult);
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this, magnitude >= 0 ? "slower" : "faster").arg("value", Math.abs(magnitude)).styled();
        }
    },
    FIGHTER_REPLACEMENT_DECAY_PERCENT(PERCENT, dynamicStat("replacement_rate_decrease_mult"),
            "stat.rateAtWhichFighterReplacementCapabilityDecaysFromLosses", true),
    FIGHTER_REPLACEMENT_RECOVERY_PERCENT(PERCENT, dynamicStat("replacement_rate_increase_mult"),
            "stat.rateAtWhichFighterReplacementCapabilityRecovers", false),
    FIGHTER_PD_DAMAGE_BONUS_PERCENT {
        @Override
        public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
            MutableShipStatsAPI fighterStats = fighter.getMutableStats();
            fighterStats.getDamageToFighters().modifyPercent(modId, magnitude);
            fighterStats.getDamageToMissiles().modifyPercent(modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.PERCENT.describeStat(magnitude, "stat.damageDealtByFightersLaunchedFromThisShipToOtherFightersAndMissiles");
        }
    },
    FIGHTER_RELAUNCH_TIME_FLAT {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod("fighter_rearm_time_extra_fraction_of_base_refit_time_mod").modifyFlat(modId, magnitude / 100f);
        }

        @Override
        public boolean supportsTemporaryGating() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.FLAT.describeStat(magnitude, "stat.fighterRelaunchTimeAsAOfBaseRefitTime");
        }
    },
    FIGHTER_ARMOR_PERCENT(RoleStat.ARMOR),
    FIGHTER_SHIELD_DAMAGE_TAKEN_PERCENT(RoleStat.SHIELD_DAMAGE_TAKEN),
    FIGHTER_RATE_OF_FIRE_PERCENT(RoleStat.RATE_OF_FIRE),
    FIGHTER_ENGAGEMENT_RANGE_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getFighterWingRange), "stat.fighterEngagementRange", false),
    FIGHTER_ENGAGEMENT_RANGE_MULT(MULT, bonus(MutableShipStatsAPI::getFighterWingRange), "stat.fighterEngagementRange", false),
    FIGHTER_WEAPON_RANGE_FLAT(RoleStat.WEAPON_RANGE),
    FIGHTER_ROLE_DAMAGE_PERCENT(WingRole.FIGHTER, RoleStat.WEAPON_DAMAGE),
    FIGHTER_ROLE_TOP_SPEED_PERCENT(WingRole.FIGHTER, RoleStat.TOP_SPEED),
    FIGHTER_ROLE_ARMOR_PERCENT(WingRole.FIGHTER, RoleStat.ARMOR),
    FIGHTER_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT(WingRole.FIGHTER, RoleStat.SHIELD_DAMAGE_TAKEN),
    FIGHTER_ROLE_RATE_OF_FIRE_PERCENT(WingRole.FIGHTER, RoleStat.RATE_OF_FIRE),

    INTERCEPTOR_ROLE_DAMAGE_PERCENT(WingRole.INTERCEPTOR, RoleStat.WEAPON_DAMAGE),
    INTERCEPTOR_ROLE_TOP_SPEED_PERCENT(WingRole.INTERCEPTOR, RoleStat.TOP_SPEED),
    INTERCEPTOR_ROLE_ARMOR_PERCENT(WingRole.INTERCEPTOR, RoleStat.ARMOR),
    INTERCEPTOR_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT(WingRole.INTERCEPTOR, RoleStat.SHIELD_DAMAGE_TAKEN),
    INTERCEPTOR_ROLE_RATE_OF_FIRE_PERCENT(WingRole.INTERCEPTOR, RoleStat.RATE_OF_FIRE),

    BOMBER_ROLE_DAMAGE_PERCENT(WingRole.BOMBER, RoleStat.WEAPON_DAMAGE),
    BOMBER_ROLE_TOP_SPEED_PERCENT(WingRole.BOMBER, RoleStat.TOP_SPEED),
    BOMBER_ROLE_ARMOR_PERCENT(WingRole.BOMBER, RoleStat.ARMOR),
    BOMBER_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT(WingRole.BOMBER, RoleStat.SHIELD_DAMAGE_TAKEN),
    BOMBER_ROLE_RATE_OF_FIRE_PERCENT(WingRole.BOMBER, RoleStat.RATE_OF_FIRE),

    SUPPORT_ROLE_DAMAGE_PERCENT(WingRole.SUPPORT, RoleStat.WEAPON_DAMAGE),
    SUPPORT_ROLE_TOP_SPEED_PERCENT(WingRole.SUPPORT, RoleStat.TOP_SPEED),
    SUPPORT_ROLE_ARMOR_PERCENT(WingRole.SUPPORT, RoleStat.ARMOR),
    SUPPORT_ROLE_SHIELD_DAMAGE_TAKEN_PERCENT(WingRole.SUPPORT, RoleStat.SHIELD_DAMAGE_TAKEN),
    SUPPORT_ROLE_RATE_OF_FIRE_PERCENT(WingRole.SUPPORT, RoleStat.RATE_OF_FIRE),

    REMOVE_ALL_FIGHTER_BAYS {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getNumFighterBays().modifyFlat(modId, -Math.round(stats.getNumFighterBays().getBaseValue()));
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return ship.onlyBuiltInWings() ? null : Translation.text("node.block.builtInWingsOnly");
        }
    },
    FIGHTER_BAYS_FLAT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getNumFighterBays().modifyFlat(modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.FLAT.describeStat(magnitude, "stat.numberOfFighterBays");
        }

        @Override
        public String blockDeallocationReason(FleetMemberAPI member, float magnitude) {
            int fittedWings = member.getVariant().getFittedWings().size();
            float baysWithoutThis = member.getStats().getNumFighterBays().getModifiedValue() - magnitude;
            if (fittedWings > baysWithoutThis) {
                return Translation.text("node.block.fighterBaysInUse");
            }
            return null;
        }

        @Override
        public boolean hasDeallocationCondition() {
            return true;
        }

        @Override
        public StyledText deallocationWarning(float magnitude) {
            return EffectText.msg(this, "warning").styled();
        }
    },
    CONVERTED_HANGAR_FIGHTER_BAYS_FLAT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            FIGHTER_BAYS_FLAT.apply(stats, modId, magnitude + convertedHangarBonusBays(stats));
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).styled();
        }

        @Override
        public String blockDeallocationReason(FleetMemberAPI member, float magnitude) {
            return FIGHTER_BAYS_FLAT.blockDeallocationReason(member, magnitude + convertedHangarBonusBays(member.getStats()));
        }

        @Override
        public boolean hasDeallocationCondition() {
            return true;
        }

        @Override
        public StyledText deallocationWarning(float magnitude) {
            return FIGHTER_BAYS_FLAT.deallocationWarning(magnitude);
        }
    };

    private static float convertedHangarBonusBays(MutableShipStatsAPI stats) {
        return stats.getDynamic().getMod(Stats.CONVERTED_HANGAR_MOD).computeEffective(0f);
    }

    private static WingRole effectiveRole(ShipAPI fighter) {
        FighterWingAPI wing = fighter.getWing();
        WingRole role = wing == null ? null : wing.getRole();
        if (role == WingRole.FIGHTER || role == WingRole.INTERCEPTOR || role == WingRole.BOMBER || role == WingRole.SUPPORT) {
            return role;
        }
        return WingRole.FIGHTER;
    }

    private static boolean matchesRole(ShipAPI fighter, WingRole role) {
        return role == null || effectiveRole(fighter) == role;
    }

    private static String roleKey(WingRole role) {
        return "fighter.role." + (role == null ? "ALL" : role.name());
    }

    private final WingRole role;
    private final RoleStat roleStat;
    private final SimpleStatEffect simpleStat;

    FighterSkillEffect() {
        this(null, null, null);
    }

    FighterSkillEffect(RoleStat roleStat) {
        this(null, roleStat, null);
    }

    FighterSkillEffect(WingRole role, RoleStat roleStat) {
        this(role, roleStat, null);
    }

    FighterSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(null, null, new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    FighterSkillEffect(WingRole role, RoleStat roleStat, SimpleStatEffect simpleStat) {
        this.role = role;
        this.roleStat = roleStat;
        this.simpleStat = simpleStat;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        if (simpleStat != null) {
            simpleStat.apply(stats, modId, magnitude);
        }
    }

    @Override
    public boolean supportsTemporaryGating() {
        return simpleStat != null && simpleStat.supportsTemporaryGating();
    }

    @Override
    public boolean lowerIsBetter() {
        if (simpleStat != null) {
            return simpleStat.lowerIsBetter();
        }
        return roleStat != null && roleStat.stat.lowerIsBetter();
    }

    @Override
    public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
        if (roleStat != null && matchesRole(fighter, role)) {
            roleStat.applyTo(fighter, modId, magnitude);
        }
    }

    @Override
    public StyledText description(float magnitude) {
        if (simpleStat != null) {
            return simpleStat.description(magnitude);
        }
        if (roleStat == null) {
            return EffectText.templated(this, magnitude);
        }
        String stat = Translation.msg("fighter.launched").arg("stat", Translation.text(roleStat.stat.statKey())).arg("role", Translation.text(roleKey(role))).text();
        return roleStat.stat.mode().description(magnitude, stat);
    }

    private enum RoleStat {
        WEAPON_DAMAGE(PERCENT, WeaponStatFamily.DAMAGE.target(WeaponScope.ALL), "weapon.stat.DAMAGE"),
        TOP_SPEED(PERCENT, stat(MutableShipStatsAPI::getMaxSpeed), "stat.topSpeed"),
        ARMOR(PERCENT, bonus(MutableShipStatsAPI::getArmorBonus), "stat.armor") {
            @Override
            void applyTo(ShipAPI fighter, String modId, float magnitude) {
                StatBonus armor = fighter.getMutableStats().getArmorBonus();
                float baseArmor = fighter.getHullSpec().getArmorRating();
                float before = armor.computeEffective(baseArmor);
                super.applyTo(fighter, modId, magnitude);
                if (before > 0f) {
                    scaleArmorCells(fighter.getArmorGrid(), armor.computeEffective(baseArmor) / before);
                }
            }
        },
        SHIELD_DAMAGE_TAKEN(new SimpleStatEffect(PERCENT, stat(MutableShipStatsAPI::getShieldDamageTakenMult), "stat.damageTakenByShields", true)),
        RATE_OF_FIRE(PERCENT, all(stat(MutableShipStatsAPI::getBallisticRoFMult), stat(MutableShipStatsAPI::getEnergyRoFMult),
                stat(MutableShipStatsAPI::getMissileRoFMult)), "weapon.stat.FIRE_RATE"),
        WEAPON_RANGE(FLAT, all(bonus(MutableShipStatsAPI::getBallisticWeaponRangeBonus), bonus(MutableShipStatsAPI::getEnergyWeaponRangeBonus)),
                "weapon.stat.RANGE");

        private final SimpleStatEffect stat;

        RoleStat(StatMode mode, StatTarget target, String statKey) {
            this(new SimpleStatEffect(mode, target, statKey, false));
        }

        RoleStat(SimpleStatEffect stat) {
            this.stat = stat;
        }

        void applyTo(ShipAPI fighter, String modId, float magnitude) {
            stat.apply(fighter.getMutableStats(), modId, magnitude);
        }

        private static void scaleArmorCells(ArmorGridAPI grid, float factor) {
            if (grid == null || factor == 1f) {
                return;
            }
            float[][] cells = grid.getGrid();
            for (int x = 0; x < cells.length; x++) {
                for (int y = 0; y < cells[x].length; y++) {
                    grid.setArmorValue(x, y, grid.getArmorValue(x, y) * factor);
                }
            }
        }
    }
}
