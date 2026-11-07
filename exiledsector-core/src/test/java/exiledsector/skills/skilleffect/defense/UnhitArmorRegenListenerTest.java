package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnhitArmorRegenListenerTest {

    private static ArmorGridAPI grid(float maxArmorInCell, float[][] cells) {
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getMaxArmorInCell()).thenReturn(maxArmorInCell);
        when(grid.getGrid()).thenReturn(cells);
        when(grid.getArmorValue(anyInt(), anyInt())).thenAnswer(call -> cells[(int) call.getArgument(0)][(int) call.getArgument(1)]);
        doAnswer(call -> {
            cells[(int) call.getArgument(0)][(int) call.getArgument(1)] = call.getArgument(2);
            return null;
        }).when(grid).setArmorValue(anyInt(), anyInt(), anyFloat());
        return grid;
    }

    @Test
    void onlyCellsUnhitForTheDelayRepairAndNeverPastFullArmor() {
        float[][] cells = {{50f, 50f, 98f}};
        float[][] secondsUnhit = {{UnhitArmorRegenListener.UNHIT_SECONDS, 1f, 10f}};

        float restored = UnhitArmorRegenListener.mend(grid(100f, cells), secondsUnhit, 5f, 1000f);

        assertEquals(5f + 2f, restored, 0.001f);
        assertArrayEquals(new float[]{55f, 50f, 100f}, cells[0], 0.001f);
    }

    @Test
    void theBudgetCutsTheRepairShortEvenlyAcrossCells() {
        float[][] cells = {{0f, 0f}};
        float[][] secondsUnhit = {{10f, 10f}};

        float restored = UnhitArmorRegenListener.mend(grid(100f, cells), secondsUnhit, 10f, 5f);

        assertEquals(5f, restored, 0.001f);
        assertArrayEquals(new float[]{2.5f, 2.5f}, cells[0], 0.001f);
        assertEquals(0f, UnhitArmorRegenListener.mend(grid(100f, cells), secondsUnhit, 10f, 0f));
    }

    private static float repairedOverADeployment(float percent, float lowCapPercent) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(engine.getShipGrid().getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(call -> Collections.emptyIterator());
        float[][] cells = {new float[10]};
        ArmorGridAPI grid = grid(100f, cells);
        ShipAPI ship = mock(ShipAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getArmorGrid()).thenReturn(grid);
        when(ship.getFluxTracker().getMaxFlux()).thenReturn(1_000_000f);
        when(ship.getMutableStats().getDynamic().getValue(UnhitArmorRegenListener.REGEN_PERCENT_KEY, 0f)).thenReturn(percent);
        when(ship.getMutableStats().getDynamic().getValue(UnhitArmorRegenListener.LOW_CAP_REGEN_PERCENT_KEY, 0f)).thenReturn(lowCapPercent);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            UnhitArmorRegenListener listener = new UnhitArmorRegenListener(ship);
            for (int tick = 0; tick < 40; tick++) {
                listener.advance(UnhitArmorRegenListener.TICK_SECONDS);
            }
        }
        float repaired = 0f;
        for (float armor : cells[0]) {
            repaired += armor;
        }
        return repaired;
    }

    @Test
    void eachSourceStopsAtItsOwnShareOfTheShipsArmorPerDeployment() {
        assertEquals(1000f * UnhitArmorRegenListener.LOW_DEPLOYMENT_CAP_PERCENT / 100f, repairedOverADeployment(0f, 50f), 0.01f);
        assertEquals(1000f * UnhitArmorRegenListener.DEPLOYMENT_CAP_PERCENT / 100f, repairedOverADeployment(50f, 0f), 0.01f);
        assertEquals(1000f * (UnhitArmorRegenListener.DEPLOYMENT_CAP_PERCENT + UnhitArmorRegenListener.LOW_DEPLOYMENT_CAP_PERCENT) / 100f,
                repairedOverADeployment(50f, 50f), 0.01f);
    }

    @Test
    void aCellThatLosesArmorRestartsItsDelay() {
        float[][] cells = {{100f, 100f}};
        ArmorGridAPI grid = grid(100f, cells);
        UnhitArmorRegenListener listener = new UnhitArmorRegenListener(mock(ShipAPI.class));
        listener.trackHits(grid, 0.5f);
        listener.trackHits(grid, 3f);

        cells[0][0] = 80f;
        listener.trackHits(grid, 3f);

        assertArrayEquals(new float[]{0f, 6f}, listener.cellSecondsUnhit()[0], 0.001f);
    }
}
