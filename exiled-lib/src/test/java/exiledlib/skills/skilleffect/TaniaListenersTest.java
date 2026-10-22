package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaniaListenersTest {

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

    private static WeaponAPI weapon(boolean hardpoint, boolean decorative, boolean systemSlot) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.isHardpoint()).thenReturn(hardpoint);
        when(slot.isSystemSlot()).thenReturn(systemSlot);
        when(weapon.getSlot()).thenReturn(slot);
        when(weapon.isDecorative()).thenReturn(decorative);
        return weapon;
    }

    @Test
    void thermoAcceleratorCutsWeaponFluxCostAndKeepsOnlyRealHardpointsFromFiring() {
        WeaponAPI hardpoint = weapon(true, false, false);
        WeaponAPI turret = weapon(false, false, false);
        WeaponAPI decorative = weapon(true, true, false);
        WeaponAPI system = weapon(true, false, true);
        ShipAPI ship = ship("aetos", Map.of(ThermoAcceleratorListener.FLUX_COST_REDUCTION_PERCENT_KEY, 50f));
        when(ship.getAllWeapons()).thenReturn(List.of(hardpoint, turret, decorative, system));

        ThermoAcceleratorListener listener = new ThermoAcceleratorListener(ship);
        listener.advance(0.1f);

        String modId = "exiledSector_thermoAccelerator_aetos";
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod()).modifyPercent(modId, -50f);
        verify(ship.getMutableStats().getEnergyWeaponFluxCostMod()).modifyPercent(modId, -50f);
        verify(ship.getMutableStats().getMissileWeaponFluxCostMod()).modifyPercent(modId, -50f);
        verify(hardpoint).setForceNoFireOneFrame(true);
        verify(turret, never()).setForceNoFireOneFrame(true);
        verify(decorative, never()).setForceNoFireOneFrame(true);
        verify(system, never()).setForceNoFireOneFrame(true);
    }

    @Test
    void moduleBonusesScaleModuleHullAndArmor() {
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
        ShipAPI ship = ship("black_hawk", Map.of(ModuleBoost.HULL_PERCENT_KEY, 20f, ModuleBoost.ARMOR_PERCENT_KEY, 10f));
        withModules(ship, List.of(plate));

        assertTrue(ModuleBoost.hasModuleBonus(ship.getMutableStats()));
        ModuleBoost.boost(ship, plate, Map.of(), "exiledSector_moduleShare_black_hawk_");

        verify(plate).setMaxHitpoints(1200f);
        verify(plate).setHitpoints(600f);
        verify(grid).setArmorValue(eq(0), eq(0), floatThat(armor -> Math.abs(armor - 110f) < 0.01f));
        verify(grid).setArmorValue(eq(1), eq(0), floatThat(armor -> Math.abs(armor - 440f) < 0.01f));
    }

    @Test
    void riseFromTheAshesCancelsOneLethalHitPerShipPerBattle() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        Map<String, Object> customData = new HashMap<>();
        when(engine.getCustomData()).thenReturn(customData);
        ShipAPI ship = ship("sandgrouse", Map.of(RiseFromTheAshesListener.HULL_PERCENT_KEY, 25f,
                RiseFromTheAshesListener.DAMAGE_TAKEN_PERCENT_KEY, -50f));
        when(ship.getFleetMemberId()).thenReturn("member_7");
        when(ship.getMaxHitpoints()).thenReturn(2000f);
        when(ship.getHitpoints()).thenReturn(100f);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            RiseFromTheAshesListener listener = new RiseFromTheAshesListener(ship);

            assertFalse(listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 50f));
            assertTrue(listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 300f));
            verify(ship).setHitpoints(500f);
            verify(ship.getMutableStats().getHullDamageTakenMult()).modifyMult("exiledSector_riseFromTheAshes_sandgrouse", 0.5f);
            assertTrue(listener.rising());
            assertTrue(customData.containsKey("exiledSector_riseFromTheAshesUsed_member_7"));

            assertFalse(new RiseFromTheAshesListener(ship).notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 300f));

            listener.advance(RiseFromTheAshesListener.DURATION_SECONDS + 0.1f);
            assertFalse(listener.rising());
            verify(ship.getMutableStats().getHullDamageTakenMult()).unmodify("exiledSector_riseFromTheAshes_sandgrouse");
            verify(ship, never()).setJitterUnder(Mockito.any(), Mockito.any(), anyFloat(), anyInt(), anyFloat(), anyFloat());
        }
    }

    @Test
    void solarisOverdriveFreesWeaponFluxAndSpeedsProjectilesAfterTheSystemStartsThenWaitsOutItsCooldown() {
        ShipSystemAPI system = mock(ShipSystemAPI.class);
        ShipAPI ship = ship("aracari", Map.of(SolarisOverdriveListener.SECONDS_KEY, 4f,
                SolarisOverdriveListener.PROJECTILE_SPEED_PERCENT_KEY, 50f));
        when(ship.getSystem()).thenReturn(system);
        SolarisOverdriveListener listener = new SolarisOverdriveListener(ship);
        String modId = "exiledSector_solarisOverdrive_aracari";

        listener.advance(0.1f);
        assertFalse(listener.overdriven());
        when(system.isActive()).thenReturn(true);
        listener.advance(0.1f);
        assertTrue(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod()).modifyMult(modId, 0f);
        verify(ship.getMutableStats().getEnergyWeaponFluxCostMod()).modifyMult(modId, 0f);
        verify(ship.getMutableStats().getBallisticProjectileSpeedMult()).modifyPercent(modId, 50f);

        listener.advance(4f);
        assertFalse(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod()).unmodify(modId);
        verify(ship.getMutableStats().getBallisticProjectileSpeedMult()).unmodify(modId);

        when(system.isActive()).thenReturn(false);
        listener.advance(0.1f);
        when(system.isActive()).thenReturn(true);
        listener.advance(0.1f);
        assertFalse(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod(), times(1)).modifyMult(modId, 0f);
    }

    @Test
    void hullModInteractionsNeedTheHullModAndBuiltInOnesNeedItAsAnSMod() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.hasHullMod("ballistic_rangefinder")).thenReturn(true);
        when(variant.hasHullMod("blast_doors")).thenReturn(true);
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("additional_berthing")));

        HullModInteractionEffect.RANGEFINDER_BALLISTIC_WEAPON_DAMAGE_PERCENT.apply(stats, "frame", 5f);
        HullModInteractionEffect.ECM_BALLISTIC_WEAPON_RANGE_PERCENT.apply(stats, "frame", 5f);
        HullModInteractionEffect.BUILT_IN_BLAST_DOORS_HULL_PERCENT.apply(stats, "frame", 12.5f);
        HullModInteractionEffect.BUILT_IN_BERTHING_PEAK_CR_PERCENT.apply(stats, "frame", 12.5f);

        verify(stats.getBallisticWeaponDamageMult()).modifyPercent("frame", 5f);
        verify(stats.getBallisticWeaponRangeBonus()).unmodify("frame");
        verify(stats.getHullBonus()).unmodify("frame");
        verify(stats.getPeakCRDuration()).modifyPercent("frame", 12.5f);
    }

    @Test
    void builtInAuxiliaryThrustersAddTopSpeedOnlyDuringTheZeroFluxBoost() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship("sandgrouse", Map.of());
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("auxiliarythrusters")));
        when(ship.getVariant()).thenReturn(variant);
        when(ship.getFluxTracker()).thenReturn(flux);
        HullModInteractionEffect effect = HullModInteractionEffect.BUILT_IN_AUX_THRUSTERS_ZERO_FLUX_TOP_SPEED_PERCENT;

        assertTrue(effect.advancesInCombat());
        effect.advanceInCombat(ship, "frame", 7.5f);
        verify(ship.getMutableStats().getMaxSpeed()).unmodify("frame");
        when(flux.isEngineBoostActive()).thenReturn(true);
        effect.advanceInCombat(ship, "frame", 7.5f);
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent("frame", 7.5f);
    }
}
