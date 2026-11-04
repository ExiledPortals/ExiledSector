package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedTargetDamageListenerTest {

    private static ShipAPI ship(String id, int owner) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getParentStation()).thenReturn(null);
        return ship;
    }

    private static ShipAPI owner(float percent) {
        ShipAPI owner = ship("owner", 0);
        when(owner.getMutableStats().getDynamic().getValue(SharedTargetDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return owner;
    }

    @Test
    void hitsOnAnEnemyAnAllyHasTargetedDealIncreasedDamage() {
        ShipAPI owner = owner(8f);
        ShipAPI enemy = ship("enemy", 1);
        ShipAPI ally = ship("ally", 0);
        when(ally.getShipTarget()).thenReturn(enemy);
        SharedTargetDamageListener listener = new SharedTargetDamageListener(owner);
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);

        listener.scan(List.of(owner, ally, enemy));

        assertEquals("exiledSector_sharedTargetDamage_owner", listener.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        verify(damage.getModifier()).modifyPercent("exiledSector_sharedTargetDamage_owner", 8f);
    }

    @Test
    void theOwnersOwnTargetAndStrikecraftTargetsDontCount() {
        ShipAPI owner = owner(8f);
        ShipAPI enemy = ship("enemy", 1);
        ShipAPI fighter = ship("fighter", 0);
        when(fighter.isFighter()).thenReturn(true);
        when(fighter.getShipTarget()).thenReturn(enemy);
        when(owner.getShipTarget()).thenReturn(enemy);
        SharedTargetDamageListener listener = new SharedTargetDamageListener(owner);
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);

        listener.scan(List.of(owner, fighter, enemy));

        assertFalse(listener.isShared(enemy));
        assertNull(listener.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        verify(damage.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aModuleOfATargetedStationCountsAsShared() {
        ShipAPI owner = owner(8f);
        ShipAPI station = ship("station", 1);
        ShipAPI module = ship("module", 1);
        when(module.getParentStation()).thenReturn(station);
        ShipAPI ally = ship("ally", 0);
        when(ally.getShipTarget()).thenReturn(station);
        SharedTargetDamageListener listener = new SharedTargetDamageListener(owner);

        listener.scan(List.of(owner, ally, station, module));

        assertTrue(listener.isShared(module));
    }

    @Test
    void aDeadOwnerForgetsItsSharedTargets() {
        ShipAPI owner = owner(8f);
        ShipAPI enemy = ship("enemy", 1);
        ShipAPI ally = ship("ally", 0);
        when(ally.getShipTarget()).thenReturn(enemy);
        SharedTargetDamageListener listener = new SharedTargetDamageListener(owner);
        listener.scan(List.of(owner, ally, enemy));

        when(owner.isAlive()).thenReturn(false);
        listener.scan(List.of(owner, ally, enemy));

        assertFalse(listener.isShared(enemy));
    }
}
