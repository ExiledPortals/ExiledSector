package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.ConvertedHangar;
import com.fs.starfarer.api.loading.WingRole;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.CombatGate;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SimpleStatEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.WaivedPenalty;
import exiledsector.skills.skilleffect.defense.ArmorCells;
import exiledsector.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.onhit.RiftStrike;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import static exiledsector.skills.skilleffect.Backings.listener;
import static exiledsector.skills.skilleffect.Backings.statEffect;
import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicStat;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum FighterSkillEffect implements BackedSkillEffect {

    FIGHTER_WEAPON_DAMAGE_PERCENT(RoleStat.WEAPON_DAMAGE),
    FIGHTER_TOP_SPEED_PERCENT(RoleStat.TOP_SPEED),
    FIGHTER_CREW_LOSS_PERCENT(statEffect(PERCENT, dynamicStat("fighter_crew_loss_mult"),
            "stat.pilotCasualties", true)) {
        @Override
        public StyledText description(float magnitude) {
            return describeLaunched(PERCENT, null, magnitude, "stat.pilotCasualties");
        }
    },
    FIGHTER_CREW_LOSS_MULT(statEffect(MULT, dynamicStat("fighter_crew_loss_mult"),
            "stat.pilotCasualties", true)) {
        @Override
        public StyledText description(float magnitude) {
            return describeLaunched(MULT, null, magnitude, "stat.pilotCasualties");
        }
    },
    FIGHTER_REFIT_TIME_MULT(statEffect(MULT, liveStat(MutableShipStatsAPI::getFighterRefitTimeMult), "stat.fighterRefitTime", true)),
    FIGHTER_REFIT_TIME_PERCENT(statEffect(PERCENT, liveStat(MutableShipStatsAPI::getFighterRefitTimeMult), "stat.fighterRefitTime", true)),
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
    FIGHTER_REPLACEMENT_DECAY_PERCENT(statEffect(PERCENT, dynamicStat("replacement_rate_decrease_mult"),
            "stat.replacementRateDecrease", true)),
    FIGHTER_REPLACEMENT_RECOVERY_PERCENT(statEffect(PERCENT, dynamicStat("replacement_rate_increase_mult"),
            "stat.replacementRateRecovery", false)),
    FIGHTER_REPLACEMENT_RECOVERY_WHILE_REGROUPING_PERCENT(new ConditionalStatEffect(PERCENT, dynamicStat("replacement_rate_increase_mult"),
            "stat.replacementRateRecoveryWhileRegrouping", ShipAPI::isPullBackFighters)),
    FIGHTER_REPLACEMENT_RATE_RESTORED_BY_FRAGMENTS_PERCENT(listener(HiveReplacementListener.RESTORED_PERCENT_KEY,
            HiveReplacementListener.class, HiveReplacementListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", Math.min(magnitude, 100f))
                    .arg("fragments", HiveReplacementListener.FRAGMENTS_PER_REPLACEMENT).styled();
        }
    },
    FIGHTER_PD_DAMAGE_BONUS_PERCENT {
        @Override
        public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
            MutableShipStatsAPI fighterStats = fighter.getMutableStats();
            fighterStats.getDamageToFighters().modifyPercent(modId, magnitude);
            fighterStats.getDamageToMissiles().modifyPercent(modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return describeLaunched(PERCENT, null, magnitude, "stat.strikecraftDamageToStrikecraftAndMissiles");
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
    FIGHTER_DAMAGE_TAKEN_MULT(RoleStat.DAMAGE_TAKEN),
    STRIKECRAFT_GAIN_AFFINITY_BONUSES {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).styled();
        }
    },
    FIGHTER_RATE_OF_FIRE_PERCENT(RoleStat.RATE_OF_FIRE),
    FIGHTER_ENGAGEMENT_RANGE_PERCENT(statEffect(PERCENT, bonus(MutableShipStatsAPI::getFighterWingRange), "stat.fighterEngagementRange", false)),
    FIGHTER_ENGAGEMENT_RANGE_MULT(statEffect(MULT, bonus(MutableShipStatsAPI::getFighterWingRange), "stat.fighterEngagementRange", false)),
    FIGHTER_WEAPON_RANGE_FLAT(RoleStat.WEAPON_RANGE),
    FIGHTER_WEAPON_RANGE_PERCENT(RoleStat.WEAPON_RANGE_PERCENT),
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
            return EffectText.templated(this, magnitude);
        }

        @Override
        public StyledText deallocationWarning(float magnitude) {
            return FIGHTER_BAYS_FLAT.deallocationWarning(magnitude);
        }
    },
    STRIKECRAFT_OP_DEPLOYMENT_COST_FLAT {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float addedPoints = magnitude * ConvertedHangar.computeDPModifier(ConvertedHangar.getFighterOPCost(stats));
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(modId, addedPoints);
            stats.getSuppliesToRecover().modifyFlat(modId, addedPoints);
            FleetMemberAPI member = stats.getFleetMember();
            float suppliesPerDeployment = member == null ? 0f : member.getHullSpec().getSuppliesToRecover();
            if (suppliesPerDeployment > 0f) {
                stats.getCRPerDeploymentPercent().modifyMult(modId, 1f + addedPoints / suppliesPerDeployment);
            }
        }

        @Override
        public boolean supportsTemporaryGating() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).arg("op", ConvertedHangar.FIGHTER_OP_PER_DP).styled();
        }
    },
    CONVERTED_HANGAR_DEPLOYMENT_COST_FLAT(new WaivedPenalty(STRIKECRAFT_OP_DEPLOYMENT_COST_FLAT, Stats.CONVERTED_HANGAR_NO_DP_INCREASE)),
    CONVERTED_HANGAR_REFIT_TIME_MULT(new WaivedPenalty(FIGHTER_REFIT_TIME_MULT, Stats.CONVERTED_HANGAR_NO_REFIT_PENALTY)),
    CONVERTED_HANGAR_REPLACEMENT_RATE_MULT(new WaivedPenalty(FIGHTER_REPLACEMENT_RATE_MULT, Stats.CONVERTED_HANGAR_NO_REFIT_PENALTY)),
    CONVERTED_HANGAR_RELAUNCH_TIME_FLAT(new WaivedPenalty(FIGHTER_RELAUNCH_TIME_FLAT, Stats.CONVERTED_HANGAR_NO_REARM_INCREASE)),
    CONVERTED_HANGAR_MIN_CREW_FLAT(new WaivedPenalty(LogisticsSkillEffect.MIN_CREW_FLAT, Stats.CONVERTED_HANGAR_NO_CREW_INCREASE)),
    WING_SCRAMBLE_COPIES(listener(WingScrambleListener.COPIES_KEY, WingScrambleListener.class, WingScrambleListener::new)) {
        @Override
        public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
            for (WingScrambleListener listener : parentShip.getListeners(WingScrambleListener.class)) {
                listener.fighterLaunched();
            }
        }
    },
    FIGHTER_WING_SHARED_DAMAGE_PERCENT(new FighterListenerEffect<>(PhaseEntangledWingsListener.SHARED_PERCENT_KEY, PhaseEntangledWingsListener.class,
            PhaseEntangledWingsListener::new)) {

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", Math.min(magnitude, PhaseEntangledWingsListener.MAX_SHARED_PERCENT)).styled();
        }
    },
    STRIKECRAFT_DAMAGE_TO_DRONES_PERCENT(new FighterListenerEffect<>(DroneDamageListener.DAMAGE_PERCENT_KEY, DroneDamageListener.class,
            DroneDamageListener::new)),
    STRIKECRAFT_DAMAGE_TO_DAMAGED_SHIPS_PERCENT(new FighterListenerEffect<>(DamagedShipDamageListener.DAMAGE_PERCENT_KEY,
            DamagedShipDamageListener.class, DamagedShipDamageListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("hull", CombatGate.LOW_HULL_LEVEL * 100f).styled();
        }
    },
    STRIKECRAFT_WEAPON_DAMAGE_PERCENT_PER_NEARBY_STRIKECRAFT(new FighterListenerEffect<>(SwarmLinkListener.DAMAGE_PERCENT_PER_STRIKECRAFT_KEY,
            SwarmLinkListener.class, SwarmLinkListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", SwarmLinkListener.RANGE)
                    .arg("max", SwarmLinkListener.MAX_DAMAGE_PERCENT).styled();
        }
    },
    STRIKECRAFT_DEATH_BURST_DAMAGE_FLAT(new FighterListenerEffect<>(CinderBurstListener.DAMAGE_KEY, CinderBurstListener.class,
            CinderBurstListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", CinderBurstListener.BURST_RADIUS).styled();
        }
    },
    STRIKECRAFT_DAMAGE_PER_LOST_STRIKECRAFT_PERCENT(new FighterListenerEffect<>(AccretionListener.DAMAGE_PERCENT_PER_STACK_KEY,
            AccretionListener.class, AccretionListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", AccretionStacks.STACK_SECONDS)
                    .arg("max", AccretionStacks.MAX_STACKS).styled();
        }
    },
    STRIKECRAFT_EMP_DAMAGE_PERCENT(new FighterListenerEffect<>(StrikecraftEmpListener.EMP_PERCENT_KEY, StrikecraftEmpListener.class,
            StrikecraftEmpListener::new)),
    STRIKECRAFT_RIFT_STRIKE_CHANCE_PERCENT(new FighterListenerEffect<>(StrikecraftRiftStrikeListener.CHANCE_PERCENT_KEY,
            StrikecraftRiftStrikeListener.class, StrikecraftRiftStrikeListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("share", RiftStrike.DAMAGE_SHARE_PERCENT)
                    .arg("min", RiftStrike.MIN_DAMAGE).arg("max", RiftStrike.MAX_DAMAGE).styled();
        }
    },
    STRIKECRAFT_RIFT_EXPLOSION_DAMAGE_PERCENT(new FighterListenerEffect<>(StrikecraftRiftExplosionDamageListener.DAMAGE_PERCENT_KEY,
            StrikecraftRiftExplosionDamageListener.class, (fighter, carrier) -> new StrikecraftRiftExplosionDamageListener(carrier))),
    FIGHTER_WING_DAMAGE_TAKEN_MULT_PER_FIGHTER(new FighterListenerEffect<>(WingNumbersDamageListener.REDUCTION_PERCENT_PER_FIGHTER_KEY,
            WingNumbersDamageListener.class, WingNumbersDamageListener::new)) {

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("max", WingNumbersDamageListener.MAX_REDUCTION_PERCENT).styled();
        }
    },
    HARDLIGHT_MARKING_DAMAGE_PERCENT_PER_HIT(HardlightMarking.backing(HardlightMarking.PERCENT_PER_HIT_KEY)) {

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("reset", HardlightMarking.RESET_AFTER_SECONDS).styled();
        }
    },
    HARDLIGHT_MARKING_MAX_DAMAGE_PERCENT(HardlightMarking.backing(HardlightMarking.MAX_PERCENT_KEY));

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
        DAMAGE_TAKEN(new SimpleStatEffect(MULT, all(stat(MutableShipStatsAPI::getHullDamageTakenMult), stat(MutableShipStatsAPI::getArmorDamageTakenMult),
                stat(MutableShipStatsAPI::getShieldDamageTakenMult)), "stat.damageTaken", true)),
        RATE_OF_FIRE(PERCENT, WeaponStatFamily.RATE_OF_FIRE.target(WeaponScope.ALL), "weapon.stat.RATE_OF_FIRE"),
        WEAPON_RANGE(FLAT, all(bonus(MutableShipStatsAPI::getBallisticWeaponRangeBonus), bonus(MutableShipStatsAPI::getEnergyWeaponRangeBonus)),
                "weapon.stat.RANGE"),
        WEAPON_RANGE_PERCENT(PERCENT, WeaponStatFamily.RANGE.target(WeaponScope.ALL), "weapon.stat.RANGE");

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
