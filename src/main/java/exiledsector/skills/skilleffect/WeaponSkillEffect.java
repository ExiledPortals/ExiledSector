package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

public enum WeaponSkillEffect implements SkillEffect {

    WEAPON_DAMAGE_MULT_PER_DMOD {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
            float mult = SkillEffectSupport.compoundMultPerDMod(stats, magnitude);
            WeaponStatFamily.DAMAGE.target(WeaponScope.ALL).apply(stats, modId, StatMode.MULT, (mult - 1f) * 100f);
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
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            MutableStat burnLevel = stats.getMaxBurnLevel();
            float burnOverDefault = Math.max(0f, burnLevel.getModifiedValue() - burnLevel.getBaseValue());
            WeaponStatFamily.DAMAGE.target(WeaponScope.BALLISTIC).apply(stats, modId, StatMode.MULT, burnOverDefault * magnitude);
        }
    },
    ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            float rangeBonus = stats.getSensorStrength().getModifiedValue() * magnitude;
            WeaponStatFamily.RANGE.target(WeaponScope.ENERGY).apply(stats, modId, StatMode.FLAT, rangeBonus);
        }
    },
    MISSILE_RELOAD_PERCENT_PER_MINUTE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public boolean isConditional() {
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
            // "large_ballistic_mod" is the same dynamic stat key vanilla's own Heavy Ballistics
            // Integration hull mod reads to reduce the ordnance point cost of large ballistic weapons
            stats.getDynamic().getMod(LARGE_BALLISTIC_OP_COST_KEY).modifyFlat(modId, magnitude);
        }

        @Override
        public StyledText description(float magnitude) {
            return StatMode.FLAT.describeStat(magnitude, "stat.ordnancePointCostOfLargeBallisticWeapons");
        }
    };

    private static final String LARGE_BALLISTIC_OP_COST_KEY = "large_ballistic_mod";

    @Override
    public StyledText description(float magnitude) {
        return EffectText.templated(this, magnitude);
    }
}
