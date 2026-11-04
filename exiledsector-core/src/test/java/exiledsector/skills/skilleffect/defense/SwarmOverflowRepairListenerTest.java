package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwarmOverflowRepairListenerTest {

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
    void eachFragmentRepairsTheMostDamagedCellAtThatMoment() {
        float[][] cells = {{20f, 50f}, {100f, 40f}};

        float restored = SwarmOverflowRepairListener.restoreMostDamaged(grid(100f, cells), 3, 25f, 1000f);

        assertEquals(75f, restored, 0.001f);
        assertArrayEquals(new float[]{70f, 50f}, cells[0], 0.001f);
        assertArrayEquals(new float[]{100f, 65f}, cells[1], 0.001f);
    }

    @Test
    void aRepairNeverOverfillsACellOrPassesTheDeploymentCap() {
        float[][] cells = {{95f, 0f}};

        assertEquals(30f, SwarmOverflowRepairListener.restoreMostDamaged(grid(100f, cells), 5, 50f, 30f), 0.001f);
        assertArrayEquals(new float[]{95f, 30f}, cells[0], 0.001f);
    }

    @Test
    void undamagedArmorTakesNoRepairSoNothingCountsTowardTheCap() {
        float[][] cells = {{100f, 100f}};

        assertEquals(0f, SwarmOverflowRepairListener.restoreMostDamaged(grid(100f, cells), 4, 25f, 1000f));
    }
}
