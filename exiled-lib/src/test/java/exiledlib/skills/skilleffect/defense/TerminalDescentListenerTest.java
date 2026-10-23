package exiledlib.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalDescentListenerTest {

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void terminalDescentEntersBelowAQuarterHullAndLeavesOnlyAboveFortyPercent() {
        assertFalse(TerminalDescentListener.nextDescentState(false, 0.3f));
        assertTrue(TerminalDescentListener.nextDescentState(false, 0.2f));
        assertTrue(TerminalDescentListener.nextDescentState(true, 0.35f));
        assertTrue(TerminalDescentListener.nextDescentState(true, 0.4f));
        assertFalse(TerminalDescentListener.nextDescentState(true, 0.41f));
    }

    @Test
    void terminalDescentAppliesItsPackageAndBlocksVentingWhileActive() {
        ShipAPI ship = ship("nightmare", 0, Map.of(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, -25f,
                TerminalDescentListener.FLUX_DISSIPATION_PERCENT_KEY, 17f, TerminalDescentListener.SYSTEM_REGEN_PERCENT_KEY, 17f,
                TerminalDescentListener.SHIELD_DAMAGE_TAKEN_PERCENT_KEY, 10f));
        float[] hull = {1000f};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hull[0]);
        TerminalDescentListener descent = new TerminalDescentListener(ship);
        MutableShipStatsAPI stats = ship.getMutableStats();
        String modId = "exiledSector_terminalDescent_nightmare";

        descent.advance(0.1f);
        assertFalse(descent.inDescent());

        hull[0] = 200f;
        descent.advance(0.1f);
        descent.advance(0.1f);
        assertTrue(descent.inDescent());
        verify(stats.getHullDamageTakenMult()).modifyPercent(modId, -25f);
        verify(stats.getFluxDissipation()).modifyPercent(modId, 17f);
        verify(stats.getSystemRegenBonus()).modifyPercent(modId, 17f);
        verify(stats.getShieldDamageTakenMult()).modifyPercent(modId, 10f);
        verify(stats.getPeakCRDuration()).modifyMult(modId, 0f);
        verify(ship, times(2)).blockCommandForOneFrame(ShipCommand.VENT_FLUX);

        hull[0] = 450f;
        descent.advance(0.1f);
        assertFalse(descent.inDescent());
        verify(stats.getPeakCRDuration()).unmodify(modId);
    }

    @Test
    void terminalDescentWaitsForTheHullToDropDuringTheBattle() {
        ShipAPI ship = ship("battered", 0, Map.of(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, -25f));
        float[] hull = {200f};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hull[0]);
        TerminalDescentListener descent = new TerminalDescentListener(ship);

        descent.advance(0.1f);
        assertFalse(descent.inDescent());
        verify(ship, never()).blockCommandForOneFrame(ShipCommand.VENT_FLUX);

        hull[0] = 300f;
        descent.advance(0.1f);
        hull[0] = 200f;
        descent.advance(0.1f);
        assertTrue(descent.inDescent());
    }

    @Test
    void terminalDescentStopsAVentAlreadyRunning() {
        ShipAPI ship = ship("venting", 0, Map.of(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, -25f));
        float[] hull = {1000f};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hull[0]);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.isVenting()).thenReturn(true);
        when(ship.getFluxTracker()).thenReturn(flux);
        TerminalDescentListener descent = new TerminalDescentListener(ship);

        descent.advance(0.1f);
        hull[0] = 100f;
        descent.advance(0.1f);

        verify(flux).stopVenting();
    }
}
