package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediumEnergySlotRangeListenerTest {

    private final List<Object> gridShips = new ArrayList<>();

    private MockedStatic<Global> globalMock;

    private CombatEngineAPI combatEngine;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        combatEngine = engine;
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String id, float x, Map<String, Float> dynamicValues) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        dynamicValues.forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
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
        ShipAPI ship = ship("a", 0f, Map.of(MediumEnergySlotRangeListener.RANGE_FLAT_KEY, 100f));
        MediumEnergySlotRangeListener listener = new MediumEnergySlotRangeListener();

        assertEquals(100f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.SMALL)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, mock(WeaponAPI.class)));
    }
}
