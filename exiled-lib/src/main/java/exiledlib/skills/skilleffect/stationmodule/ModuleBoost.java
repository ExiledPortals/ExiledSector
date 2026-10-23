package exiledlib.skills.skilleffect.stationmodule;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;
import exiledlib.skills.skilleffect.defense.ArmorCells;
import exiledlib.skills.skilleffect.weapon.WeaponScope;
import exiledlib.skills.skilleffect.weapon.WeaponStatFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModuleBoost {

    static final String HULL_PERCENT_KEY = "exiledSector_moduleHullPercent";
    static final String ARMOR_PERCENT_KEY = "exiledSector_moduleArmorPercent";
    private static final List<String> MODULE_BONUS_KEYS = List.of(HULL_PERCENT_KEY, ARMOR_PERCENT_KEY);
    private static final String MODULE_BONUS_MOD_ID_PREFIX = "exiledSector_moduleBonus_";
    private static final float MAX_SHIELD_ARC = 360f;

    private ModuleBoost() {
    }

    public static boolean hasModuleBonus(MutableShipStatsAPI parentStats) {
        for (String key : MODULE_BONUS_KEYS) {
            if (parentStats.getDynamic().getValue(key, 0f) != 0f) {
                return true;
            }
        }
        return false;
    }

    public static int expectedModuleCount(ShipAPI ship) {
        Map<String, String> stationModules = ship.getVariant() == null ? null : ship.getVariant().getStationModules();
        return stationModules == null ? 0 : stationModules.size();
    }

    public static void boost(ShipAPI parentShip, ShipAPI module, Map<SkillEffect, Float> sharedTotals, String sharedModIdPrefix) {
        MutableShipStatsAPI stats = module.getMutableStats();
        ShipHullSpecAPI hullSpec = module.getHullSpec();
        float hullBase = hullSpec.getHitpoints();
        float armorBase = hullSpec.getArmorRating();
        float arcBase = shieldArcBase(module);
        float hullBefore = effective(stats.getHullBonus(), hullBase);
        float armorBefore = effective(stats.getArmorBonus(), armorBase);
        float arcBefore = effective(stats.getShieldArcBonus(), arcBase);
        List<WeaponAPI> ammoWeapons = ammoWeapons(module);
        float[] ammoBefore = maxAmmo(stats, ammoWeapons);

        applyShared(stats, sharedTotals, sharedModIdPrefix, false);
        applyShared(stats, sharedTotals, sharedModIdPrefix, true);
        applyModuleBonus(parentShip, stats);

        rescaleHull(module, ratio(hullBefore, effective(stats.getHullBonus(), hullBase)));
        rescaleArmor(module, ratio(armorBefore, effective(stats.getArmorBonus(), armorBase)));
        rescaleShieldArc(module.getShield(), ratio(arcBefore, effective(stats.getShieldArcBonus(), arcBase)));
        rescaleAmmo(ammoWeapons, ammoBefore, maxAmmo(stats, ammoWeapons));
        sharedTotals.forEach((effect, magnitude) -> {
            if (effect.moduleShare() == ModuleShare.SHARE_REBUILD) {
                effect.applyAfterShipCreation(module, sharedModIdPrefix + effect.name(), magnitude);
            }
        });
    }

    private static void applyShared(MutableShipStatsAPI stats, Map<SkillEffect, Float> sharedTotals, String modIdPrefix, boolean afterOtherEffects) {
        sharedTotals.forEach((effect, magnitude) -> {
            if (effect.moduleShare().isShared() && effect.appliesAfterOtherEffects() == afterOtherEffects) {
                effect.apply(stats, modIdPrefix + effect.name(), magnitude);
            }
        });
    }

    private static void applyModuleBonus(ShipAPI parentShip, MutableShipStatsAPI stats) {
        MutableShipStatsAPI parentStats = parentShip.getMutableStats();
        String modId = MODULE_BONUS_MOD_ID_PREFIX + parentShip.getId();
        modifyPercentIfSet(stats.getHullBonus(), modId, parentStats.getDynamic().getValue(HULL_PERCENT_KEY, 0f));
        modifyPercentIfSet(stats.getArmorBonus(), modId, parentStats.getDynamic().getValue(ARMOR_PERCENT_KEY, 0f));
    }

    private static void modifyPercentIfSet(StatBonus bonus, String modId, float percent) {
        if (percent != 0f) {
            bonus.modifyPercent(modId, percent);
        }
    }

    private static float shieldArcBase(ShipAPI module) {
        ShieldAPI shield = module.getShield();
        ShipHullSpecAPI hullSpec = module.getHullSpec();
        return shield == null || hullSpec.getShieldSpec() == null ? 0f : hullSpec.getShieldSpec().getArc();
    }

    private static float effective(StatBonus bonus, float base) {
        return base > 0f ? bonus.computeEffective(base) : 0f;
    }

    private static float ratio(float before, float after) {
        return before > 0f ? after / before : 1f;
    }

    private static void rescaleHull(ShipAPI module, float hullRatio) {
        if (hullRatio != 1f) {
            module.setMaxHitpoints(module.getMaxHitpoints() * hullRatio);
            module.setHitpoints(module.getHitpoints() * hullRatio);
        }
    }

    private static void rescaleArmor(ShipAPI module, float armorRatio) {
        ArmorGridAPI grid = module.getArmorGrid();
        if (armorRatio != 1f && grid != null) {
            float maxArmorInCell = grid.getMaxArmorInCell();
            ArmorCells.update(grid, current -> Math.min(current, maxArmorInCell) * armorRatio);
            module.syncWithArmorGridState();
        }
    }

    private static void rescaleShieldArc(ShieldAPI shield, float arcRatio) {
        if (arcRatio != 1f && shield != null) {
            shield.setArc(Math.min(MAX_SHIELD_ARC, shield.getArc() * arcRatio));
        }
    }

    private static List<WeaponAPI> ammoWeapons(ShipAPI module) {
        List<WeaponAPI> weapons = new ArrayList<>();
        List<WeaponAPI> allWeapons = module.getAllWeapons();
        if (allWeapons != null) {
            for (WeaponAPI weapon : allWeapons) {
                if (weapon.usesAmmo() && weapon.getSpec() != null && ammoBonus(module.getMutableStats(), weapon.getType()) != null) {
                    weapons.add(weapon);
                }
            }
        }
        return weapons;
    }

    private static float[] maxAmmo(MutableShipStatsAPI stats, List<WeaponAPI> weapons) {
        float[] maxAmmo = new float[weapons.size()];
        for (int i = 0; i < weapons.size(); i++) {
            WeaponAPI weapon = weapons.get(i);
            maxAmmo[i] = effective(ammoBonus(stats, weapon.getType()), weapon.getSpec().getMaxAmmo());
        }
        return maxAmmo;
    }

    private static StatBonus ammoBonus(MutableShipStatsAPI stats, WeaponAPI.WeaponType type) {
        if (type == null) return null;
        return switch (type) {
            case BALLISTIC -> stats.getBallisticAmmoBonus();
            case ENERGY -> stats.getEnergyAmmoBonus();
            case MISSILE -> stats.getMissileAmmoBonus();
            default -> null;
        };
    }

    private static void rescaleAmmo(List<WeaponAPI> weapons, float[] before, float[] after) {
        for (int i = 0; i < weapons.size(); i++) {
            float ammoRatio = ratio(before[i], after[i]);
            if (ammoRatio != 1f) {
                WeaponAPI weapon = weapons.get(i);
                weapon.setMaxAmmo(Math.round(weapon.getMaxAmmo() * ammoRatio));
                weapon.resetAmmo();
            }
        }
    }
}
