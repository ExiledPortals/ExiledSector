package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
