package exiledlib.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;
import exiledlib.skills.skilleffect.PerDModMultEffect;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.SkillEffectSupport;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;

import java.util.function.Function;

import static exiledlib.skills.skilleffect.StatMode.FLAT;
import static exiledlib.skills.skilleffect.StatMode.MULT;
import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.all;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.liveStat;
import static exiledlib.skills.skilleffect.StatTarget.stat;

public enum DefenseSkillEffect implements BackedSkillEffect {

    HULL_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getHullBonus), StatNames.HULL_POINTS, false),
    HULL_FLAT(FLAT, bonus(MutableShipStatsAPI::getHullBonus), StatNames.HULL_POINTS, false),
    HULL_MULT(MULT, bonus(MutableShipStatsAPI::getHullBonus), StatNames.HULL_POINTS, false),
    ARMOR_FLAT(FLAT, bonus(MutableShipStatsAPI::getArmorBonus), "stat.armor", false),
    SHIP_RECOVERY_CHANCE_BONUS {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(modId, magnitude);
        }

        @Override
        public boolean appliesToNpcShips() {
            return false;
        }
    },
    BREAK_PROBABILITY_PERCENT(PERCENT, stat(MutableShipStatsAPI::getBreakProb), "stat.chanceOfThisShipBreakingApartWhenDestroyed", true),
    ARMOR_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getArmorBonus), "stat.armor", false),
    ARMOR_MULT(MULT, bonus(MutableShipStatsAPI::getArmorBonus), "stat.armor", false),
    ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD(new PerDModMultEffect(liveStat(MutableShipStatsAPI::getArmorDamageTakenMult),
            "stat.armorDamageTakenPerDMod", true)),
    ARMOR_FLAT_FOR_LOW_BASE_ARMOR {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            ShipVariantAPI variant = stats.getVariant();
            ShipHullSpecAPI hull = variant == null ? null : variant.getHullSpec();
            if (hull == null || hull.isPhase()) {
                return;
            }
            float bonus = LowBaseArmorBonus.bonus(variant.getHullSize(), hull.getArmorRating());
            stats.getArmorBonus().modifyFlat(modId, bonus * magnitude);
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return LowBaseArmorBonus.fits(ship.hullSize(), ship.phaseHull(), ship.baseArmor()) ? null : Translation.text("node.block.lowBaseArmor");
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this)
                    .arg("most", LowBaseArmorBonus.mostByHullSize(magnitude))
                    .arg("least", LowBaseArmorBonus.leastByHullSize(magnitude))
                    .arg("cutoff", LowBaseArmorBonus.cutoffByHullSize())
                    .styled();
        }

        @Override
        public StyledText description(float magnitude, ShipAPI.HullSize hullSize) {
            if (!LowBaseArmorBonus.covers(hullSize)) {
                return description(magnitude);
            }
            return EffectText.msg(this, "hullSize")
                    .arg("most", LowBaseArmorBonus.most(hullSize, magnitude))
                    .arg("least", LowBaseArmorBonus.least(hullSize, magnitude))
                    .arg("cutoff", LowBaseArmorBonus.cutoff(hullSize))
                    .styled();
        }
    },
    DMOD_EFFECT_MULT {
        @Override
        public StatMode statMode() {
            return StatMode.MULT;
        }

        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            SkillEffectSupport.applyDModEffectMult(stats, modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.MULT.describeStat(magnitude, "stat.negativeEffectsFromDMods");
        }
    },
    DMOD_EFFECT_MULT_PER_DMOD {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public boolean isMultiplicative() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
            SkillEffectSupport.applyDModEffectMult(stats, modId, (mult - 1f) * 100f);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.MULT.describeStat(magnitude, "stat.negativeEffectsFromDModsPerDMod");
        }
    },
    NON_LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public boolean isMultiplicative() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
            SkillEffectSupport.applyNonLogisticsDModEffectMult(stats, modId, (mult - 1f) * 100f);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.MULT.describeStat(magnitude, "stat.negativeEffectsFromNonLogisticsDModsPerDMod");
        }
    },
    LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public boolean isMultiplicative() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
            SkillEffectSupport.applyLogisticsDModEffectMult(stats, modId, (mult - 1f) * 100f);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.MULT.describeStat(magnitude, "stat.negativeEffectsFromLogisticsDModsPerDMod");
        }
    },
    ENGINE_DURABILITY_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getEngineHealthBonus), "stat.engineDurability", false),
    REPAIR_TIME_PERCENT(PERCENT, all(liveStat(MutableShipStatsAPI::getCombatWeaponRepairTimeMult),
            liveStat(MutableShipStatsAPI::getCombatEngineRepairTimeMult)),
            "stat.weaponAndEngineRepairTime", true),
    REPAIR_TIME_MULT(MULT, all(liveStat(MutableShipStatsAPI::getCombatWeaponRepairTimeMult),
            liveStat(MutableShipStatsAPI::getCombatEngineRepairTimeMult)),
            "stat.weaponAndEngineRepairTime", true) {
        @Override
        public boolean invertsDescriptionColours() {
            return false;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this, magnitude <= 0f ? "reduces" : "increases").arg("value", Math.abs(magnitude)).styled();
        }
    },
    EMP_DAMAGE_TAKEN_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getEmpDamageTakenMult), "stat.empDamageTaken", true),
    EMP_DAMAGE_TAKEN_MULT(MULT, liveStat(MutableShipStatsAPI::getEmpDamageTakenMult), "stat.empDamageTaken", true),
    ENERGY_DAMAGE_TAKEN_PERCENT(PERCENT, all(liveStat(MutableShipStatsAPI::getEnergyDamageTakenMult),
            liveStat(MutableShipStatsAPI::getEnergyShieldDamageTakenMult)),
            "stat.energyDamageTaken", true),
    KINETIC_DAMAGE_TAKEN_PERCENT(PERCENT, all(liveStat(MutableShipStatsAPI::getKineticDamageTakenMult),
            liveStat(MutableShipStatsAPI::getKineticShieldDamageTakenMult)),
            "stat.kineticDamageTaken", true),
    HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT(PERCENT, all(liveStat(MutableShipStatsAPI::getHighExplosiveDamageTakenMult),
            liveStat(MutableShipStatsAPI::getHighExplosiveShieldDamageTakenMult)),
            "stat.highExplosiveDamageTaken", true),
    FRAGMENTATION_DAMAGE_TAKEN_PERCENT(PERCENT, all(liveStat(MutableShipStatsAPI::getFragmentationDamageTakenMult),
            liveStat(MutableShipStatsAPI::getFragmentationShieldDamageTakenMult)),
            "stat.fragmentationDamageTaken", true),
    NANOFORGE_HULL_REGEN_PERCENT(NanoforgeMendingListener.REGEN_PERCENT_KEY, NanoforgeMendingListener.class, NanoforgeMendingListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("seconds", NanoforgeMendingListener.UNDAMAGED_SECONDS).arg("value", magnitude)
                    .arg("limit", NanoforgeMendingListener.MAX_TOTAL_REGEN_PERCENT_OF_HULL).styled();
        }
    },
    RISE_FROM_THE_ASHES_HULL_PERCENT(RiseFromTheAshesListener.HULL_PERCENT_KEY, RiseFromTheAshesListener.class, RiseFromTheAshesListener::new),
    RISE_FROM_THE_ASHES_ARMOR_RESTORE_PERCENT(RiseFromTheAshesListener.ARMOR_RESTORE_PERCENT_KEY, RiseFromTheAshesListener.class,
            RiseFromTheAshesListener::new),
    RISE_FROM_THE_ASHES_DAMAGE_TAKEN_PERCENT(RiseFromTheAshesListener.DAMAGE_TAKEN_PERCENT_KEY, RiseFromTheAshesListener.class,
            RiseFromTheAshesListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).arg("seconds", RiseFromTheAshesListener.DURATION_SECONDS).styled();
        }
    },
    TERMINAL_DESCENT_HULL_DAMAGE_TAKEN_PERCENT(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, TerminalDescentListener.class,
            TerminalDescentListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", Math.abs(magnitude))
                    .arg("enter", TerminalDescentListener.ENTER_BELOW_HULL_FRACTION * 100f)
                    .arg("leave", TerminalDescentListener.LEAVE_ABOVE_HULL_FRACTION * 100f).styled();
        }
    },
    TERMINAL_DESCENT_FLUX_DISSIPATION_PERCENT(TerminalDescentListener.FLUX_DISSIPATION_PERCENT_KEY, TerminalDescentListener.class,
            TerminalDescentListener::new),
    TERMINAL_DESCENT_SYSTEM_REGEN_PERCENT(TerminalDescentListener.SYSTEM_REGEN_PERCENT_KEY, TerminalDescentListener.class,
            TerminalDescentListener::new),
    TERMINAL_DESCENT_SHIELD_DAMAGE_TAKEN_PERCENT(TerminalDescentListener.SHIELD_DAMAGE_TAKEN_PERCENT_KEY, TerminalDescentListener.class,
            TerminalDescentListener::new),
    DISPLACER_FAILSAFE_HULL_PERCENT(DisplacerFailsafeListener.HULL_PERCENT_KEY, DisplacerFailsafeListener.class, DisplacerFailsafeListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("distance", Math.round(DisplacerFailsafeListener.DISPLACE_DISTANCE)).styled();
        }
    };

    private final EffectBacking backing;

    DefenseSkillEffect() {
        this((EffectBacking) null);
    }

    DefenseSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    <T> DefenseSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory));
    }

    DefenseSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }

    private static final class StatNames {
        static final String HULL_POINTS = "stat.hullPoints";

        private StatNames() {
        }
    }
}
