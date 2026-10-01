package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

sealed interface StatTarget {

    void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude);

    default void applyAfterShipCreation(ShipAPI ship) {
    }

    default boolean supportsTemporaryGating() {
        return false;
    }

    default boolean supports(StatMode mode) {
        return true;
    }

    static StatTarget stat(Function<MutableShipStatsAPI, MutableStat> getter) {
        return new OfStat(getter, false);
    }

    static StatTarget bonus(Function<MutableShipStatsAPI, StatBonus> getter) {
        return new OfBonus(getter, false);
    }

    static StatTarget liveStat(Function<MutableShipStatsAPI, MutableStat> getter) {
        return new OfStat(getter, true);
    }

    static StatTarget liveBonus(Function<MutableShipStatsAPI, StatBonus> getter) {
        return new OfBonus(getter, true);
    }

    static StatTarget dynamicStat(String key) {
        return stat(stats -> stats.getDynamic().getStat(key));
    }

    static StatTarget dynamicMod(String key) {
        return bonus(stats -> stats.getDynamic().getMod(key));
    }

    static StatTarget all(StatTarget... parts) {
        return new Composite(List.of(parts));
    }

    record OfStat(Function<MutableShipStatsAPI, MutableStat> stat, boolean liveInCombat) implements StatTarget {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude) {
            mode.apply(stat.apply(stats), modId, magnitude);
        }

        @Override
        public boolean supportsTemporaryGating() {
            return liveInCombat;
        }
    }

    record OfBonus(Function<MutableShipStatsAPI, StatBonus> bonus, boolean liveInCombat) implements StatTarget {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude) {
            mode.apply(bonus.apply(stats), modId, magnitude);
        }

        @Override
        public boolean supportsTemporaryGating() {
            return liveInCombat;
        }
    }

    record Composite(List<StatTarget> parts) implements StatTarget {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude) {
            for (StatTarget part : parts) {
                part.apply(stats, modId, mode, magnitude);
            }
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship) {
            for (StatTarget part : parts) {
                part.applyAfterShipCreation(ship);
            }
        }

        @Override
        public boolean supportsTemporaryGating() {
            return parts.stream().allMatch(StatTarget::supportsTemporaryGating);
        }

        @Override
        public boolean supports(StatMode mode) {
            return parts.stream().allMatch(part -> part.supports(mode));
        }
    }

    record Compensated(StatTarget plus, StatTarget offset, Set<StatMode> modes) implements StatTarget {

        static final String OFFSET_SUFFIX = "_nonBeamOffset";

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude) {
            plus.apply(stats, modId, mode, magnitude);
            offset.apply(stats, modId + OFFSET_SUFFIX, mode, mode.inverse(magnitude));
        }

        @Override
        public boolean supportsTemporaryGating() {
            return plus.supportsTemporaryGating() && offset.supportsTemporaryGating();
        }

        @Override
        public boolean supports(StatMode mode) {
            return modes.contains(mode);
        }
    }

    record PercentagePoints(Function<MutableShipStatsAPI, MutableStat> stat) implements StatTarget {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude) {
            stat.apply(stats).modifyFlat(modId, magnitude / 100f);
        }

        @Override
        public boolean supports(StatMode mode) {
            return mode == StatMode.PERCENT;
        }
    }

    record PerWeaponAmmo(WeaponScope scope, boolean regeneration) implements StatTarget {

        private String key() {
            return "exiledSector_" + scope.name() + (regeneration ? "_AMMO_REGEN" : "_AMMO");
        }

        @Override
        public void apply(MutableShipStatsAPI stats, String modId, StatMode mode, float magnitude) {
            mode.apply(stats.getDynamic().getMod(key()), modId, magnitude);
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            StatBonus child = stats.getDynamic().getMod(key());
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (scope.matches(weapon) && weapon.usesAmmo()) {
                    overrideWeapon(weapon, stats, child);
                }
            }
        }

        private void overrideWeapon(WeaponAPI weapon, MutableShipStatsAPI stats, StatBonus child) {
            if (regeneration) {
                MutableStat energy = stats.getEnergyAmmoRegenMult();
                float pooled = (energy.getBaseValue() + energy.getFlatMod())
                        * (1f + (energy.getPercentMod() + child.getPercentMod()) / 100f)
                        * energy.getMult() * child.getMult();
                float current = energy.getModifiedValue();
                if (current > 0f) {
                    weapon.getAmmoTracker().setAmmoPerSecond(weapon.getSpec().getAmmoPerSecond() * pooled / current);
                }
                return;
            }
            StatBonus energy = stats.getEnergyAmmoBonus();
            float pooledMult = (1f + (energy.getPercentMod() + child.getPercentMod()) / 100f) * energy.getMult() * child.getMult();
            int maxAmmo = (int) (weapon.getSpec().getMaxAmmo() * pooledMult);
            maxAmmo = (int) (maxAmmo + energy.getFlatBonus() + child.getFlatBonus());
            weapon.setMaxAmmo(maxAmmo);
            weapon.resetAmmo();
        }

        @Override
        public boolean supports(StatMode mode) {
            return !regeneration || mode != StatMode.FLAT;
        }
    }
}
