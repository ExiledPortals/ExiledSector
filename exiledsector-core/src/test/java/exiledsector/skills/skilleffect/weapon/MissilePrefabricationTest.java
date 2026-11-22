package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissilePrefabricationTest {

    private static WeaponAPI missile(boolean builtIn, float ammoPerSecond) {
        WeaponAPI weapon = mock(WeaponAPI.class, RETURNS_DEEP_STUBS);
        when(weapon.getType()).thenReturn(WeaponType.MISSILE);
        when(weapon.getSlot().isBuiltIn()).thenReturn(builtIn);
        when(weapon.usesAmmo()).thenReturn(true);
        when(weapon.getSpec().getAmmoPerSecond()).thenReturn(ammoPerSecond);
        when(weapon.getSpec().getReloadSize()).thenReturn(2f);
        when(weapon.getMaxAmmo()).thenReturn(10);
        when(weapon.getAmmo()).thenReturn(10);
        return weapon;
    }

    private static ShipAPI ship(List<WeaponAPI> weapons) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(stats.getMissileAmmoRegenMult().getModifiedValue()).thenReturn(1f);
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getAllWeapons()).thenReturn(weapons);
        Map<String, Object> customData = new HashMap<>();
        when(ship.getCustomData()).thenReturn(customData);
        doAnswer(call -> customData.put(call.getArgument(0), call.getArgument(1))).when(ship).setCustomData(anyString(), org.mockito.ArgumentMatchers.any());
        return ship;
    }

    @Test
    void extraAmmoRoundsUpToWholeReloads() {
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getAmmoPerSecond()).thenReturn(0.1f);
        when(spec.getReloadSize()).thenReturn(4f);

        assertEquals(16, MissilePrefabrication.extraAmmo(spec, 155f, 1f));
        assertEquals(32, MissilePrefabrication.extraAmmo(spec, 155f, 2f));
    }

    @Test
    void reloadingMissilesGainTheirReloadsUpFrontOnceAndStopReloading() {
        WeaponAPI missile = missile(false, 0.1f);
        WeaponAPI builtIn = missile(true, 0.1f);
        WeaponAPI noReload = missile(false, 0f);
        ShipAPI ship = ship(List.of(missile, builtIn, noReload));

        MissilePrefabrication.stock(ship, 155f);
        MissilePrefabrication.stock(ship, 155f);

        verify(missile, times(1)).setMaxAmmo(26);
        verify(missile.getAmmoTracker(), times(1)).setAmmo(26);
        verify(missile.getAmmoTracker(), times(1)).setAmmoPerSecond(0f);
        verify(builtIn, never()).setMaxAmmo(anyInt());
        verify(noReload, never()).setMaxAmmo(anyInt());
    }
}
