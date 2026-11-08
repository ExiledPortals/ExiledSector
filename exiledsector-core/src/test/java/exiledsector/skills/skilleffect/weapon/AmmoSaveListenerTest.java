package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmmoSaveListenerTest {

    private static final class Magazine {
        private int ammo;

        Magazine(int ammo) {
            this.ammo = ammo;
        }
    }

    private static WeaponAPI weapon(WeaponType type, boolean usesAmmo, Magazine magazine, int maxAmmo) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.usesAmmo()).thenReturn(usesAmmo);
        when(weapon.getMaxAmmo()).thenReturn(maxAmmo);
        when(weapon.getAmmo()).thenAnswer(invocation -> magazine.ammo);
        doAnswer(invocation -> {
            magazine.ammo = invocation.getArgument(0);
            return null;
        }).when(weapon).setAmmo(anyInt());
        return weapon;
    }

    private static ShipAPI ship(float chancePercent, List<WeaponAPI> weapons) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.getAllWeapons()).thenReturn(weapons);
        when(ship.getMutableStats().getDynamic().getValue(AmmoSaveListener.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        return ship;
    }

    private static Random rolls(float first, float... rest) {
        Random random = mock(Random.class);
        Float[] boxed = new Float[rest.length];
        for (int i = 0; i < rest.length; i++) {
            boxed[i] = rest[i];
        }
        when(random.nextFloat()).thenReturn(first, boxed);
        return random;
    }

    @Test
    void onlyBallisticAndEnergyWeaponsThatUseAmmoQualify() {
        assertTrue(AmmoSaveListener.savesAmmo(weapon(WeaponType.BALLISTIC, true, new Magazine(10), 10)));
        assertTrue(AmmoSaveListener.savesAmmo(weapon(WeaponType.ENERGY, true, new Magazine(10), 10)));
        assertFalse(AmmoSaveListener.savesAmmo(weapon(WeaponType.MISSILE, true, new Magazine(10), 10)));
        assertFalse(AmmoSaveListener.savesAmmo(weapon(WeaponType.BALLISTIC, false, new Magazine(0), 0)));
    }

    @Test
    void eachShotFiredRollsToHaveItsAmmoRefunded() {
        Magazine magazine = new Magazine(10);
        WeaponAPI ballistic = weapon(WeaponType.BALLISTIC, true, magazine, 10);
        AmmoSaveListener listener = new AmmoSaveListener(ship(10f, List.of(ballistic)), rolls(0.05f, 0.5f, 0.09f));

        listener.advance(0.1f);
        magazine.ammo = 7;
        listener.advance(0.1f);

        assertEquals(9, magazine.ammo);

        listener.advance(0.1f);

        assertEquals(9, magazine.ammo);
    }

    @Test
    void aRefundNeverExceedsTheMagazine() {
        Magazine magazine = new Magazine(10);
        WeaponAPI energy = weapon(WeaponType.ENERGY, true, magazine, 9);
        AmmoSaveListener listener = new AmmoSaveListener(ship(100f, List.of(energy)), rolls(0f));

        listener.advance(0.1f);
        magazine.ammo = 8;
        listener.advance(0.1f);

        assertEquals(9, magazine.ammo);
    }

    @Test
    void missileWeaponsAndReloadsAreLeftAlone() {
        Magazine missileMagazine = new Magazine(4);
        Magazine ballisticMagazine = new Magazine(5);
        WeaponAPI missile = weapon(WeaponType.MISSILE, true, missileMagazine, 4);
        WeaponAPI ballistic = weapon(WeaponType.BALLISTIC, true, ballisticMagazine, 10);
        AmmoSaveListener listener = new AmmoSaveListener(ship(100f, List.of(missile, ballistic)), rolls(0f));

        listener.advance(0.1f);
        missileMagazine.ammo = 3;
        ballisticMagazine.ammo = 6;
        listener.advance(0.1f);

        verify(missile, never()).setAmmo(anyInt());
        verify(ballistic, never()).setAmmo(anyInt());
        assertEquals(3, missileMagazine.ammo);
        assertEquals(6, ballisticMagazine.ammo);
    }
}
