package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullDamageAllyRepairListenerTest {

    private static ShipAPI ship(int owner, float hitpoints, float maxHitpoints) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.getHitpoints()).thenReturn(hitpoints);
        when(ship.getMaxHitpoints()).thenReturn(maxHitpoints);
        return ship;
    }

    @Test
    void hullDamageToEnemiesBanksItsShareAndDamageToAlliesDoesNot() {
        ShipAPI ownerShip = ship(0, 1000f, 1000f);
        when(ownerShip.getMutableStats().getDynamic().getValue(HullDamageAllyRepairListener.REPAIR_PERCENT_KEY, 0f)).thenReturn(10f);
        HullDamageAllyRepairListener listener = new HullDamageAllyRepairListener(ownerShip);

        listener.onHullDamageDealt(ship(1, 500f, 1000f), 300f);
        listener.onHullDamageDealt(ship(0, 500f, 1000f), 300f);

        assertEquals(30f, listener.pendingRepair(), 0.001f);
    }

    @Test
    void theAllyMissingTheMostHullIsRepairedUpToItsMaximum() {
        ShipAPI scratched = ship(0, 900f, 1000f);
        ShipAPI wrecked = ship(0, 4950f, 5000f);
        ShipAPI battered = ship(0, 200f, 1000f);

        HullDamageAllyRepairListener.repairMostDamaged(List.of(scratched, wrecked, battered), 1000f);

        verify(battered).setHitpoints(1000f);
        verify(scratched, never()).setHitpoints(anyFloat());
        verify(wrecked, never()).setHitpoints(anyFloat());
    }

    @Test
    void anUndamagedFleetTakesNoRepair() {
        ShipAPI healthy = ship(0, 1000f, 1000f);

        HullDamageAllyRepairListener.repairMostDamaged(List.of(healthy), 50f);

        verify(healthy, never()).setHitpoints(anyFloat());
    }
}
