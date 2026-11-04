package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlowerShipDamageListenerTest {

    private static ShipAPI ship(String id, int owner, float topSpeed) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMaxSpeed()).thenReturn(topSpeed);
        when(ship.getMutableStats().getTimeMult().getModifiedValue()).thenReturn(1f);
        when(ship.getEngineController().isFlamedOut()).thenReturn(false);
        return ship;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private static SlowerShipDamageListener listener(float topSpeed, float percent) {
        ShipAPI owner = ship("hunter", 0, topSpeed);
        when(owner.getMutableStats().getDynamic().getValue(SlowerShipDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return new SlowerShipDamageListener(owner);
    }

    @Test
    void hitsOnSlowerEnemyShipsDealTheBonusDamage() {
        DamageAPI hit = damage();

        String modId = listener(120f, 8f).modifyDamageDealt(null, ship("prey", 1, 80f), hit, new Vector2f(), false);

        assertEquals("exiledSector_slowerShipDamage_hunter", modId);
        verify(hit.getModifier()).modifyPercent(modId, 8f);
    }

    @Test
    void flamedOutAndTimeDilatedTargetsCountAsSlower() {
        ShipAPI flamedOut = ship("flamed out", 1, 200f);
        when(flamedOut.getEngineController().isFlamedOut()).thenReturn(true);
        ShipAPI dilated = ship("dilated", 1, 150f);
        when(dilated.getMutableStats().getTimeMult().getModifiedValue()).thenReturn(0.5f);
        SlowerShipDamageListener listener = listener(120f, 8f);

        DamageAPI flamedOutHit = damage();
        DamageAPI dilatedHit = damage();

        String flamedOutModId = listener.modifyDamageDealt(null, flamedOut, flamedOutHit, new Vector2f(), false);
        String dilatedModId = listener.modifyDamageDealt(null, dilated, dilatedHit, new Vector2f(), false);

        assertEquals(0f, SlowerShipDamageListener.currentTopSpeed(flamedOut));
        assertEquals(75f, SlowerShipDamageListener.currentTopSpeed(dilated), 0.001f);
        verify(flamedOutHit.getModifier()).modifyPercent(flamedOutModId, 8f);
        verify(dilatedHit.getModifier()).modifyPercent(dilatedModId, 8f);
    }

    @Test
    void fasterOrEqualShipsStrikecraftAndAlliesTakeNormalDamage() {
        SlowerShipDamageListener listener = listener(120f, 8f);
        DamageAPI hit = damage();
        ShipAPI fighter = ship("fighter", 1, 50f);
        when(fighter.isFighter()).thenReturn(true);

        assertNull(listener.modifyDamageDealt(null, ship("faster", 1, 150f), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("equal", 1, 120f), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, fighter, hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("ally", 0, 50f), hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
