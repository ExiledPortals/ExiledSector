package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnergySlotRangeListenerTest {

    private final List<Object> gridShips = new ArrayList<>();

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    private CombatEngineAPI combatEngine;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        combatEngine = engine;
        combat.shipGridOver(gridShips);
    }

    private static ShipAPI ship(String id, float x, Map<String, Float> dynamicValues) {
        ShipAPI ship = CombatTestShips.ship().id(id).dynamic(dynamicValues).at(new Vector2f(x, 0f)).alive().build();
        when(ship.getVelocity()).thenReturn(new Vector2f());
        return ship;
    }

    private static WeaponAPI weaponIn(WeaponAPI.WeaponType slotType, WeaponAPI.WeaponSize slotSize) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.getWeaponType()).thenReturn(slotType);
        when(slot.getSlotSize()).thenReturn(slotSize);
        when(weapon.getSlot()).thenReturn(slot);
        return weapon;
    }

    @Test
    void onlyWeaponsInMediumEnergySlotsGainTheFlatRange() {
        ShipAPI ship = ship("a", 0f, Map.of(EnergySlotRangeListener.MEDIUM_SLOT_RANGE_FLAT_KEY, 100f));
        EnergySlotRangeListener listener = new EnergySlotRangeListener();

        assertEquals(100f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.SMALL)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, mock(WeaponAPI.class)));
    }

    @Test
    void theAnySizeRangeReachesEveryEnergySlotAndStacksWithTheMediumOne() {
        ShipAPI ship = ship("a", 0f, Map.of(EnergySlotRangeListener.MEDIUM_SLOT_RANGE_FLAT_KEY, 100f,
                EnergySlotRangeListener.ANY_SLOT_RANGE_FLAT_KEY, 50f));
        EnergySlotRangeListener listener = new EnergySlotRangeListener();

        assertEquals(50f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.SMALL)));
        assertEquals(150f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(50f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.LARGE)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.HYBRID, WeaponAPI.WeaponSize.LARGE)));
    }
}
