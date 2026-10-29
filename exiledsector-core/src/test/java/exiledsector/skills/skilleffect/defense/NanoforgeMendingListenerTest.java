package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NanoforgeMendingListenerTest {

    private static ShipAPI shipWithDynamicValue(String key, float value) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(key, 0f)).thenReturn(value);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    @Test
    void nanoforgeMendsHullOnlyAfterGoingUndamagedForTheDelay() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 1f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(500f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);

        listener.advance(4f);
        verify(ship, never()).setHitpoints(anyFloat());

        listener.advance(1f);
        verify(ship).setHitpoints(510f);
    }

    @Test
    void nanoforgeRestartsTheDelayWhenHullDamageIsTaken() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 1f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(500f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);
        listener.advance(4f);

        when(ship.getHitpoints()).thenReturn(400f);
        listener.advance(1f);
        listener.advance(3f);

        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void nanoforgeNeverMendsPastMaximumHull() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 10f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(990f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);

        listener.advance(6f);

        verify(ship).setHitpoints(1000f);
    }

    @Test
    void nanoforgeRepairsAtMostTheShipsMaximumHullInTotalPerDeployment() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 100f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(100f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);
        listener.advance(5f);
        verify(ship).setHitpoints(1000f);

        when(ship.getHitpoints()).thenReturn(200f);
        listener.advance(1f);
        listener.advance(5f);
        verify(ship).setHitpoints(300f);

        when(ship.getHitpoints()).thenReturn(300f);
        listener.advance(10f);
        verify(ship, times(2)).setHitpoints(anyFloat());
    }
}
