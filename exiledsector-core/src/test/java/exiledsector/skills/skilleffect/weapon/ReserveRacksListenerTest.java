package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReserveRacksListenerTest {

    private static WeaponAPI launcher(WeaponAPI.WeaponType type, int maxAmmo, boolean systemSlot) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.isSystemSlot()).thenReturn(systemSlot);
        when(weapon.getSlot()).thenReturn(slot);
        when(weapon.getType()).thenReturn(type);
        when(weapon.usesAmmo()).thenReturn(maxAmmo > 0);
        when(weapon.getMaxAmmo()).thenReturn(maxAmmo);
        return weapon;
    }

    private static ShipAPI shipWith(float reloadPercent, List<WeaponAPI> weapons) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(ReserveRacksListener.RELOAD_PERCENT_KEY, 0f)).thenReturn(reloadPercent);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getAllWeapons()).thenReturn(weapons);
        return ship;
    }

    @Test
    void onlyAmmoMissileWeaponsOutsideSystemSlotsHoldAReserve() {
        assertTrue(ReserveRacksListener.holdsReserve(launcher(WeaponAPI.WeaponType.MISSILE, 4, false)));
        assertFalse(ReserveRacksListener.holdsReserve(launcher(WeaponAPI.WeaponType.MISSILE, 4, true)));
        assertFalse(ReserveRacksListener.holdsReserve(launcher(WeaponAPI.WeaponType.BALLISTIC, 40, false)));
        assertFalse(ReserveRacksListener.holdsReserve(launcher(WeaponAPI.WeaponType.MISSILE, 0, false)));
    }

    @Test
    void theReloadIsAShareOfMaximumAmmoOfAtLeastOneRound() {
        assertEquals(4, ReserveRacksListener.reloadedAmmo(4, 100f));
        assertEquals(2, ReserveRacksListener.reloadedAmmo(4, 50f));
        assertEquals(1, ReserveRacksListener.reloadedAmmo(2, 10f));
        assertEquals(4, ReserveRacksListener.reloadedAmmo(4, 300f));
    }

    @Test
    void eachLauncherReloadsOnlyTheFirstTimeItRunsDry() {
        WeaponAPI launcher = launcher(WeaponAPI.WeaponType.MISSILE, 4, false);
        when(launcher.getAmmo()).thenReturn(2, 0, 0, 0);
        ReserveRacksListener listener = new ReserveRacksListener(shipWith(100f, List.of(launcher)));

        for (int i = 0; i < 4; i++) {
            listener.advance(0.5f);
        }

        verify(launcher, times(1)).setAmmo(4);
    }

    @Test
    void aShipWithoutTheEffectNeverReloads() {
        WeaponAPI launcher = launcher(WeaponAPI.WeaponType.MISSILE, 4, false);
        when(launcher.getAmmo()).thenReturn(0);
        ReserveRacksListener listener = new ReserveRacksListener(shipWith(0f, List.of(launcher)));

        listener.advance(0.5f);

        verify(launcher, never()).setAmmo(anyInt());
    }
}
