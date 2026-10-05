package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissileReloaderTest {

    private final SkillEffect reload = SkillEffect.byName("MISSILE_RELOAD_PERCENT_PER_MINUTE");
    private final List<WeaponAPI> weapons = new ArrayList<>();
    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        ship = mock(ShipAPI.class);
        Map<String, Object> customData = new HashMap<>();
        when(ship.getCustomData()).thenReturn(customData);
        doAnswer(call -> customData.put(call.getArgument(0), call.getArgument(1))).when(ship).setCustomData(anyString(), any());
        when(ship.isAlive()).thenReturn(true);
        when(ship.getAllWeapons()).thenReturn(weapons);
    }

    private AtomicInteger weapon(WeaponType type, int baseAmmo, int maxAmmo, int ammo, float ammoPerSecond) {
        AtomicInteger current = new AtomicInteger(ammo);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getMaxAmmo()).thenReturn(baseAmmo);
        when(spec.getAmmoPerSecond()).thenReturn(ammoPerSecond);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.usesAmmo()).thenReturn(true);
        when(weapon.getSpec()).thenReturn(spec);
        when(weapon.getSlot()).thenReturn(slot);
        when(weapon.getMaxAmmo()).thenReturn(maxAmmo);
        when(weapon.getAmmo()).thenAnswer(call -> current.get());
        doAnswer(call -> {
            current.set(call.getArgument(0));
            return null;
        }).when(weapon).setAmmo(anyInt());
        weapons.add(weapon);
        return current;
    }

    private void advanceSeconds(String modId, float magnitude, int seconds) {
        for (int i = 0; i < seconds; i++) {
            reload.advanceInCombat(ship, modId, magnitude, 1f);
        }
    }

    @Test
    void itIsAConditionalEffectSoItRunsEveryCombatFrame() {
        assertTrue(reload.isConditional());
    }

    @Test
    void aLauncherReloadsTheGivenShareOfItsBaseAmmoEachMinute() {
        AtomicInteger ammo = weapon(WeaponType.MISSILE, 20, 40, 5, 0f);

        advanceSeconds("node", 10f, 59);
        assertEquals(6, ammo.get());
        advanceSeconds("node", 10f, 2);
        assertEquals(7, ammo.get());
    }

    @Test
    void reloadingStopsAtMaxAmmoAndKeepsNoBankedProgress() {
        AtomicInteger ammo = weapon(WeaponType.MISSILE, 20, 6, 5, 0f);

        advanceSeconds("node", 10f, 600);
        assertEquals(6, ammo.get());
        ammo.set(5);
        advanceSeconds("node", 10f, 29);
        assertEquals(5, ammo.get());
    }

    @Test
    void regeneratingMissilesAndOtherWeaponTypesAreLeftAlone() {
        AtomicInteger regenerating = weapon(WeaponType.MISSILE, 20, 20, 0, 0.1f);
        AtomicInteger ballistic = weapon(WeaponType.BALLISTIC, 20, 20, 0, 0f);
        AtomicInteger energy = weapon(WeaponType.ENERGY, 20, 20, 0, 0f);

        advanceSeconds("node", 100f, 120);

        assertEquals(0, regenerating.get());
        assertEquals(0, ballistic.get());
        assertEquals(0, energy.get());
    }

    @Test
    void nothingReloadsWhilePausedOrOnceTheShipIsAHulk() {
        AtomicInteger ammo = weapon(WeaponType.MISSILE, 20, 20, 0, 0f);

        for (int i = 0; i < 100; i++) {
            reload.advanceInCombat(ship, "node", 100f, 0f);
        }
        assertEquals(0, ammo.get());
        when(ship.isHulk()).thenReturn(true);
        advanceSeconds("node", 100f, 120);
        assertEquals(0, ammo.get());
    }

    @Test
    void separateSourcesStack() {
        AtomicInteger ammo = weapon(WeaponType.MISSILE, 20, 40, 0, 0f);

        for (int i = 0; i < 61; i++) {
            reload.advanceInCombat(ship, "node", 10f, 1f);
            reload.advanceInCombat(ship, "socket", 10f, 1f);
        }

        assertEquals(4, ammo.get());
    }

    @Test
    void decorativeAndSystemSlotsNeverReload() {
        weapon(WeaponType.MISSILE, 20, 20, 0, 0f);
        WeaponAPI systemLauncher = weapons.get(0);
        when(systemLauncher.getSlot().isSystemSlot()).thenReturn(true);

        assertFalse(MissileReloader.reloads(systemLauncher));
    }
}
