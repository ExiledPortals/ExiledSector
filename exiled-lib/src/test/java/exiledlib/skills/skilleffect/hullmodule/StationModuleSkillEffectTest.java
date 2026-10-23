package exiledlib.skills.skilleffect.hullmodule;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StationModuleSkillEffectTest {

    private static ShipAPI ship(String id, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    private static ShipAPI module(boolean alive) {
        ShipAPI module = mock(ShipAPI.class);
        when(module.isAlive()).thenReturn(alive);
        when(module.getHitpoints()).thenReturn(alive ? 500f : 0f);
        return module;
    }

    private static void withModules(ShipAPI ship, List<ShipAPI> modules) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        Map<String, String> stationModules = new HashMap<>();
        for (int i = 0; i < modules.size(); i++) {
            stationModules.put("WS000" + i, "plate_" + i);
        }
        when(variant.getStationModules()).thenReturn(stationModules);
        when(ship.getVariant()).thenReturn(variant);
        when(ship.getChildModulesCopy()).thenReturn(modules);
    }

    @Test
    void moduleBonusesScaleModuleHullAndArmorAndRaiseModuleWeaponStats() {
        ShipAPI plate = module(true);
        MutableShipStatsAPI plateStats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(plate.getMutableStats()).thenReturn(plateStats);
        when(plateStats.getHullBonus()).thenReturn(new StatBonus());
        when(plateStats.getArmorBonus()).thenReturn(new StatBonus());
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHitpoints()).thenReturn(1000f);
        when(hullSpec.getArmorRating()).thenReturn(400f);
        when(plate.getHullSpec()).thenReturn(hullSpec);
        when(plate.getMaxHitpoints()).thenReturn(1000f);
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getGrid()).thenReturn(new float[2][1]);
        when(grid.getMaxArmorInCell()).thenReturn(400f);
        when(grid.getArmorValue(0, 0)).thenReturn(100f);
        when(grid.getArmorValue(1, 0)).thenReturn(440f);
        when(plate.getArmorGrid()).thenReturn(grid);
        ShipAPI ship = ship("black_hawk", Map.of(ModuleBoost.HULL_PERCENT_KEY, 20f, ModuleBoost.ARMOR_PERCENT_KEY, 10f,
                ModuleBoost.WEAPON_DAMAGE_PERCENT_KEY, 15f));
        withModules(ship, List.of(plate));

        assertTrue(ModuleBoost.hasModuleBonus(ship.getMutableStats()));
        ModuleBoost.boost(ship, plate, Map.of(), "exiledSector_moduleShare_black_hawk_");

        verify(plate).setMaxHitpoints(1200f);
        verify(plate).setHitpoints(600f);
        verify(grid).setArmorValue(eq(0), eq(0), floatThat(armor -> Math.abs(armor - 110f) < 0.01f));
        verify(grid).setArmorValue(eq(1), eq(0), floatThat(armor -> Math.abs(armor - 440f) < 0.01f));
        verify(plateStats.getBallisticWeaponDamageMult()).modifyPercent("exiledSector_moduleBonus_black_hawk", 15f);
        verify(plateStats.getEnergyWeaponDamageMult()).modifyPercent("exiledSector_moduleBonus_black_hawk", 15f);
        verify(plateStats.getBallisticWeaponRangeBonus(), never()).modifyPercent(Mockito.anyString(), anyFloat());
    }
}
