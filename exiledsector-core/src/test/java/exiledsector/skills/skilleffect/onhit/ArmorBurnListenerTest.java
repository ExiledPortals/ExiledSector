package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmorBurnListenerTest {

    private static ShipAPI burningShip() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(ArmorBurnListener.BURN_PERCENT_KEY, 0f)).thenReturn(5f);
        when(ship.getOwner()).thenReturn(0);
        return ship;
    }

    private static ShipAPI target(ArmorGridAPI grid, Vector2f point, int owner) {
        ShipAPI target = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat armorTaken = mock(MutableStat.class);
        when(armorTaken.getModifiedValue()).thenReturn(1f);
        when(stats.getArmorDamageTakenMult()).thenReturn(armorTaken);
        when(target.getMutableStats()).thenReturn(stats);
        when(target.getOwner()).thenReturn(owner);
        when(target.getArmorGrid()).thenReturn(grid);
        when(grid.getGrid()).thenReturn(new float[5][5]);
        when(grid.getCellAtLocation(point)).thenReturn(new int[]{2, 2});
        when(grid.getArmorValue(anyInt(), anyInt())).thenReturn(100f);
        return target;
    }

    private static DamagingProjectileAPI shot() {
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        when(shot.getWeapon()).thenReturn(mock(WeaponAPI.class));
        return shot;
    }

    private static DamageAPI damage(float amount) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(amount);
        return damage;
    }

    private static ArmorDecay decay(ShipAPI target) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(target).addListener(captor.capture());
        ArmorDecay armorDecay = (ArmorDecay) captor.getValue();
        when(target.getListeners(ArmorDecay.class)).thenReturn(List.of(armorDecay));
        return armorDecay;
    }

    @Test
    void aWeaponHitBurnsOnlyTheStruckCellOverThreeSecondsAndBurnsAddUp() {
        Vector2f point = new Vector2f();
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        ShipAPI enemy = target(grid, point, 1);
        ArmorBurnListener listener = new ArmorBurnListener(burningShip());

        assertNull(listener.modifyDamageDealt(shot(), enemy, damage(300f), point, false));
        ArmorDecay armorDecay = decay(enemy);
        listener.modifyDamageDealt(shot(), enemy, damage(300f), point, false);

        armorDecay.advance(ArmorBurnListener.BURN_SECONDS / 3f);
        verify(grid).setArmorValue(2, 2, 90f);
        verify(grid, never()).setArmorValue(eq(1), eq(2), anyFloat());
    }

    @Test
    void shieldHitsAlliesAndHitsWithoutAWeaponNeverBurn() {
        Vector2f point = new Vector2f();
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        ShipAPI enemy = target(grid, point, 1);
        ShipAPI ally = target(grid, point, 0);
        ArmorBurnListener listener = new ArmorBurnListener(burningShip());

        listener.modifyDamageDealt(shot(), enemy, damage(300f), point, true);
        listener.modifyDamageDealt(shot(), ally, damage(300f), point, false);
        listener.modifyDamageDealt(mock(ShipAPI.class), enemy, damage(300f), point, false);

        verify(enemy, never()).addListener(any());
        verify(ally, never()).addListener(any());
    }
}
