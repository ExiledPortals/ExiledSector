package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

public enum WeaponSkillEffect implements BackedSkillEffect {

    WEAPON_DAMAGE_MULT_PER_DMOD {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
            WeaponStatFamily.DAMAGE.target(WeaponScope.ALL).apply(stats, modId, StatMode.MULT, (mult - 1f) * 100f);
        }

        @Override
        public boolean isMultiplicative() {
            return true;
        }

        @Override
        public boolean supportsTemporaryGating() {
            return WeaponStatFamily.DAMAGE.target(WeaponScope.ALL).supportsTemporaryGating();
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.MULT.describeStat(magnitude, "stat.weaponDamagePerDMod");
        }
    },
    BALLISTIC_WEAPON_DAMAGE_PER_BURN_LEVEL_MULT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            applyBurnDamage(stats, modId, magnitude);
        }

        @Override
        public boolean appliesAfterOtherEffects() {
            return true;
        }

        @Override
        public StatMode statMode() {
            return StatMode.MULT;
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            applyBurnDamage(ship.getMutableStats(), modId, magnitude);
        }
    },
    ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            applySensorRange(stats, modId, magnitude);
        }

        @Override
        public boolean appliesAfterOtherEffects() {
            return true;
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            applySensorRange(ship.getMutableStats(), modId, magnitude);
        }
    },
    MISSILE_RELOAD_PERCENT_PER_MINUTE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public boolean advancesInCombat() {
            return true;
        }

        @Override
        public void advanceInCombat(ShipAPI ship, String modId, float magnitude, float amount) {
            MissileReloader.advance(ship, modId, magnitude, amount);
        }
    },
    BALLISTIC_WEAPON_LARGE_OP_COST_FLAT {
        @Override
        public boolean lowerIsBetter() {
            return true;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            stats.getDynamic().getMod(LARGE_BALLISTIC_OP_COST_KEY).modifyFlat(modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.FLAT.describeStat(magnitude, "stat.ordnancePointCostOfLargeBallisticWeapons");
        }
    };

    private static final String LARGE_BALLISTIC_OP_COST_KEY = "large_ballistic_mod";

    private static void applyBurnDamage(MutableShipStatsAPI stats, String modId, float magnitude) {
        MutableStat burnLevel = stats.getMaxBurnLevel();
        float burnOverDefault = Math.max(0f, burnLevel.getModifiedValue() - burnLevel.getBaseValue());
        WeaponStatFamily.DAMAGE.target(WeaponScope.BALLISTIC).apply(stats, modId, StatMode.MULT, burnOverDefault * magnitude);
    }

    private static void applySensorRange(MutableShipStatsAPI stats, String modId, float magnitude) {
        float rangeBonus = stats.getSensorStrength().getModifiedValue() * magnitude;
        WeaponStatFamily.RANGE.target(WeaponScope.ENERGY).apply(stats, modId, StatMode.FLAT, rangeBonus);
    }

    @Override
    public EffectBacking backing() {
        return null;
    }
}
