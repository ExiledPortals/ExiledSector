package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmmoSpentTrackerTest {

    private static WeaponAPI weapon(int ammo, int maxAmmo, AmmoTrackerAPI ammoTracker) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getAmmo()).thenReturn(ammo);
        when(weapon.getMaxAmmo()).thenReturn(maxAmmo);
        when(weapon.getAmmoTracker()).thenReturn(ammoTracker);
        return weapon;
    }

    @Test
    void countsTheShotsSpentSinceTheLastCheck() {
        WeaponAPI weapon = weapon(10, 10, null);
        AmmoSpentTracker tracker = new AmmoSpentTracker(List.of(weapon));

        when(weapon.getAmmo()).thenReturn(7);
        assertEquals(3, tracker.spentSinceLastCheck(0));
        assertEquals(0, tracker.spentSinceLastCheck(0));
    }

    @Test
    void aReloadInTheSameFrameStillCountsTheShotsFired() {
        AmmoTrackerAPI ammoTracker = mock(AmmoTrackerAPI.class);
        when(ammoTracker.getReloadProgress()).thenReturn(0.9f);
        when(ammoTracker.getReloadSize()).thenReturn(3f);
        WeaponAPI weapon = weapon(5, 10, ammoTracker);
        AmmoSpentTracker tracker = new AmmoSpentTracker(List.of(weapon));

        when(ammoTracker.getReloadProgress()).thenReturn(0.1f);
        when(weapon.getAmmo()).thenReturn(7);

        assertEquals(1, tracker.spentByAllSinceLastCheck());
    }

    @Test
    void firingAsManyShotsAsTheReloadInItsFrameCountsEveryShot() {
        AmmoTrackerAPI ammoTracker = mock(AmmoTrackerAPI.class);
        when(ammoTracker.getReloadProgress()).thenReturn(0.9f);
        when(ammoTracker.getReloadSize()).thenReturn(3f);
        WeaponAPI weapon = weapon(5, 10, ammoTracker);
        AmmoSpentTracker tracker = new AmmoSpentTracker(List.of(weapon));

        when(ammoTracker.getReloadProgress()).thenReturn(0.1f);
        when(weapon.getAmmo()).thenReturn(5);

        assertEquals(3, tracker.spentSinceLastCheck(0));
    }

    @Test
    void aFractionalReloadAddsOnlyTheWholeRoundsTheEngineAdds() {
        AmmoTrackerAPI ammoTracker = mock(AmmoTrackerAPI.class);
        when(ammoTracker.getReloadProgress()).thenReturn(0.9f);
        when(ammoTracker.getReloadSize()).thenReturn(2.6f);
        WeaponAPI weapon = weapon(5, 10, ammoTracker);
        AmmoSpentTracker tracker = new AmmoSpentTracker(List.of(weapon));

        when(ammoTracker.getReloadProgress()).thenReturn(0.1f);
        when(weapon.getAmmo()).thenReturn(7);

        assertEquals(0, tracker.spentSinceLastCheck(0));
    }

    @Test
    void reloadProgressResettingAtFullAmmoIsNotAReload() {
        AmmoTrackerAPI ammoTracker = mock(AmmoTrackerAPI.class);
        when(ammoTracker.getReloadProgress()).thenReturn(0.4f);
        when(ammoTracker.getReloadSize()).thenReturn(3f);
        WeaponAPI weapon = weapon(10, 10, ammoTracker);
        AmmoSpentTracker tracker = new AmmoSpentTracker(List.of(weapon));

        when(ammoTracker.getReloadProgress()).thenReturn(0f);
        when(weapon.getAmmo()).thenReturn(9);

        assertEquals(1, tracker.spentSinceLastCheck(0));
    }

    @Test
    void aRefundPutsAmmoBackUpToTheMaximumWithoutCountingAsSpent() {
        WeaponAPI weapon = weapon(10, 10, null);
        AmmoSpentTracker tracker = new AmmoSpentTracker(List.of(weapon));
        when(weapon.getAmmo()).thenReturn(8);
        tracker.spentSinceLastCheck(0);

        tracker.refund(0, 5);

        verify(weapon).setAmmo(10);
        when(weapon.getAmmo()).thenReturn(10);
        assertEquals(0, tracker.spentSinceLastCheck(0));
    }
}
