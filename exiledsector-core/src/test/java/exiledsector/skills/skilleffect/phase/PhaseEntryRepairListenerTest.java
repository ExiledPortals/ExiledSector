package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseEntryRepairListenerTest {

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
    void everyCellRepairsItsShareUpToFullArmor() {
        float[][] cells = {{50f, 95f}, {100f, 0f}};

        float restored = PhaseEntryRepairListener.restore(grid(100f, cells), 10f, 1000f);

        assertEquals(10f + 5f + 0f + 10f, restored, 0.001f);
        assertArrayEquals(new float[]{60f, 100f}, cells[0], 0.001f);
        assertArrayEquals(new float[]{100f, 10f}, cells[1], 0.001f);
    }

    @Test
    void undamagedArmorRepairsNothingSoNothingCountsTowardTheCap() {
        float[][] cells = {{100f, 100f}};

        assertEquals(0f, PhaseEntryRepairListener.restore(grid(100f, cells), 10f, 1000f));
    }

    @Test
    void theLastRepairIsCutShortToLandExactlyOnTheCap() {
        float[][] cells = {{0f, 0f}};

        float restored = PhaseEntryRepairListener.restore(grid(100f, cells), 10f, 5f);

        assertEquals(5f, restored, 0.001f);
        assertArrayEquals(new float[]{2.5f, 2.5f}, cells[0], 0.001f);
        assertEquals(0f, PhaseEntryRepairListener.restore(grid(100f, cells), 10f, 0f));
    }
}
