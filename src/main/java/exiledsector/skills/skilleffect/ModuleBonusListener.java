package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ModuleBonusListener extends ShipCombatListener implements AdvanceableListener {

    static final String HULL_PERCENT_KEY = "exiledSector_moduleHullPercent";
    static final String ARMOR_PERCENT_KEY = "exiledSector_moduleArmorPercent";
    static final String WEAPON_DAMAGE_PERCENT_KEY = "exiledSector_moduleWeaponDamagePercent";
    static final String WEAPON_RANGE_PERCENT_KEY = "exiledSector_moduleWeaponRangePercent";
    static final String WEAPON_FIRE_RATE_PERCENT_KEY = "exiledSector_moduleWeaponFireRatePercent";
    private static final String MOD_ID_PREFIX = "exiledSector_moduleBonus_";
    private static final float CHECK_SECONDS = 1f;
    private static final StatTarget WEAPON_DAMAGE = WeaponStatFamily.DAMAGE.target(WeaponScope.ALL);
    private static final StatTarget WEAPON_RANGE = WeaponStatFamily.RANGE.target(WeaponScope.ALL);
    private static final StatTarget WEAPON_FIRE_RATE = WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL);

    private final Set<ShipAPI> boostedModules = Collections.newSetFromMap(new IdentityHashMap<>());
    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private boolean checkedOnce;
    private boolean everyModuleBoosted;

    ModuleBonusListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        if (everyModuleBoosted || !ownerIsAliveNotHulk()) {
            return;
        }
        if (checkedOnce) {
            checkInterval.advance(amount);
            if (!checkInterval.intervalElapsed()) {
                return;
            }
        }
        checkedOnce = true;
        List<ShipAPI> modules = ownerShip.getChildModulesCopy();
        if (modules != null) {
            for (ShipAPI module : modules) {
                if (CombatQueries.isAliveNotHulk(module) && boostedModules.add(module)) {
                    boost(module);
                }
            }
        }
        everyModuleBoosted = !boostedModules.isEmpty() || boostedModules.size() >= expectedModuleCount(ownerShip);
    }

    static int expectedModuleCount(ShipAPI ship) {
        Map<String, String> stationModules = ship.getVariant() == null ? null : ship.getVariant().getStationModules();
        return stationModules == null ? 0 : stationModules.size();
    }

    void boost(ShipAPI module) {
        MutableShipStatsAPI stats = module.getMutableStats();
        float hullFactor = raise(stats.getHullBonus(), module.getHullSpec().getHitpoints(), magnitude(HULL_PERCENT_KEY));
        if (hullFactor != 1f) {
            module.setMaxHitpoints(module.getMaxHitpoints() * hullFactor);
            module.setHitpoints(module.getHitpoints() * hullFactor);
        }
        float armorFactor = raise(stats.getArmorBonus(), module.getHullSpec().getArmorRating(), magnitude(ARMOR_PERCENT_KEY));
        ArmorGridAPI grid = module.getArmorGrid();
        if (armorFactor != 1f && grid != null) {
            float maxArmorInCell = grid.getMaxArmorInCell();
            ArmorCells.update(grid, current -> Math.min(current, maxArmorInCell) * armorFactor);
            module.syncWithArmorGridState();
        }
        applyIfSet(WEAPON_DAMAGE, stats, magnitude(WEAPON_DAMAGE_PERCENT_KEY));
        applyIfSet(WEAPON_RANGE, stats, magnitude(WEAPON_RANGE_PERCENT_KEY));
        applyIfSet(WEAPON_FIRE_RATE, stats, magnitude(WEAPON_FIRE_RATE_PERCENT_KEY));
    }

    private float raise(StatBonus bonus, float base, float percent) {
        if (percent == 0f || base <= 0f) {
            return 1f;
        }
        float before = bonus.computeEffective(base);
        bonus.modifyPercent(modId, percent);
        float after = bonus.computeEffective(base);
        return before > 0f ? after / before : 1f;
    }

    private void applyIfSet(StatTarget target, MutableShipStatsAPI stats, float percent) {
        if (percent != 0f) {
            target.apply(stats, modId, StatMode.PERCENT, percent);
        }
    }
}
