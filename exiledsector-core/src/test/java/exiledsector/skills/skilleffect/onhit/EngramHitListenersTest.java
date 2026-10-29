package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EngramHitListenersTest {

    private static ShipAPI ship(int owner, String key, float value) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(stats.getMaxSpeed()).thenReturn(new MutableStat(100f));
        when(stats.getShieldDamageTakenMult()).thenReturn(new MutableStat(1f));
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(1000f);
        if (key != null) {
            when(dynamic.getValue(key, 0f)).thenReturn(value);
        }
        return ship;
    }

    private static <T> T addedListener(ShipAPI target, Class<T> type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(target).addListener(captor.capture());
        return type.cast(captor.getValue());
    }

    @Test
    void aBeamShieldHitRaisesShieldDamageTakenUntilTheDisruptionLapses() {
        ShipAPI shooter = ship(0, BeamShieldDisruptionListener.SHIELD_DAMAGE_PERCENT_KEY, 10f);
        ShipAPI target = ship(1, null, 0f);
        BeamShieldDisruptionListener listener = new BeamShieldDisruptionListener(shooter);

        listener.modifyDamageDealt(mock(BeamAPI.class), target, mock(DamageAPI.class), new Vector2f(), false);
        verify(target, never()).addListener(any());
        listener.modifyDamageDealt(mock(BeamAPI.class), target, mock(DamageAPI.class), new Vector2f(), true);

        BeamShieldDisruptionListener.ShieldDisruption disruption = addedListener(target, BeamShieldDisruptionListener.ShieldDisruption.class);
        assertEquals(1.1f, target.getMutableStats().getShieldDamageTakenMult().getModifiedValue(), 1e-4f);
        disruption.advance(BeamShieldDisruptionListener.DISRUPTION_SECONDS + 0.01f);
        assertEquals(1f, target.getMutableStats().getShieldDamageTakenMult().getModifiedValue(), 1e-4f);
        verify(target).removeListener(disruption);
    }

    @Test
    void missileHitsSlowAnEnemyShipForAFewSeconds() {
        ShipAPI shooter = ship(0, MissileSnareListener.TOP_SPEED_PERCENT_KEY, 20f);
        ShipAPI target = ship(1, null, 0f);
        ShipAPI ally = ship(0, null, 0f);
        MissileSnareListener listener = new MissileSnareListener(shooter);

        listener.modifyDamageDealt(mock(MissileAPI.class), ally, mock(DamageAPI.class), new Vector2f(), false);
        verify(ally, never()).addListener(any());
        listener.modifyDamageDealt(mock(MissileAPI.class), target, mock(DamageAPI.class), new Vector2f(), true);

        MissileSnareListener.Snare snare = addedListener(target, MissileSnareListener.Snare.class);
        assertEquals(80f, target.getMutableStats().getMaxSpeed().getModifiedValue(), 1e-3f);
        snare.advance(MissileSnareListener.SNARE_SECONDS - 0.5f);
        assertEquals(80f, target.getMutableStats().getMaxSpeed().getModifiedValue(), 1e-3f);
        snare.advance(1f);
        assertEquals(100f, target.getMutableStats().getMaxSpeed().getModifiedValue(), 1e-3f);
    }

    @Test
    void theKillingBlowVentsTheKillersFluxOnce() {
        ShipAPI killer = ship(0, KillFluxVentListener.MAX_FLUX_PERCENT_KEY, 20f);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.getMaxFlux()).thenReturn(5000f);
        when(killer.getFluxTracker()).thenReturn(flux);
        ShipAPI victim = ship(1, null, 0f);
        when(victim.getListeners(KillFluxVentListener.KillWatch.class)).thenReturn(List.of());

        new KillFluxVentListener(killer).modifyDamageDealt(mock(MissileAPI.class), victim, mock(DamageAPI.class), new Vector2f(), false);
        KillFluxVentListener.KillWatch watch = addedListener(victim, KillFluxVentListener.KillWatch.class);

        watch.reportDamageApplied(killer, victim, null);
        verify(flux, never()).decreaseFlux(anyFloat());
        when(victim.getHitpoints()).thenReturn(0f);
        watch.reportDamageApplied(killer, victim, null);
        watch.reportDamageApplied(killer, victim, null);
        verify(flux).decreaseFlux(1000f);
    }

    @Test
    void strikecraftAndStationModulesAreNotKills() {
        ShipAPI killer = ship(0, null, 0f);
        ShipAPI fighter = ship(1, null, 0f);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI module = ship(1, null, 0f);
        ShipAPI station = ship(1, null, 0f);
        when(module.getParentStation()).thenReturn(station);

        assertEquals(false, KillFluxVentListener.countsAsKill(killer, fighter));
        assertEquals(false, KillFluxVentListener.countsAsKill(killer, module));
        assertEquals(true, KillFluxVentListener.countsAsKill(killer, ship(1, null, 0f)));
    }
}
