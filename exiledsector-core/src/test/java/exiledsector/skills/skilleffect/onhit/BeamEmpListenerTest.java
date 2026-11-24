package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeamEmpListenerTest {

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatListenerManagerAPI listeners;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(any())).thenReturn(List.of());
    }

    private static ShipAPI ship(int owner, float empPercent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats().getDynamic().getValue(BeamEmpListener.EMP_PERCENT_KEY, 0f)).thenReturn(empPercent);
        return ship;
    }

    private static DamageAPI beamDamage(float baseDamage, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getType()).thenReturn(DamageType.ENERGY);
        when(damage.getBaseDamage()).thenReturn(baseDamage);
        when(damage.getFluxComponent()).thenReturn(emp);
        return damage;
    }

    @Test
    void beamHitsOnArmorAndHullAddEmpFromTheirBaseDamageForThatHitOnly() {
        ShipAPI target = ship(1, 0f);
        DamageAPI damage = beamDamage(200f, 10f);

        assertNull(new BeamEmpListener(ship(0, 8f)).modifyDamageDealt(mock(BeamAPI.class), target, damage, new Vector2f(), false));

        verify(damage).setFluxComponent(26f);
        ArgumentCaptor<Object> added = ArgumentCaptor.forClass(Object.class);
        verify(listeners, atLeastOnce()).addListener(added.capture());
        List<Object> restorers = added.getAllValues().stream().filter(HitDamageRestorer.class::isInstance).toList();
        ((DamageListener) restorers.get(restorers.size() - 1)).reportDamageApplied(null, target, mock(ApplyDamageResultAPI.class));
        verify(damage).setFluxComponent(10f);
    }

    @Test
    void shieldHitsProjectilesAndAlliesGetNoExtraEmp() {
        BeamEmpListener listener = new BeamEmpListener(ship(0, 8f));
        DamageAPI damage = beamDamage(200f, 0f);

        listener.modifyDamageDealt(mock(BeamAPI.class), ship(1, 0f), damage, new Vector2f(), true);
        listener.modifyDamageDealt(mock(DamagingProjectileAPI.class), ship(1, 0f), damage, new Vector2f(), false);
        listener.modifyDamageDealt(mock(BeamAPI.class), ship(0, 0f), damage, new Vector2f(), false);

        verify(damage, never()).setFluxComponent(anyFloat());
    }
}
