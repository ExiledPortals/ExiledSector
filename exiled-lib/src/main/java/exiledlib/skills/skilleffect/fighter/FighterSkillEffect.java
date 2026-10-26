package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WingRole;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.SkillEffectSupport;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;
import exiledlib.skills.skilleffect.WaivedPenalty;
import exiledlib.skills.skilleffect.defense.ArmorCells;
import exiledlib.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledlib.skills.skilleffect.weapon.WeaponScope;
import exiledlib.skills.skilleffect.weapon.WeaponStatFamily;

import java.util.function.Function;

import static exiledlib.skills.skilleffect.StatMode.FLAT;
import static exiledlib.skills.skilleffect.StatMode.MULT;
import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.all;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.dynamicStat;
import static exiledlib.skills.skilleffect.StatTarget.liveStat;
import static exiledlib.skills.skilleffect.StatTarget.stat;

public enum FighterSkillEffect implements BackedSkillEffect {

    FIGHTER_WEAPON_DAMAGE_PERCENT(RoleStat.WEAPON_DAMAGE),
    FIGHTER_TOP_SPEED_PERCENT(RoleStat.TOP_SPEED),
    FIGHTER_CREW_LOSS_PERCENT(PERCENT, dynamicStat("fighter_crew_loss_mult"),
            "stat.casualtiesSufferedByFighterPilotsLaunchedFromThisShip", true) {
        @Override
        public StyledText description(float magnitude) {
            return describeLaunched(PERCENT, null, magnitude, "stat.casualtiesSufferedByFighterPilotsLaunchedFromThisShip");
        }
    },
    FIGHTER_CREW_LOSS_MULT(MULT, dynamicStat("fighter_crew_loss_mult"),
            "stat.casualtiesSufferedByFighterPilotsLaunchedFromThisShip", true) {
        @Override
        public StyledText description(float magnitude) {
            return describeLaunched(MULT, null, magnitude, "stat.casualtiesSufferedByFighterPilotsLaunchedFromThisShip");
        }
    },
    FIGHTER_REFIT_TIME_MULT(MULT, liveStat(MutableShipStatsAPI::getFighterRefitTimeMult), "stat.fighterRefitTime", true),
    FIGHTER_REFIT_TIME_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getFighterRefitTimeMult), "stat.fighterRefitTime", true),
    FIGHTER_REPLACEMENT_RATE_MULT {
        @Override
        public StatMode statMode() {
            return StatMode.MULT;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = SkillEffectSupport.inverseMultFrom(magnitude);
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
            return describeLaunched(PERCENT, null, magnitude, "stat.damageDealtByFightersLaunchedFromThisShipToOtherFightersAndMissiles");
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
            return EffectText.signed(this, magnitude).styled();
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
    BOMBER_ROLE_ACCELERATION_PERCENT(WingRole.BOMBER, RoleStat.ACCELERATION),
    BOMBER_PAYLOAD_DETONATION_CHANCE_PERCENT(new FighterListenerEffect<>(BomberPayloadDetonation.CHANCE_PERCENT_KEY,
            BomberPayloadDetonation.class, BomberPayloadDetonation::armed)) {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("interval", BomberPayloadDetonation.CHECK_INTERVAL_SECONDS)
                    .arg("distance", BomberPayloadDetonation.SAFE_DISTANCE_FROM_CARRIER).styled();
        }
    },

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
        public StyledText deallocationWarning(float magnitude) {
            return FIGHTER_BAYS_FLAT.deallocationWarning(magnitude);
        }
    },
    CONVERTED_HANGAR_REFIT_TIME_MULT(new WaivedPenalty(FIGHTER_REFIT_TIME_MULT, Stats.CONVERTED_HANGAR_NO_REFIT_PENALTY)),
    CONVERTED_HANGAR_REPLACEMENT_RATE_MULT(new WaivedPenalty(FIGHTER_REPLACEMENT_RATE_MULT, Stats.CONVERTED_HANGAR_NO_REFIT_PENALTY)),
    CONVERTED_HANGAR_RELAUNCH_TIME_FLAT(new WaivedPenalty(FIGHTER_RELAUNCH_TIME_FLAT, Stats.CONVERTED_HANGAR_NO_REARM_INCREASE)),
    CONVERTED_HANGAR_MIN_CREW_FLAT(new WaivedPenalty(LogisticsSkillEffect.MIN_CREW_FLAT, Stats.CONVERTED_HANGAR_NO_CREW_INCREASE)),
    WING_SCRAMBLE_COPIES(WingScrambleListener.COPIES_KEY, WingScrambleListener.class, WingScrambleListener::new) {
        @Override
        public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
            for (WingScrambleListener listener : parentShip.getListeners(WingScrambleListener.class)) {
                listener.fighterLaunched();
            }
        }
    },
    FIGHTER_WING_SHARED_DAMAGE_PERCENT(new FighterListenerEffect<>(WingSharedFateListener.SHARED_PERCENT_KEY, WingSharedFateListener.class,
            WingSharedFateListener::new)) {

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", Math.min(magnitude, WingSharedFateListener.MAX_SHARED_PERCENT)).styled();
        }
    },
    FIGHTER_WING_DAMAGE_TAKEN_PERCENT_PER_FIGHTER(new FighterListenerEffect<>(WingNumbersDamageListener.REDUCTION_PERCENT_PER_FIGHTER_KEY,
            WingNumbersDamageListener.class, WingNumbersDamageListener::new)) {

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("max", WingNumbersDamageListener.MAX_REDUCTION_PERCENT).styled();
        }
    },
    FIGHTER_FOCUS_FIRE_DAMAGE_PERCENT_PER_HIT(FighterFocusFireMarks.backing(FighterFocusFireMarks.PERCENT_PER_HIT_KEY)) {

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("reset", FighterFocusFireMarks.RESET_AFTER_SECONDS).styled();
        }
    },
    FIGHTER_FOCUS_FIRE_MAX_DAMAGE_PERCENT(FighterFocusFireMarks.backing(FighterFocusFireMarks.MAX_PERCENT_KEY));

    private static float convertedHangarBonusBays(MutableShipStatsAPI stats) {
        return stats.getDynamic().getMod(Stats.CONVERTED_HANGAR_MOD).computeEffective(0f);
    }

    private static WingRole effectiveRole(ShipAPI fighter) {
        FighterWingAPI wing = fighter.getWing();
        return effectiveRole(wing == null ? null : wing.getRole());
    }

    static WingRole effectiveRole(WingRole role) {
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

    private static StyledText describeLaunched(StatMode mode, WingRole role, float magnitude, String statKey) {
        String subject = Translation.msg("fighter.launched").arg("role", Translation.text(roleKey(role))).text();
        return mode.description(magnitude, subject, Translation.text(statKey));
    }

    private final EffectBacking backing;

    FighterSkillEffect() {
        this((EffectBacking) null);
    }

    FighterSkillEffect(RoleStat roleStat) {
        this(new RoleBacking(null, roleStat));
    }

    FighterSkillEffect(WingRole role, RoleStat roleStat) {
        this(new RoleBacking(role, roleStat));
    }

    FighterSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    <T> FighterSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory));
    }

    FighterSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }

    record RoleBacking(WingRole role, RoleStat roleStat) implements EffectBacking {

        @Override
        public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
            if (matchesRole(fighter, role)) {
                roleStat.applyTo(fighter, modId, magnitude);
            }
        }

        @Override
        public StatMode mode() {
            return roleStat.stat.mode();
        }

        @Override
        public boolean supportsTemporaryGating() {
            return false;
        }

        @Override
        public boolean lowerIsBetter() {
            return roleStat.stat.lowerIsBetter();
        }

        @Override
        public StyledText description(SkillEffect effect, float magnitude) {
            return describeLaunched(roleStat.stat.mode(), role, magnitude, roleStat.stat.statKey());
        }
    }

    private enum RoleStat {
        WEAPON_DAMAGE(PERCENT, WeaponStatFamily.DAMAGE.target(WeaponScope.ALL), "weapon.stat.DAMAGE"),
        TOP_SPEED(PERCENT, stat(MutableShipStatsAPI::getMaxSpeed), "stat.topSpeed"),
        ACCELERATION(PERCENT, stat(MutableShipStatsAPI::getAcceleration), "stat.acceleration"),
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
        RATE_OF_FIRE(PERCENT, WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL), "weapon.stat.FIRE_RATE"),
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
            ArmorCells.update(grid, current -> current * factor);
        }
    }
}
