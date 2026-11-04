package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeamEmpListenerTest {

    private MockedStatic<Global> globalMock;
    private CombatListenerManagerAPI listeners;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(any())).thenReturn(List.of());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(int owner, float empPercent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats().getDynamic().getValue(BeamEmpListener.EMP_PERCENT_KEY, 0f)).thenReturn(empPercent);
        return ship;
    }

    private static DamageAPI beamDamage(float baseDamage, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(baseDamage);
        when(damage.getFluxComponent()).thenReturn(emp);
        return damage;
    }

    @Test
    void beamHitsOnArmorAndHullAddEmpForThatHitOnly() {
        ShipAPI target = ship(1, 0f);
        DamageAPI damage = beamDamage(200f, 10f);

        assertNull(new BeamEmpListener(ship(0, 8f)).modifyDamageDealt(mock(BeamAPI.class), target, damage, new Vector2f(), false));

        verify(damage).setFluxComponent(26f);
        ArgumentCaptor<Object> restorer = ArgumentCaptor.forClass(Object.class);
        verify(listeners).addListener(restorer.capture());
        ((DamageListener) restorer.getValue()).reportDamageApplied(null, target, mock(ApplyDamageResultAPI.class));
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
