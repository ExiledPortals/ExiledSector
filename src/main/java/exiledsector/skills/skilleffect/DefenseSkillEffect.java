package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipFacts;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

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
    ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getArmorDamageTakenMult().modifyMult(modId, SkillEffectSupport.compoundMultPerDMod(stats, magnitude));
        }

        @Override
        public boolean supportsTemporaryGating() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.MULT.describeStat(magnitude, "stat.armorDamageTakenPerDMod");
        }
    },
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
    SHIELD_ABSORPTION_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getShieldAbsorptionMult), "stat.damageTakenByShields", true),
    SHIELD_DAMAGE_TAKEN_MULT(MULT, liveStat(MutableShipStatsAPI::getShieldDamageTakenMult), "stat.damageTakenByShields", true),
    ENGINE_DURABILITY_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getEngineHealthBonus), "stat.engineDurability", false),
    REPAIR_TIME_PERCENT(PERCENT, all(liveStat(MutableShipStatsAPI::getCombatWeaponRepairTimeMult),
            liveStat(MutableShipStatsAPI::getCombatEngineRepairTimeMult)),
            "stat.weaponAndEngineRepairTime", true),
    REPAIR_TIME_MULT(MULT, all(liveStat(MutableShipStatsAPI::getCombatWeaponRepairTimeMult),
            liveStat(MutableShipStatsAPI::getCombatEngineRepairTimeMult)),
            "stat.weaponAndEngineRepairTime", true) {
        @Override
        public boolean lowerIsBetter() {
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
            "stat.energyDamageTakenIncludingHitsOnShieldsArmorAndHull", true);

    private final EffectBacking backing;

    DefenseSkillEffect() {
        this((EffectBacking) null);
    }

    DefenseSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
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
